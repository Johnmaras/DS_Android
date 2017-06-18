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

import com.example.john.testing_the_maps.Messages.Message;
import com.example.john.testing_the_maps.PointAdapter.Coordinates;
import com.example.john.testing_the_maps.PointAdapter.CoordinatesDeserializer;
import com.example.john.testing_the_maps.PointAdapter.CoordinatesSerializer;
import com.example.john.testing_the_maps.PointAdapter.LatLngAdapter;
import com.example.john.testing_the_maps.PointAdapter.LatLngAdapterDeserializer;
import com.example.john.testing_the_maps.PointAdapter.LatLngAdapterSerializer;
import com.example.john.testing_the_maps.PointAdapter.PolylineAdapter;
import com.example.john.testing_the_maps.PointAdapter.PolylineAdapterDeserializer;
import com.example.john.testing_the_maps.PointAdapter.PolylineAdapterSerializer;
import com.example.john.testing_the_maps.point_in_polygon.Point;
import com.example.john.testing_the_maps.point_in_polygon.Polygon;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
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
    private ArrayList<Polyline> polylines = new ArrayList<>();
    private ArrayList<Circle> circles = new ArrayList<>();

    private static String masterIP = null;
    private static int masterPort = 0;

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

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setOnMapClickListener(this);

        context.setQueryRateLimit(3)
               .setConnectTimeout(1, TimeUnit.SECONDS)
               .setReadTimeout(1, TimeUnit.SECONDS)
               .setWriteTimeout(1, TimeUnit.SECONDS).setApiKey(ApiKey);


        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            int index;
            @Override
            public void onMarkerDragStart(Marker marker){
                index = markers.indexOf(marker);
                circles.get(index).remove();
                circles.remove(index);
            }
            @Override
            public void onMarkerDrag(Marker marker) {
                String snippet = marker.getPosition().toString();
                markers.get(index).setSnippet(snippet);
                if(markers.get(index).isInfoWindowShown())markers.get(index).showInfoWindow();
            }

            @Override
            public void onMarkerDragEnd(Marker marker){
                CircleOptions co = new CircleOptions();
                co.center(marker.getPosition());
                co.radius(555);
                circles.add(index, mMap.addCircle(co));
                String snippet = marker.getPosition().toString();
                markers.get(index).setSnippet(snippet);
                if(markers.get(index).isInfoWindowShown())markers.get(index).showInfoWindow();
            }

        });

        final Button BtnClearMarkers = (Button) findViewById(R.id.btnClearMarkers);

        BtnClearMarkers.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                for(Marker marker: markers){
                    marker.remove();
                }
                for(Circle circle: circles){
                    circle.remove();
                }
                circles.clear();
                markers.clear();
                olderMarker = 0;
            }
        });

        final Button BtnClearMap = (Button)findViewById(R.id.btnClearMap);

        BtnClearMap.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                for(Polyline pl: polylines){
                    pl.remove();
                }
                polylines.clear();

                if(markers.isEmpty()){
                    for(Circle c: circles){
                        c.remove();
                    }
                    circles.clear();
                }
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

                        Marker m2 = markers.get(1);

                        LatLngAdapter origin;
                        LatLngAdapter dest;
                        if(m1.getTitle().equals("Origin")){
                            origin = new LatLngAdapter(m1.getPosition().latitude, m1.getPosition().longitude);
                            dest = new LatLngAdapter(m2.getPosition().latitude, m2.getPosition().longitude);
                        }else{
                            dest = new LatLngAdapter(m1.getPosition().latitude, m1.getPosition().longitude);
                            origin = new LatLngAdapter(m2.getPosition().latitude, m2.getPosition().longitude);
                        }

                        DirectionsRequest request = new DirectionsRequest();
                        request.execute(origin, dest);

                     }
                }else{
                    Toast.makeText(MapsActivity.this, "You must place two markers", Toast.LENGTH_SHORT).show();
                }
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
            if(londonPolygon.contains(new Point((float) latLng.latitude, (float) latLng.longitude))) {
                if(!markers.isEmpty() && markers.size() == 2){
                    markers.get(olderMarker).remove();
                    markers.remove(olderMarker);
                    circles.get(olderMarker).remove();
                    circles.remove(olderMarker);
                }
                options.position(latLng);
                options.draggable(true);
                options.title((olderMarker + 1 == 1) ? "Origin" : "Destination");

                String snippet = options.getPosition().toString();
                options.snippet(snippet);

                options.icon(BitmapDescriptorFactory.defaultMarker(
                            options.getTitle().equals("Origin") ?
                                     BitmapDescriptorFactory.HUE_GREEN:
                                     BitmapDescriptorFactory.HUE_RED));

                markers.add(olderMarker, mMap.addMarker(options));

                CircleOptions circleOptions = new CircleOptions();
                circleOptions.radius(555);
                circleOptions.center(markers.get(olderMarker).getPosition());
                circles.add(olderMarker, mMap.addCircle(circleOptions));

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

    private class DirectionsRequest extends AsyncTask<LatLngAdapter, Void, String> {

        final GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson;

        @Override
        protected String doInBackground(LatLngAdapter... latLngs) {
            Socket masterCon = null;
            String results = null;

            int i = 0;
            while(masterCon == null && i < 10){
                try{

                    gsonBuilder.registerTypeAdapter(PolylineAdapter.class, new PolylineAdapterDeserializer());
                    gsonBuilder.registerTypeAdapter(PolylineAdapter.class, new PolylineAdapterSerializer());
                    gsonBuilder.registerTypeAdapter(LatLngAdapter.class, new LatLngAdapterDeserializer());
                    gsonBuilder.registerTypeAdapter(LatLngAdapter.class, new LatLngAdapterSerializer());
                    gsonBuilder.registerTypeAdapter(Coordinates.class, new CoordinatesDeserializer());
                    gsonBuilder.registerTypeAdapter(Coordinates.class, new CoordinatesSerializer());
                    gson = gsonBuilder.create();

                    masterCon = new Socket(InetAddress.getByName(masterIP), masterPort);
                    ObjectOutputStream out = new ObjectOutputStream(masterCon.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(masterCon.getInputStream());

                    in.readBoolean();

                    Coordinates query = new Coordinates(latLngs[0], latLngs[1]);
                    Message message = new Message(9, query);
                    String messageJson = gson.toJson(message, Message.class);
                    System.out.println(messageJson);

                    out.writeObject(messageJson);
                    out.flush();

                    results = (String)in.readObject();

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
        protected void onPostExecute(String s) {
            if(s != null && !s.isEmpty()){
                PolylineAdapter polylineAdapter = gson.fromJson(s, PolylineAdapter.class);
                PolylineOptions polylineOptions = new PolylineOptions();
                try{
                    for(LatLngAdapter point : polylineAdapter.getPoints()){
                        polylineOptions.add(toAndroidLatLng(toLatLng(point)));
                    }
                    polylines.add(mMap.addPolyline(polylineOptions));
                }catch (NullPointerException e){
                    Log.e("Polyline", "Empty route");
                }
            }
        }
    }
}