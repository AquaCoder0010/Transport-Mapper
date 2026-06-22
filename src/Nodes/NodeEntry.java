package Nodes;
import org.openstreetmap.gui.jmapviewer.Coordinate;


public class NodeEntry{ 
    // no use for nodeId
    private final String nodeId;
    private final String startNodeName;
    private final String endNodeName;
    private Coordinate startCoordinate;  
    private Coordinate endCoordinate;


    public NodeEntry(String nodeId, String startNodeName, String endNodeName, Coordinate startCoordinate, Coordinate endCoordinate) {
        this.nodeId = nodeId;
        this.startNodeName = startNodeName;
        this.endNodeName = endNodeName;
        this.startCoordinate = startCoordinate;
        this.endCoordinate = endCoordinate;
    }

    public String getNodeID() {
        return nodeId;
    }

    public String getRouteName() {
        return this.startNodeName + " <-> " + this.endNodeName;
    }

    public Coordinate getStartCoordinate() {
        return this.startCoordinate;
    }

    public Coordinate getEndCoordinate() {
        return this.endCoordinate;
    }
}


