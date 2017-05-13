package com.example.john.testing_the_maps;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private MarkerOptions options = new MarkerOptions();
    private ArrayList<LatLng> places = new ArrayList<>();
    private ArrayList<Marker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        boolean marksVisibility;
        places.add(new LatLng(37.994129, 23.731960));
        places.add(new LatLng(37.9757, 23.7339));
        int i = 1;
        for(LatLng point : places){
            options.position(point);
            options.title("Point " + i++);
            options.snippet("test");
            markers.add(mMap.addMarker(options));
        }
        marksVisibility = true;
        

        LatLng focus_point = options.getPosition();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(focus_point));
        //how much to zoom, speed
        mMap.animateCamera(CameraUpdateFactory.zoomTo(14.5f), 2000, null);

        final Button btn = (Button) findViewById(R.id.btnToggleHide);

        btn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){
                for(Marker marker: markers){
                    marker.setVisible(!marker.isVisible());
                }

            }
        });

        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }
}
