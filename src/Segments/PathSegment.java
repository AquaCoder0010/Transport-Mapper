package Segments;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import java.util.*;


public class PathSegment extends Segment {
    private boolean isBus;
    private String routeId;
    private String routeLabel;

    public PathSegment(List<Coordinate> path, double distanceKm){
        super(path, distanceKm);
        isBus = false;
    }

    public boolean isBus(){
        return isBus;
    }

    public String getRouteID(){
        return routeId;
    }

    public String getRouteLabel(){
        return routeLabel;
    }

    public void setBus(boolean isBus) {
        this.isBus = isBus;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public void setRouteLabel(String routeLabel) {
        this.routeLabel = routeLabel;
    }   
}
