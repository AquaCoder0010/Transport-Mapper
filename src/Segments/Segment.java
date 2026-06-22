package Segments;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import java.util.*;


public class Segment {
    List<Coordinate> path;
    double distanceKm;
    public Segment(List<Coordinate> path, double distanceKm) {
        this.path = path;
        this.distanceKm = distanceKm;
    }
    public List<Coordinate> getPath(){
        return path;
    }
    public void setPath(List<Coordinate> v){
        this.path = v;
    }
    public double getDistance(){
        return distanceKm;
    }
    public void setDistance(double distance){
        this.distanceKm = distance;
    }
}
