package com.example.john.testing_the_maps;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.EncodedPolyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


//TODO listen for gps state changes and hide/reveal the my location marker accordingly
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private MarkerOptions options = new MarkerOptions();
    private ArrayList<LatLng> places = new ArrayList<>();
    private ArrayList<Marker> markers = new ArrayList<>();
    private boolean marksVisible = true;
    private final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

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

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            new AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    MY_PERMISSIONS_REQUEST_LOCATION );
                        }
                    })
                    .create()
                    .show();
            return;
        }

        GeoApiContext context = new GeoApiContext()
                .setQueryRateLimit(3)
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS).setApiKey("AIzaSyAa5T-N6-BRrJZSK0xlSrWlTh-C7RjOVdY");

        DirectionsResult result = new DirectionsResult();
        try {
            result = DirectionsApi.getDirections(context, "Toronto", "Montreal").await();
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        EncodedPolyline encPolyline = result.routes[0].overviewPolyline;
        PolylineOptions polylineOptions = new PolylineOptions();

        LatLng linePoint = null;
        for(com.google.maps.model.LatLng point: encPolyline.decodePath()){
            linePoint = new LatLng(point.lat, point.lng);
            polylineOptions.add(linePoint);
        }
        final Polyline polyline = mMap.addPolyline(polylineOptions);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(linePoint));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(5f), 2000, null);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(marksVisible) {
                    options.position(latLng);
                    options.draggable(true);
                    options.title("Point " + (markers.size() + 1));
                    options.snippet("Testing the Maps");
                    markers.add(mMap.addMarker(options));
                }else{
                    Toast.makeText(MapsActivity.this, "Cannot add mark when marks are hidden", Toast.LENGTH_SHORT).show();
                }
            }
        });

        /*places.add(new LatLng(37.994129, 23.731960));
        places.add(new LatLng(37.9757, 23.7339));
        int i = 1;
        for(LatLng point : places){
            options.position(point);
            options.draggable(true);
            options.title("Point " + i++);
            options.snippet("test");
            markers.add(mMap.addMarker(options));
        }*/


        /*LatLng focus_point = options.getPosition();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(focus_point));
        //how much to zoom, speed
        mMap.animateCamera(CameraUpdateFactory.zoomTo(14.5f), 2000, null);*/

        final Button BtnToggleHide = (Button) findViewById(R.id.btnToggleHide);
        BtnToggleHide.setText("Hide");

        BtnToggleHide.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){
                for(Marker marker: markers){
                    marker.setVisible(!marker.isVisible());
                }
                marksVisible = !marksVisible;
                BtnToggleHide.setText(marksVisible ? "Hide":"Reveal");
            }
        });

        final FloatingActionButton BtnGetDirs = (FloatingActionButton) findViewById(R.id.btnGetDirections);

        BtnGetDirs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(polyline.isVisible()){
                    polyline.setVisible(false);
                }else{
                    polyline.setVisible(true);
                }
            }

        });
    }
}

