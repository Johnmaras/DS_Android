package com.example.john.testing_the_maps;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.EncodedPolyline;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

//TODO listen for gps state changes and hide/reveal the my location marker accordingly
//TODO optimize london_file
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private final String ApiKey = "AIzaSyAa5T-N6-BRrJZSK0xlSrWlTh-C7RjOVdY";
    private final GeoApiContext context = new GeoApiContext();

    private GoogleMap mMap;

    private MarkerOptions options = new MarkerOptions();
    private ArrayList<LatLng> londonBounds;
    private ArrayList<Marker> markers = new ArrayList<>();
    private ArrayList<Polyline> polylines = new ArrayList<>();
    private ArrayList<PolylineOptions> polOptions = new ArrayList<>();

    private int olderMarker = 0;

    private static File london_file;


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

        context.setQueryRateLimit(3)
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
        //String url = "http://polygons.openstreetmap.fr/get_poly.py?id=65606&params=0"; //bulk
        String url = "http://polygons.openstreetmap.fr/get_geojson.py?id=65606&params=0"; // json
        GetBounds foo = new GetBounds();
        foo.execute(url);
    }

    private class GetBounds extends AsyncTask<String, Void, ArrayList<LatLng>>{

        @Override
        protected ArrayList<LatLng> doInBackground(String... params) {

            if(london_file == null || !london_file.exists()){
                Log.e("MapsActivity_GetBounds", "london_file is null or it doesn't exist");
                try{
                    london_file  = new File(MapsActivity.this.getFilesDir(), "london.json");

                    String url = params[0];
                    URL website = new URL(url);

                    FileUtils.copyURLToFile(website, london_file, 10000, 10000);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            ArrayList<LatLng> bounds = new ArrayList<>();

            try {
                JsonParser parser = new JsonParser();
                FileReader fr = new FileReader(london_file);
                Object obj = parser.parse(fr);

                JsonObject obj2 = (JsonObject)obj;
                JsonArray geometries = (JsonArray)obj2.get("geometries");
                JsonObject geo2 = (JsonObject)geometries.get(0);
                JsonArray coordinates = (JsonArray)geo2.get("coordinates"); //has two arrays
                JsonArray array = (JsonArray) coordinates.get(0);
                JsonArray array0 = (JsonArray) array.get(0);

                Iterator it_inner = array0.iterator();
                LatLng point;
                while(it_inner.hasNext()){
                    JsonArray tuple = (JsonArray)it_inner.next();
                    double longitude = tuple.get(0).getAsDouble();
                    double latitude = tuple.get(1).getAsDouble();
                    point = new LatLng(latitude, longitude);
                    bounds.add(point);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return bounds;
        }

        @Override
        protected void onPostExecute(ArrayList<LatLng> latLngs) {

            londonBounds = latLngs;

            PolygonOptions londonPolygon = new PolygonOptions();
            List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(30), new Gap(20));
            londonPolygon.strokePattern(pattern);
            londonPolygon.strokeColor(getResources().getColor(R.color.AreaBounds));

            londonPolygon.addAll(londonBounds);
            double maxLat = Double.MIN_VALUE;
            double maxLng = Double.MIN_VALUE;
            double minLat = Double.MAX_VALUE;
            double minLng = Double.MAX_VALUE;
            for(LatLng point: londonBounds){
                double lat = point.latitude;
                double lng = point.longitude;
                if(lat > maxLat) maxLat = lat;
                if(lat < minLat) minLat = lat;
                if(lng > maxLng) maxLng = lng;
                if(lng < minLng) minLng = lng;
            }
            Log.e("Maps_onPost", "maxLat = " + maxLat);
            Log.e("Maps_onPost", "maxLng = " + maxLng);
            Log.e("Maps_onPost", "minLat = " + minLat);
            Log.e("Maps_onPost", "minLng = " + minLng);
            LatLng point1 = new LatLng(maxLat, maxLng);
            LatLng point2 = new LatLng(minLat, minLng);
            mMap.addPolygon(londonPolygon);
            LatLngBounds latLngBounds = new LatLngBounds(point2, point1);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 32), 2000, null);
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

