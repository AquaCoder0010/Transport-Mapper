import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;

import Nodes.NodeEntry;
import Nodes.NodeLoader;
import Nodes.NodesToPath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class MapViewer extends JFrame {
    private static final Coordinate ADDIS_ABABA = new Coordinate(9.035, 38.764);

    private List<NodeEntry> nodesList;
    private Coordinate startPos;
    private Coordinate endPos;
    private boolean selectingStart = true;

    private JTextField startField;
    private JTextField endField;
    private JLabel resultLabel;
    private JMapViewer map;
    private MapMarkerDot startMarker;
    private MapMarkerDot endMarker;
    private List<MapPolygonImpl> pathPolygons = new ArrayList<>();

    private static final Color[] ROUTE_COLORS = {
        Color.RED, Color.ORANGE, Color.GREEN, Color.MAGENTA,
        Color.CYAN, Color.PINK, new Color(139, 69, 19),
        new Color(75, 0, 130), new Color(0, 150, 0), new Color(255, 140, 0)
    };
    private final Map<String, Color> routeColorMap = new HashMap<>();
    private int nextColorIndex = 0;

    public MapViewer() {
        super("Transport Simulator - Addis Ababa");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLayout(new BorderLayout());

        map = new JMapViewer();
        map.setDisplayPosition(ADDIS_ABABA, 12);

        loadNodes();
        plotBusNodes();
        createControlPanel();
        setupMouseListener();
        setupEnterBinding();

        add(map, BorderLayout.CENTER);
        setVisible(true);
        requestFocusInWindow();
    }

    private void loadNodes() {
        try {
            nodesList = NodeLoader.loadRoutes("info//bus_info//output.txt");
        } catch (IOException e) {
            System.err.println("Error reading routes: " + e.getMessage());
        }
    }

    private void plotBusNodes(){
        for (NodeEntry entry : nodesList) {
            MapMarkerDot marker = new MapMarkerDot(entry.getStartCoordinate());
            marker.setBackColor(Color.MAGENTA);
            map.addMapMarker(marker);
            
            marker = new MapMarkerDot(entry.getEndCoordinate());
            marker.setBackColor(Color.MAGENTA);
            map.addMapMarker(marker);
        }
    }

    private void createControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;


        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Start:"), gbc);
        gbc.gridx = 1;
        startField = new JTextField(22);
        startField.setEditable(false);
        panel.add(startField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("End:"), gbc);
        gbc.gridx = 1;
        endField = new JTextField(22);
        endField.setEditable(false);
        panel.add(endField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Distance:"), gbc);
        gbc.gridx = 1;
        resultLabel = new JLabel("Click two points on the map, then press Enter");
        panel.add(resultLabel, gbc);

        add(panel, BorderLayout.EAST);
    }

    private void setupMouseListener() {
        map.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Coordinate pos = (Coordinate) map.getPosition(e.getPoint());
                if (selectingStart) {
                    startPos = pos;
                    startField.setText(String.format("%.6f, %.6f", pos.getLat(), pos.getLon()));

                    if (startMarker != null)
                        map.removeMapMarker(startMarker);
                    
                    startMarker = new MapMarkerDot(pos);
                    startMarker.setBackColor(Color.GREEN);
                    map.addMapMarker(startMarker);
                    selectingStart = false;
                    
                } else {
                    endPos = pos;
                    endField.setText(String.format("%.6f, %.6f", pos.getLat(), pos.getLon()));

                    if (endMarker != null) 
                        map.removeMapMarker(endMarker);

                    endMarker = new MapMarkerDot(pos);
                    endMarker.setBackColor(Color.RED);
                    map.addMapMarker(endMarker);
                    selectingStart = true;
                    
                }
                requestFocusInWindow();
            }
        });
    }

    private void setupEnterBinding() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "calculatePath");

            
        getRootPane().getActionMap().put("calculatePath", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculatePath();
            }
        });
    }

    private void calculatePath() {
        if (startPos == null || endPos == null) {
            resultLabel.setText("Click two points on the map first");
            return;
        }
        if (nodesList == null || nodesList.isEmpty()) {
            resultLabel.setText("No route data loaded");
            return;
        }

        resultLabel.setText("Calculating...");
        
        new Thread(() -> {
            try {
                List<NodesToPath.PathSegment> segments = new NodesToPath().getSegmentedRoutes(startPos, endPos, nodesList);
                if (segments == null || segments.isEmpty()) {
                    SwingUtilities.invokeLater(() -> resultLabel.setText("No path found"));
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    for (MapPolygonImpl p : pathPolygons) {
                        map.removeMapPolygon(p);
                    }
                    pathPolygons.clear();

                    Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL, 0, new float[]{8, 6}, 0);
                    Stroke solid = new BasicStroke(4);

                    routeColorMap.clear();
                    nextColorIndex = 0;

                    double totalDist = 0;
                    StringBuilder routeNames = new StringBuilder();
                    String lastRouteId = null;
                    for (NodesToPath.PathSegment seg : segments) {
                        totalDist += seg.distanceKm;
                        MapPolygonImpl poly = new MapPolygonImpl(seg.path);
                        if (seg.isBus) {
                            Color c = routeColorMap.get(seg.routeId);
                            if (c == null) {
                                c = ROUTE_COLORS[nextColorIndex % ROUTE_COLORS.length];
                                nextColorIndex++;
                                routeColorMap.put(seg.routeId, c);
                            }
                            if (!seg.routeId.equals(lastRouteId)) {
                                if (routeNames.length() > 0) routeNames.append("  |  ");
                                routeNames.append(seg.routeLabel);
                                lastRouteId = seg.routeId;
                            }
                            poly.setColor(c);
                            poly.setStroke(solid);
                        } else {
                            poly.setColor(Color.BLUE);
                            poly.setStroke(dashed);
                        }
                        map.addMapPolygon(poly);
                        pathPolygons.add(poly);
                    }

                    resultLabel.setText(String.format("<html>Total: %.2f km<br>Routes: %s</html>", totalDist, routeNames.toString()));
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> resultLabel.setText("API unavailable"));
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MapViewer());
    }
}
