package Nodes;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import java.util.ArrayList;
import java.util.List;

public class Routes {
    private List<Coordinate> path;
    private double totalCost;

    public Routes() {
        this.path      = new ArrayList<>();
        this.totalCost = Double.MAX_VALUE;
    }

    public Routes(List<Coordinate> path, double totalCost) {
        this.path      =  path;
        this.totalCost = totalCost;
    }

    public List<Coordinate> getPath()    { return path; }
    public double getTotalCost()         { return totalCost; }
    public boolean isReachable()         { return totalCost < Double.MAX_VALUE; }


}
