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
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

//based on the TestThePolyline AsyncTask we understand that Serialization is not a feasible way of exchanging data via Sockets.
//Json on the other hand is platform independent and thus ideal for our purpose.
//the one polyline gathered from the DataGatherer is valid and the Json Serialization/Deserialization works like a charm

//TODO listen for gps state changes and hide/reveal the my location marker accordingly
//TODO optimize london_file
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener{

    public static final String MESSAGE_IP = "Testing_the_Maps.IP";
    public static final String MESSAGE_PORT = "Testing_the_Maps.Port";
    private static final int REQUEST_CODE = 2610;

    private final String ApiKey = "AIzaSyAa5T-N6-BRrJZSK0xlSrWlTh-C7RjOVdY";
    private final GeoApiContext context = new GeoApiContext();

    private GoogleMap mMap;

    private MarkerOptions options = new MarkerOptions();
    private final Polygon.Builder londonBounds = new Polygon.Builder();
    private ArrayList<Marker> markers = new ArrayList<>();
    //private ArrayList<Polyline> polylines = new ArrayList<>();
    //private ArrayList<PolylineOptions> polOptions = new ArrayList<>();

    private static String masterIP;
    private static int masterPort;

    private int olderMarker = 0;

    private static File london_file;
    private static LatLngBounds londonLatLngBounds;


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

        new TestThePolyline().execute();

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setOnMapClickListener(this);

        context.setQueryRateLimit(3)
               .setConnectTimeout(1, TimeUnit.SECONDS)
               .setReadTimeout(1, TimeUnit.SECONDS)
               .setWriteTimeout(1, TimeUnit.SECONDS).setApiKey(ApiKey);

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }
            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker marker){
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
                settings.putExtra(MESSAGE_IP, masterIP);
                settings.putExtra(MESSAGE_PORT, masterPort);
                startActivityForResult(settings, REQUEST_CODE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            masterIP = data.getExtras().getString(MESSAGE_IP);
            masterPort = data.getExtras().getInt(MESSAGE_PORT);
        }
        Toast.makeText(MapsActivity.this, masterIP + " " + masterPort, Toast.LENGTH_SHORT).show();
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

    private com.google.maps.model.LatLng toLatLng(LatLngAdapter point){
        return new com.google.maps.model.LatLng(point.getLatitude(), point.getLongitude());
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

    @Override
    public void onMapClick(LatLng latLng) {
        synchronized(londonBounds) {
            Polygon londonPolygon = londonBounds.build();
            if (londonPolygon.contains(new Point((float) latLng.latitude, (float) latLng.longitude))) {
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
            } else {
                Toast.makeText(MapsActivity.this, "Place a marker inside the London bounds", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class GetBounds extends AsyncTask<String, Void, ArrayList<LatLng>>{

        @Override
        protected ArrayList<LatLng> doInBackground(String... params) {

            london_file  = new File(MapsActivity.this.getFilesDir(), "london.json");
            //FIXME the file is still present. the exists() doesn't work as expected
            if(!london_file.exists()){
                Log.e("MapsActivity_GetBounds", "london_file doesn't exist");
                try{
                    String url = params[0];
                    URL website = new URL(url);

                    FileUtils.copyURLToFile(website, london_file, 10000, 10000);

                }catch(UnknownHostException e){
                    Log.e("MapsActivity_GetBounds", "Host Not Found!");
                }catch(FileNotFoundException e){
                    Log.e("MapsActivity_GetBounds", "File Not Found!");
                }catch(MalformedURLException e){
                    Log.e("MapsActivity_GetBounds", "Wrong URL format");
                }catch(IOException e){
                    Log.e("MapsActivity_GetBounds", "There was an IO Error");
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
                Log.e("MapsActivity_GetBounds", "File not found");
            }

            return bounds;
        }

        @Override
        protected void onPostExecute(ArrayList<LatLng> latLngs) {
            Bounds londonCenter = new Bounds();

            GeocodingResult[] l = null;
            try {
                l = GeocodingApi.newRequest(context).address("London").region("uk").await();
                londonCenter = l[0].geometry.bounds;
            } catch (UnknownHostException e) {
                Log.e("MapsActivity_GetBOnPost", "Host not found!");
            } catch (ApiException e) {
                Log.e("MapsActivity_GetBOnPost", "Google API Exception");
            } catch (InterruptedException e) {
                Log.e("MapsActivity_GetBOnPost", "Google API call interrupted");
            } catch (IOException e) {
                Log.e("MapsActivity_GetBOnPost", "There was an IO Error");
            }

            if(latLngs != null && !latLngs.isEmpty()) {
                PolygonOptions londonPolygon = new PolygonOptions();
                List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(30), new Gap(20));
                londonPolygon.strokePattern(pattern);
                londonPolygon.strokeColor(getResources().getColor(R.color.AreaBounds));

                londonPolygon.addAll(latLngs);

                mMap.addPolygon(londonPolygon);
            }

            if(l != null){
                londonLatLngBounds = new LatLngBounds(toAndroidLatLng(londonCenter.southwest), toAndroidLatLng(londonCenter.northeast));
            }

            if(londonLatLngBounds != null){
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(londonLatLngBounds, 32), 2000, null);
            }else{
                Log.e("MapsActivity_GetBOnPost", "Could not get the london bounds");
            }

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
                    masterCon = new Socket(InetAddress.getByName("192.168.1.67"), 4000);
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

    private class TestThePolyline extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... params) {
            Socket masterCon = null;
            String results = null;

            int i = 0;
            while(masterCon == null && i < 10){
                try{
                    masterCon = new Socket(InetAddress.getByName("192.168.1.67"), 4000);
                    ObjectInputStream in = new ObjectInputStream(masterCon.getInputStream());

                    /*long size = in.readLong(); //get the length of the file
                    byte[] fileIn = new byte[(int) size]; //create a byte array
                    in.readFully(fileIn); //get the bytes of the file
                    FileOutputStream fos = new FileOutputStream(new File("polyline_example"));
                    ObjectOutputStream os = new ObjectOutputStream(fos);

                    os.write(fileIn);
                    os.flush();
                    os.close();*/

                    results = in.readUTF();

                    /*FileInputStream fis = new FileInputStream(new File("polyline_example"));
                    ObjectInputStream ois = new ObjectInputStream(fis);*/



                } catch (UnknownHostException e) {
                    Log.e("DirectionsRequest_back", "Host not found!");
                }catch(IOException e){
                    Log.e("DirectionsRequest_back", "Error on trying to connect to master");
                }

                i++;
            }
            return results;
        }

        @Override
        protected void onPostExecute(String polylinesJson){

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(PolylineAdapter.class, new PolylineAdapterDeserializer());
            Gson gson = gsonBuilder.create();
            //gsonBuilder.setPrettyPrinting();

            PolylineOptions po = new PolylineOptions();
            PolylineAdapter pl = gson.fromJson(polylinesJson, PolylineAdapter.class);
            for(LatLngAdapter point: pl.getPoints()){
                    po.add(toAndroidLatLng(toLatLng(point)));
            }
            mMap.addPolyline(po);
        }
    }
}