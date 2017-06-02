package com.example.john.testing_the_maps;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.john.testing_the_maps.point_in_polygon.Point;
import com.example.john.testing_the_maps.point_in_polygon.Polygon;
import com.github.clans.fab.FloatingActionButton;
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
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.Bounds;
import com.google.maps.model.GeocodingResult;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
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
    private final Polygon.Builder londonBounds = new Polygon.Builder();
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
        isOnline(this);
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
            //Marker originalMarker;
            @Override
            public void onMarkerDragStart(Marker marker) {
                //originalMarker = marker;
            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                /*Point newMarker = new Point((float)marker.getPosition().latitude, (float)marker.getPosition().longitude);
                Polygon londonPolygon = londonBounds.build();
                if(!londonPolygon.contains(newMarker)){
                    marker = originalMarker;
                }*/
            }

        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng){
                Polygon londonPolygon = londonBounds.build();
                if(londonPolygon.contains(new Point((float)latLng.latitude, (float)latLng.longitude))) {
                    if (!markers.isEmpty() && markers.size() == 2) {
                        markers.get(olderMarker).remove();
                        markers.remove(olderMarker);
                    }
                    options.position(latLng);
                    options.draggable(true);
                    options.title((olderMarker + 1 == 1) ? "Origin" : "Destination");
                    options.snippet("Testing the Maps");
                    markers.add(olderMarker, mMap.addMarker(options));
                    olderMarker = olderMarker == 0 ? 1 : 0;
                }else{
                    Toast.makeText(MapsActivity.this, "Place a marker inside the London bounds", Toast.LENGTH_SHORT).show();
                }
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

        final FloatingActionButton BtnSettings = (FloatingActionButton) findViewById(R.id.btnSettings);

        BtnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settings = new Intent(v.getContext(), Settings.class);
                startActivity(settings);
            }
        });

        final FloatingActionButton BtnGetDirs = (FloatingActionButton) findViewById(R.id.btnGetDirections);

        BtnGetDirs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){ //the workers will determine which is the origin and which the destination
                if(markers.size() == 2){
                    if(isOnline(MapsActivity.this)){
                        Marker m1 = markers.get(0);
                        m1.setTag(m1.getTitle());

                        Marker m2 = markers.get(1);
                        m2.setTag(m2.getTitle());

                        LatLngAdapter lla1 = new LatLngAdapter(m1.getPosition().latitude, m1.getPosition().longitude);
                        LatLngAdapter lla2 = new LatLngAdapter(m2.getPosition().latitude, m2.getPosition().longitude);

                        DirectionsRequest request = new DirectionsRequest();
                        request.execute(lla1, lla2);
                    }
                }else{
                    Toast.makeText(MapsActivity.this, "You must place two markers", Toast.LENGTH_SHORT).show();
                }
                /*com.google.maps.model.LatLng point1 = new com.google.maps.model.LatLng(markers.get(0).getPosition().latitude, markers.get(0).getPosition().longitude);
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
                }*/
            }
        });
    }

    private void getLondon(){
        //String url = "http://polygons.openstreetmap.fr/get_poly.py?id=65606&params=0"; //bulk
        String url = "http://polygons.openstreetmap.fr/get_geojson.py?id=65606&params=0"; // json
        GetBounds foo = new GetBounds();
        foo.execute(url);
    }

    private LatLng toAndroidLatLng(com.google.maps.model.LatLng point){
        return new LatLng(point.lat, point.lng);
    }

    public static boolean isOnline(Context context){
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if(netInfo != null && netInfo.isConnected()){
            return true;
        }
        Toast.makeText(context, "Please check your internet connection", Toast.LENGTH_SHORT).show();
        return false;
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

            try{
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

                    londonBounds.addVertex(new Point((float)latitude, (float)longitude));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return bounds;
        }

        @Override
        protected void onPostExecute(ArrayList<LatLng> latLngs) {
            Bounds londonCenter = new Bounds();
            try {
                GeocodingResult[] l = GeocodingApi.newRequest(context).address("London").region("uk").await();
                londonCenter = l[0].geometry.bounds;
            } catch (ApiException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            LatLngBounds londonLatLngBouns = new LatLngBounds(toAndroidLatLng(londonCenter.southwest), toAndroidLatLng(londonCenter.northeast));

            PolygonOptions londonPolygon = new PolygonOptions();
            List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(30), new Gap(20));
            londonPolygon.strokePattern(pattern);
            londonPolygon.strokeColor(getResources().getColor(R.color.AreaBounds));

            londonPolygon.addAll(latLngs);

            mMap.addPolygon(londonPolygon);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(londonLatLngBouns, 32), 2000, null);
        }
    }

    private class DirectionsRequest extends AsyncTask<LatLngAdapter, Integer, PolylineAdapter/*under consideration*/> {
        @Override
        protected PolylineAdapter doInBackground(LatLngAdapter... latLngs) {
            Socket masterCon = null;
            PolylineAdapter results = null;

            int i = 0;
            while(masterCon == null && i < 10){
                try{
                    //TODO get master ip and port from config file or global variable from the settings activity
                    masterCon = new Socket(InetAddress.getByName("127.0.0.1"), 4000);
                    ObjectOutputStream out = new ObjectOutputStream(masterCon.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(masterCon.getInputStream());

                    Message message = new Message();
                    message.setRequestType(9);

                    ArrayList<LatLngAdapter> points = new ArrayList<>();
                    points.add(latLngs[0]);
                    points.add(latLngs[1]);
                    message.setQuery(points);

                    out.writeObject(message);
                    out.flush();

                    results = (PolylineAdapter) in.readObject();

                } catch (UnknownHostException e) {
                    Log.e("DirectionsRequest_back", "Host not found!");
                }catch(IOException e){
                    Log.e("DirectionsRequest_back", "Error on trying to connect to master");
                } catch (ClassNotFoundException e) {
                    Log.e("DirectionsRequest_back", "Error on trying to read the results");
                }

                i++;
            }
            return results;
        }

        @Override
        protected void onPostExecute(PolylineAdapter polyline) {
            if((polyline == null || polyline.isEmpty()) && isOnline(MapsActivity.this)){
                Log.e("DirectionsRequest_post", "Bad results");
                Toast.makeText(MapsActivity.this, "Bad Results!", Toast.LENGTH_SHORT).show();
            }else if(polyline != null && !polyline.isEmpty() && !isOnline(MapsActivity.this)){
                Log.e("DirectionsRequest_post", "No internet connection");
                Toast.makeText(MapsActivity.this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
            }
        }
    }
}