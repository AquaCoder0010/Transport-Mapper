package Nodes;

import java.util.List;
import org.openstreetmap.gui.jmapviewer.Coordinate;

public class CoordinatesToPath {

    public static double euclideanDistance(Coordinate a, Coordinate b) {
        double dlat = a.getLat() - b.getLat();
        double dlon = a.getLon() - b.getLon();
        return Math.sqrt(dlat * dlat + dlon * dlon);
    }

    public static double euclideanDistanceKm(Coordinate a, Coordinate b) {
        double R = 6371.0;
        double dlat = Math.toRadians(a.getLat() - b.getLat());
        double dlon = Math.toRadians(a.getLon() - b.getLon());
        double alat = Math.toRadians(a.getLat());
        double blat = Math.toRadians(b.getLat());
        double x = dlon * Math.cos((alat + blat) / 2);
        double y = dlat;
        return Math.sqrt(x * x + y * y) * R;
    }

    public static NodeEntry findNearestNodeEntry(Coordinate point, List<NodeEntry> nodes) {
        NodeEntry nearest = null;
        double minDist = Double.MAX_VALUE;
        for (NodeEntry entry : nodes) {
            double d1 = euclideanDistance(point, entry.getStartCoordinate());
            if (d1 < minDist) {
                minDist = d1;
                nearest = entry;
            }
            double d2 = euclideanDistance(point, entry.getEndCoordinate());
            if (d2 < minDist) {
                minDist = d2;
                nearest = entry;
            }
        }
        return nearest;
    }

    public static Coordinate findNearestCoordinate(Coordinate point, List<NodeEntry> nodes) {
        Coordinate nearestCoord = null;
        double minDist = Double.MAX_VALUE;
        for (NodeEntry entry : nodes) {
            double d1 = euclideanDistance(point, entry.getStartCoordinate());
            if (d1 < minDist) {
                minDist = d1;
                nearestCoord = entry.getStartCoordinate();
            }
            double d2 = euclideanDistance(point, entry.getEndCoordinate());
            if (d2 < minDist) {
                minDist = d2;
                nearestCoord = entry.getEndCoordinate();
            }
        }
        return nearestCoord;
    }

    public static double findMinDistance(Coordinate point, List<NodeEntry> nodes) {
        double minDist = Double.MAX_VALUE;
        for (NodeEntry entry : nodes) {
            double d1 = euclideanDistance(point, entry.getStartCoordinate());
            if (d1 < minDist) minDist = d1;
            double d2 = euclideanDistance(point, entry.getEndCoordinate());
            if (d2 < minDist) minDist = d2;
        }
        return minDist;
    }
}
