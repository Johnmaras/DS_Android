package com.example.john.testing_the_maps;

import com.google.android.gms.maps.model.LatLng;

public class LatLngComparator {
    public static int compare(LatLng o1, LatLng o2){
        double lat1 = o1.latitude;
        double lng1 = o1.longitude;
        double lat2 = o2.latitude;
        double lng2 = o2.longitude;

        if(lng1 > lng2 && lat1 > lat2){
            return 1;
        }else if(lng1 == lng2 && lat1 == lat2){
            return 0;
        }else{
            return -1;
        }
    }
}
