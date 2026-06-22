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

import Segments.Segment;
import Segments.PathSegment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class MapViewer extends JFrame {
    private static final Coordinate ADDIS_ABABA = new Coordinate(9.035, 38.764);

    private static List<NodeEntry> nodesList;


    private Coordinate startPos;
    private Coordinate endPos;
    private boolean selectingStart = true;

    private JTextField startField;
    private JTextField endField;
    private JTextArea resultArea;
    private JButton calculateButton;

    private JMapViewer map;
    private MapMarkerDot startMarker;
    private MapMarkerDot endMarker;
    private List<MapPolygonImpl> pathPolygons = new ArrayList<>();
    private List<MapMarkerDot> pathNodes = new ArrayList<>();
    
    

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

        createControlPanel();
        setupMouseListener();
        setupEnterListener();

        add(map, BorderLayout.CENTER);
        setVisible(true);
        requestFocusInWindow();
    }

    private void loadNodes() {
        // change later
        try {
            nodesList = NodeLoader.loadRoutes("info//bus_info//output.txt");
        } catch (IOException e) {
            System.err.println("Error reading routes: " + e.getMessage());
        }
    }

    private void clearNodes(){
        for(MapMarkerDot m : pathNodes){
            map.removeMapMarker(m);
        }
        pathNodes.clear();
    }
    private void plotBusNodes(List<Coordinate> nodes){
        for (Coordinate n : nodes) {
            MapMarkerDot marker = new MapMarkerDot(n);
            pathNodes.add(marker);
            marker.setBackColor(Color.MAGENTA);
            map.addMapMarker(marker);
        }
    }
    private void createControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;

        JLabel titleLabel = new JLabel("Map Route Finder");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20)); 
        titleLabel.setForeground(new Color(50, 50, 50)); 
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 15, 8);
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(6, 8, 6, 8);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Start:"), gbc);
        gbc.gridx = 1;
        startField = new JTextField(22);
        startField.setEditable(false);
        panel.add(startField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("End:"), gbc);
        gbc.gridx = 1;
        endField = new JTextField(22);
        endField.setEditable(false);
        panel.add(endField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Info:"), gbc);

        gbc.gridx = 1;
        resultArea = new JTextArea(5, 22);
        resultArea.setEditable(false);
        resultArea.setText("Click two points on the map, then press Enter");
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(resultArea);
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(scrollPane, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        calculateButton = new JButton("Calculate");
        panel.add(calculateButton, gbc);
        

        JPanel centeringWrapper = new JPanel(new GridBagLayout());
        centeringWrapper.add(panel); 

        add(centeringWrapper, BorderLayout.WEST);
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
    private void setupEnterListener(){
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == calculateButton) {
                    calculatePath();
                }
            }
        });
    }
    private void calculatePath() {
        if (startPos == null || endPos == null) {
            resultArea.setText("Click two points on the map first");
            return;
        }
        resultArea.setText("Calculating...");

        new Thread(() -> {
            try {
                NodesToPath nodesToPath = new NodesToPath();
                
                if (nodesToPath.calculatePath(startPos, endPos, nodesList) == false) {
                    SwingUtilities.invokeLater(() -> resultArea.setText("No path found"));
                    return;
                }

                List<PathSegment> segments = nodesToPath.getSegmentedRoutes();
                List<Coordinate> finalNodes = nodesToPath.getBusNodes();

                SwingUtilities.invokeLater(() -> {
                    clearNodes();
                    plotBusNodes(finalNodes);
                    
                    
                    for (MapPolygonImpl p : pathPolygons) {
                        map.removeMapPolygon(p);
                    }
                    pathPolygons.clear();

                    Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{8, 6}, 0); 
                    Stroke solid = new BasicStroke(4);

                    routeColorMap.clear();
                    nextColorIndex = 0;

                    double totalDist = 0;
                    StringBuilder routeNames = new StringBuilder();
                    for (PathSegment seg : segments) {
                        totalDist += seg.getDistance();

                        MapPolygonImpl poly = new MapPolygonImpl(seg.getPath());
                        if (seg.isBus()) {
                            Color c = ROUTE_COLORS[nextColorIndex % ROUTE_COLORS.length];
                            nextColorIndex++;

                            poly.setColor(c);
                            poly.setStroke(solid);

                            if (routeNames.length() > 0) routeNames.append(", ");
                            routeNames.append(seg.getRouteLabel());
                        } else {
                            poly.setColor(Color.BLUE);
                            poly.setStroke(dashed);
                        }
                        map.addMapPolygon(poly);
                        pathPolygons.add(poly);
                    }

                    resultArea.setText(String.format("Total: %.2f km\n Routes: %s \n", totalDist, routeNames.toString()));
                });
            } catch (Exception ex) {
                System.out.print(ex.getMessage());
                SwingUtilities.invokeLater(() -> resultArea.setText("API unavailable"));
            }
        }).start();
    }

    public static void main(String[] args) {
        try{
            nodesList = NodeLoader.loadRoutes("info//bus_info//output.txt"); 
            SwingUtilities.invokeLater(() -> new MapViewer());
        }
        catch(IOException e){
            System.err.println("Error while getting routes" + e.getMessage());
            System.exit(-1);
        }
    }
}
