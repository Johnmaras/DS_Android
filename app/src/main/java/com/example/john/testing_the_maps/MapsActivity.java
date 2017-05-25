package com.example.john.testing_the_maps;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.EncodedPolyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;




//TODO listen for gps state changes and hide/reveal the my location marker accordingly
//TODO get directions asynchronously
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private final String ApiKey = "AIzaSyAa5T-N6-BRrJZSK0xlSrWlTh-C7RjOVdY";

    private GoogleMap mMap;

    private MarkerOptions options = new MarkerOptions();
    private ArrayList<LatLng> london;
    private ArrayList<Marker> markers = new ArrayList<>();
    private ArrayList<Polyline> polylines = new ArrayList<>();
    private ArrayList<PolylineOptions> polOptions = new ArrayList<>();

    private int olderMarker = 0;

    private static final File london_file = new File("london");


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

        getLondon();

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        final GeoApiContext context = new GeoApiContext()
                .setQueryRateLimit(3)
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS).setApiKey(ApiKey);

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
                options.title((olderMarker + 1 == 1) ? "Start" : "Destination");
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

                if(result != null){
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

    private void getLondon(){
        String url = "http://polygons.openstreetmap.fr/get_poly.py?id=65606&params=0"; //bulk
        //String url = "http://polygons.openstreetmap.fr/get_geojson.py?id=65606&params=0"; // json
        GetBounds foo = new GetBounds();
        foo.execute(url);
    }

    private class GetBounds extends AsyncTask<String, Void, ArrayList<LatLng>>{

        @Override
        protected ArrayList<LatLng> doInBackground(String... params) {
            //TODO download data from http://polygons.openstreetmap.fr/get_poly.py?id=65606&params=0

            if(!london_file.exists()){
                try{
                    //TODO needs write to filesystem permission
                    london_file.createNewFile();

                    String url = params[0];
                    URL website = new URL(url);

                    ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                    FileOutputStream fos = new FileOutputStream(london_file);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                    fos.close();
                    rbc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            ArrayList<LatLng> bounds = new ArrayList<>();

            try{
                BufferedReader reader = new BufferedReader(new FileReader(london_file));
                LatLng point;
                String line = reader.readLine();
                while(line != null){
                    String[] coords = line.trim().split("\\s+");
                    double longitude = Double.parseDouble(coords[0].trim());
                    double latitude = Double.parseDouble(coords[1].trim());
                    point = new LatLng(latitude, longitude);
                    bounds.add(point);
                    line = reader.readLine();
                }
            }catch(FileNotFoundException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }
            return bounds;
        }

        @Override
        protected void onPostExecute(ArrayList<LatLng> latLngs) {
            london = latLngs;
            PolygonOptions londonPolygon = new PolygonOptions();
            londonPolygon.addAll(london);
            mMap.addPolygon(londonPolygon);
        }
    }

    private class DirectionsRequest extends AsyncTask<LatLngAdapter, Void, PolylineAdapter/*under consideration*/> {
        @Override
        protected PolylineAdapter doInBackground(LatLngAdapter... latLngs) {
            return null;
        }

        @Override
        protected void onPostExecute(PolylineAdapter polyline) {
            super.onPostExecute(polyline);
        }
    }

}

