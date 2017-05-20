package com.example.john.testing_the_maps;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.EncodedPolyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


//TODO listen for gps state changes and hide/reveal the my location marker accordingly
//TODO get directions asynchronously
//TODO why do two different kinds of LatLng exist?
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private final String ApiKey = "AIzaSyAa5T-N6-BRrJZSK0xlSrWlTh-C7RjOVdY";
    private GoogleMap mMap;
    private MarkerOptions options = new MarkerOptions();
    //private ArrayList<LatLng> places = new ArrayList<>();
    private ArrayList<Marker> markers = new ArrayList<>();
    private int olderMarker = 0;
    private ArrayList<Polyline> polylines = new ArrayList<>();
    private ArrayList<PolylineOptions> polOptions = new ArrayList<>();
    //private boolean marksVisible = true;
    private final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private boolean permissionGranted = false;

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

        final GeoApiContext context = new GeoApiContext()
                .setQueryRateLimit(3)
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS).setApiKey(ApiKey);

        if(permissionGranted){
            mMap.setMyLocationEnabled(true);
        }
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(!markers.isEmpty() && markers.size() == 2){
                    markers.get(olderMarker).remove();
                    markers.remove(olderMarker);
                }
                options.position(latLng);
                options.draggable(true);
                options.title("Point " + (markers.size() + 1));
                options.snippet("Testing the Maps");
                markers.add(olderMarker, mMap.addMarker(options));
                olderMarker = olderMarker == 0 ? 1:0;
            }
        });

        final Button btnClearMarkers = (Button) findViewById(R.id.btnClearMarkers);

        btnClearMarkers.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){
                for(Marker marker: markers){
                    marker.remove();
                }
                markers.clear();
                olderMarker = 0;
            }
        });

        final FloatingActionButton BtnGetDirs = (FloatingActionButton) findViewById(R.id.btnGetDirections);

        BtnGetDirs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                com.google.maps.model.LatLng point1 = new com.google.maps.model.LatLng(markers.get(0).getPosition().latitude, markers.get(0).getPosition().longitude);
                com.google.maps.model.LatLng point2 = new com.google.maps.model.LatLng(markers.get(1).getPosition().latitude, markers.get(1).getPosition().longitude);
                DirectionsApiRequest request = DirectionsApi.newRequest(context).origin(point1).destination(point2);

                DirectionsResult result = new DirectionsResult();
                try {
                    result = request.await();
                } catch (ApiException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(result.routes != null){
                    if(result.routes.length > 0) {
                        EncodedPolyline encPolyline = result.routes[0].overviewPolyline;
                        PolylineOptions polylineOptions = new PolylineOptions();
                        Polyline polyline;
                        LatLng linePoint = null;
                        for (com.google.maps.model.LatLng point : encPolyline.decodePath()) {
                            linePoint = new LatLng(point.lat, point.lng);
                            polylineOptions.add(linePoint);
                        }
                        for (Polyline pol : polylines) {
                            pol.remove();
                        }
                        polylines.clear();
                        polyline = mMap.addPolyline(polylineOptions);
                        polOptions.add(polylineOptions);
                        polylines.add(polyline);

                        mMap.moveCamera(CameraUpdateFactory.newLatLng(linePoint));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(17), 2000, null);
                    }
                }else{
                    Toast.makeText(MapsActivity.this, "Check your internet connection", Toast.LENGTH_SHORT).show();
                }

            }

        });
    }

    private boolean getPermissions(){
        final boolean granted = false;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            new AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    MY_PERMISSIONS_REQUEST_LOCATION );
                        }
                    })
                    .create()
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == MY_PERMISSIONS_REQUEST_LOCATION){
            if(permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                permissionGranted = true;
            }
        }
    }
}

