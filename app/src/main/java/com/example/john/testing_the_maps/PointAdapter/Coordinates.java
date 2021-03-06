package com.example.john.testing_the_maps.PointAdapter;

//TODO override equals
public class Coordinates{

    private LatLngAdapter origin;
    private LatLngAdapter destination;

    public Coordinates(LatLngAdapter origin, LatLngAdapter destination){
        this.origin = origin;
        this.destination = destination;
    }

    public LatLngAdapter getOrigin(){
        return this.origin;
    }

    public LatLngAdapter getDestination() {
        return this.destination;
    }

    public Coordinates round(){
        double origin_lat = (int)(origin.getLatitude() * 100) / 100.0;
        double origin_lng = (int)(origin.getLongitude() * 100) / 100.0;

        double dest_lat = (int)(destination.getLatitude() * 100) / 100.0;
        double dest_lng = (int)(destination.getLongitude() * 100) / 100.0;

        /*origin = new PointAdapter.LatLngAdapter(origin_lat, origin_lng);
        destination = new PointAdapter.LatLngAdapter(dest_lat, dest_lng);*/
        return new Coordinates(new LatLngAdapter(origin_lat, origin_lng), new LatLngAdapter(dest_lat, dest_lng));
    }

    @Override
    public boolean equals(Object obj) {
        return origin.equals(((Coordinates)obj).getOrigin()) && destination.equals(((Coordinates)obj).getDestination());
    }

    @Override
    public String toString() {
        return "origin = " + origin + " destination = " + destination;
    }

    public static void main(String[] args){
        LatLngAdapter ll1 = new LatLngAdapter(53.2345134, 0.5533142222);
        LatLngAdapter ll2 = new LatLngAdapter(12.6, 1.0131);
        Coordinates co = new Coordinates(ll1, ll2);
        co.round();
        System.out.println(co);
    }
}
