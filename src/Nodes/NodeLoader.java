package Nodes;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.openstreetmap.gui.jmapviewer.Coordinate;


public class NodeLoader {
    public static List<NodeEntry> loadRoutes(String filePath) throws IOException {
        List<NodeEntry> routes = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(filePath))) {
            int lineNumber = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lineNumber += 1;

                if (lineNumber == 1) {
                    continue;
                }
                String[] parts = line.split(",");
                
                if (parts.length < 6) {
                    throw new IOException("Invalid CSV line " + lineNumber + ": " + line);
                }

                String nodeId = parts[0];
                String nodeName = parts[1];
                String startName = nodeName.split("-")[0].trim();
                String endName = nodeName.split("-")[1].trim();

                float startLon, endLon;
                float startLat, endLat;
                try {
                    startLon = Float.parseFloat(parts[2]);
                    startLat = Float.parseFloat(parts[3]);

                    endLon = Float.parseFloat(parts[4]);
                    endLat = Float.parseFloat(parts[5]);
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid number in line " + lineNumber, e);
                }

                Coordinate startCoord = new Coordinate(startLat, startLon);
                Coordinate endCoord = new Coordinate(endLat, endLon);
                
                routes.add(new NodeEntry(nodeId, startName, endName, startCoord, endCoord));
            }
        }
        return routes;
    }
}