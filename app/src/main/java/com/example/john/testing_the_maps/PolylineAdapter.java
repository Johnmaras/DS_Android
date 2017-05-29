package com.example.john.testing_the_maps;

import java.util.ArrayList;
import java.util.List;

public class PolylineAdapter{
    private final ArrayList<LatLngAdapter> points = new ArrayList<>();

    public ArrayList<LatLngAdapter> getPoints() {
        return points;
    }

    public void addPoint(double latitude, double longitude){
        LatLngAdapter newPoint = new LatLngAdapter(latitude, longitude);
        points.add(newPoint);
    }

    public void addPoint(LatLngAdapter point){
        points.add(point);
    }

    public void addAllPoint(List<LatLngAdapter> point){
        points.addAll(point);
    }
}
