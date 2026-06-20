package Nodes;

import java.util.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openstreetmap.gui.jmapviewer.Coordinate;

public class NodesToPath {

    public static class PathSegment {
        // is the list limited by two values ? 
        public List<Coordinate> path;
        public double distanceKm;
        public boolean isBus;
        public String routeId;
        public String routeLabel;
    }

    private static class OsrmSegment {
        List<Coordinate> path;
        double distanceKm;
        OsrmSegment(List<Coordinate> path, double distanceKm) {
            this.path = path;
            this.distanceKm = distanceKm;
        }
    }
    private static final String OSRM_BASE = "https://router.project-osrm.org/route/v1";

    private static String coordKey(Coordinate c) {
        return c.getLat() + "," + c.getLon();
    }

    private static double euclidean(Coordinate a, Coordinate b) {
        double dLat = a.getLat() - b.getLat();
        double dLon = a.getLon() - b.getLon();
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }
    private static String readResponse(HttpURLConnection conn) throws IOException {
        try (Scanner scanner = new Scanner(conn.getInputStream())) {
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNext()) {
                sb.append(scanner.nextLine());
            }
            return sb.toString();
        }
    }

    private static OsrmSegment fetchRoute(String profile, Coordinate from, Coordinate to) throws Exception {
        String urlStr = OSRM_BASE + "/" + profile + "/"
            + from.getLon() + "," + from.getLat() + ";"
            + to.getLon() + "," + to.getLat()
            + "?overview=full&geometries=geojson";

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (conn.getResponseCode() != 200) return null;

        JSONObject json = new JSONObject(readResponse(conn));
        if (!"Ok".equals(json.optString("code"))) return null;

        JSONArray routes = json.optJSONArray("routes");
        if (routes == null || routes.length() == 0) return null;

        JSONObject route = routes.getJSONObject(0);
        double distanceKm = route.optDouble("distance", 0) / 1000.0;

        JSONObject geometry = route.optJSONObject("geometry");
        JSONArray coords = geometry != null ? geometry.optJSONArray("coordinates") : null;

        List<Coordinate> path = new ArrayList<>();
        if (coords != null) {
            for (int i = 0; i < coords.length(); i++) {
                JSONArray c = coords.getJSONArray(i);
                path.add(new Coordinate(c.getDouble(1), c.getDouble(0)));
            }
        }
        return new OsrmSegment(path, distanceKm);
    }

    public List<PathSegment> getSegmentedRoutes(Coordinate start, Coordinate end, List<NodeEntry> nodes) throws Exception {
        Routes route = getRoutes(start, end, nodes);
        if (route == null || !route.isReachable()) return null;

        List<PathSegment> segments = new ArrayList<>();
        List<Coordinate> path = route.getPath();

        for (int i = 0; i < path.size() - 1; i++) {
            Coordinate a = path.get(i);
            Coordinate b = path.get(i + 1);
            NodeEntry entry = findNodeEntry(a, b, nodes);
            String profile = entry != null ? "driving" : "walking";

            OsrmSegment osrm = fetchRoute(profile, a, b);
            if (osrm == null) return null;

            PathSegment seg = new PathSegment();
            seg.path = osrm.path;
            seg.distanceKm = osrm.distanceKm;
            seg.isBus = entry != null;
            seg.routeId = entry != null ? entry.getNodeID() : null;
            seg.routeLabel = entry != null ? entry.getRouteName() : null;
            segments.add(seg);
        }

        return segments;
    }

    private static NodeEntry findNodeEntry(Coordinate a, Coordinate b, List<NodeEntry> nodes) {
        for (NodeEntry n : nodes) {
            boolean aMatch = (a.getLat() == n.getStartCoordinate().getLat() && a.getLon() == n.getStartCoordinate().getLon())
                          || (a.getLat() == n.getEndCoordinate().getLat() && a.getLon() == n.getEndCoordinate().getLon());
            boolean bMatch = (b.getLat() == n.getStartCoordinate().getLat() && b.getLon() == n.getStartCoordinate().getLon())
                          || (b.getLat() == n.getEndCoordinate().getLat() && b.getLon() == n.getEndCoordinate().getLon());
            if (aMatch && bMatch) return n;
        }
        return null;
    }

    public Routes getRoutes(Coordinate startCoordinate,
                            Coordinate endCoordinate,
                            List<NodeEntry> nodes) {

        // ── 1. Collect all unique coordinate points ───────────────────────────
        Map<String, Coordinate> coordMap = new LinkedHashMap<>();

        String startKey = coordKey(startCoordinate);
        String endKey   = coordKey(endCoordinate);

        coordMap.put(startKey, startCoordinate);
        coordMap.put(endKey,   endCoordinate);

        for (NodeEntry node : nodes) {
            coordMap.put(coordKey(node.getStartCoordinate()), node.getStartCoordinate());
            coordMap.put(coordKey(node.getEndCoordinate()),   node.getEndCoordinate());
        }

        // 
        Map<String, Map<String, Double>> graph = new HashMap<>();
        for (String key : coordMap.keySet()) graph.put(key, new HashMap<>());

        Set<String> withinNodeEdges = new HashSet<>();

        for (NodeEntry node : nodes) {
            String sk   = coordKey(node.getStartCoordinate());
            String ek   = coordKey(node.getEndCoordinate());
            double dist = euclidean(node.getStartCoordinate(), node.getEndCoordinate());

            
            graph.get(sk).merge(ek, dist, Math::min);
            graph.get(ek).merge(sk, dist, Math::min);

            withinNodeEdges.add(sk + ">" + ek);
            withinNodeEdges.add(ek + ">" + sk);
        }

        List<String> allKeys = new ArrayList<>(coordMap.keySet());
        for (int i = 0; i < allKeys.size(); i++) {
            for (int j = i + 1; j < allKeys.size(); j++) {
                String ki = allKeys.get(i);
                String kj = allKeys.get(j);

                if (withinNodeEdges.contains(ki + ">" + kj)) continue;

                double penalized = euclidean(coordMap.get(ki), coordMap.get(kj)) * 10.0;
                graph.get(ki).merge(kj, penalized, Math::min);
                graph.get(kj).merge(ki, penalized, Math::min);
            }
        }

        //
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> prev      = new HashMap<>();

        for (String key : coordMap.keySet()) {
            distances.put(key, Double.MAX_VALUE);
            prev.put(key, null);
        }
        distances.put(startKey, 0.0);

        PriorityQueue<AbstractMap.SimpleEntry<Double, String>> pq = new PriorityQueue<>(
            Comparator.comparingDouble(Map.Entry::getKey)
        );
        pq.offer(new AbstractMap.SimpleEntry<>(0.0, startKey));

        while (!pq.isEmpty()) {
            Map.Entry<Double, String> top = pq.poll();
            double currentDist = top.getKey();
            String current     = top.getValue();

            if (currentDist > distances.get(current)) continue;
            if (current.equals(endKey)) break;

            for (Map.Entry<String, Double> edge : graph.get(current).entrySet()) {
                double newDist = currentDist + edge.getValue();
                if (newDist < distances.get(edge.getKey())) {
                    distances.put(edge.getKey(), newDist);
                    prev.put(edge.getKey(), current);
                    pq.offer(new AbstractMap.SimpleEntry<>(newDist, edge.getKey()));
                }
            }
        }

        // ── 4. Reconstruct path by walking prev[] backwards ───────────────────
        List<Coordinate> path = new ArrayList<>();
        String current = endKey;
        while (current != null) {
            path.add(coordMap.get(current));
            current = prev.get(current);
        }
        Collections.reverse(path);

        return new Routes(path, distances.get(endKey));
    }
}
