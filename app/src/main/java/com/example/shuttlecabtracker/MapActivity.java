package com.example.shuttlecabtracker;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.location.*;

import com.example.hci.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.*;

import java.util.*;

import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MapActivity extends AppCompatActivity  implements TaskLoadedCallback {
    Location publicLocation = new Location("dummy");
    DatabaseReference shuttledb;
    MapView mapView;
    GoogleMap map;
    ArrayList<LatLng> cabLocs = new ArrayList<LatLng>();
    LatLng closestCab;
    Polyline currentPolyline;
    TextView timeLeft;
    String id;
    Button payButton;
    SharedPreferences sharedPreferences;
    private String m_Text="";
    private FusedLocationProviderClient fusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getPreferences(MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        timeLeft = findViewById(R.id.timeText);
        payButton = findViewById(R.id.payButton);
        // Gets the MapView from the XML layout and creates it
        shuttledb = FirebaseDatabase.getInstance().getReference();
        mapView = findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pay();
            }
        });
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                        }
                        double x = location.getLatitude();
                        double y = location.getLongitude();
                        publicLocation=location;
                        Button b1 = findViewById(R.id.refButton);
                        b1.performClick();
                    }

                });
        ;



        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                if (ActivityCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                    ActivityCompat.requestPermissions(MapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    return;
                } else {
                    map.setMyLocationEnabled(true);
                    map.getUiSettings().setMyLocationButtonEnabled(true);
                    // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
                    MapsInitializer.initialize(MapActivity.this);
                    // Updates the location and zoom of the MapView
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(publicLocation.getLatitude(), publicLocation.getLongitude()),15);
                    map.animateCamera(cameraUpdate);
                    // Gets to GoogleMap from the MapView and does initialization stuff
                    // Write you code here if permission already given.
                    FirebaseDatabase.getInstance().getReference()
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    map.clear();
                                    cabLocs.clear();
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        shuttleCabs shuttleCab = snapshot.getValue(shuttleCabs.class);
                                        LatLng shuttle = new LatLng(shuttleCab.getCabx(),shuttleCab.getCaby() );
                                        if (shuttleCab.getType()==0){
                                            if (shuttleCab.getFull()==0){
                                                cabLocs.add(shuttle);
                                            }
                                        }
                                    }
                                    closestCab = findClosest(cabLocs);
                                    map.addMarker(new MarkerOptions().position(closestCab).title("Closest Shuttle"));
                                    LatLng publicLatLng = new LatLng(publicLocation.getLatitude(),publicLocation.getLongitude());
                                    String url = getDirectionsUrl(publicLatLng,closestCab);
                                    new FetchURL(MapActivity.this   ).execute(url, "driving");

                                    Location dest = new Location("dummy");
                                    dest.setLatitude(closestCab.latitude);
                                    dest.setLongitude(closestCab.longitude);
                                    String timeleft = "Nearest cab ETA: " + Integer.toString((int)publicLocation.distanceTo(dest)/300)+ " minutes";
                                    timeLeft.setText(timeleft);

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                }
                            });
                }
            }

        });
        final Handler handler = new Handler();
        final int delay = 10000; //milliseconds
        handler.postDelayed(new Runnable(){
            public void run(){
                updateLocation();
                sendLocation();
                handler.postDelayed(this, delay);
            }
        }, delay);
    }
    public void updateLocation(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {

                        }
                        publicLocation=location;
                    }
                });
    }
    public void sendLocation(){
        if (sharedPreferences.getBoolean("firstrun", true)){
            id = shuttledb.push().getKey();
            sharedPreferences.edit().putBoolean("firstrun", false).apply();
            sharedPreferences.edit().putString("myID", id).apply();
        }
        else{
            id = sharedPreferences.getString("myID", "null");
        }
        double x = publicLocation.getLatitude();
        double y = publicLocation.getLongitude();
        shuttleCabs shuttleCab = new shuttleCabs(id,x,y,0, 1);
        shuttledb.child(id).setValue(shuttleCab);
//        Toast.makeText(this, "Record updated", Toast.LENGTH_SHORT).show();
    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters+"&key=AIzaSyDZTwEUFdGt92Tm6QV0gdOURgpBo-CRcHc";
//        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=12.975170,79.163938&destination=12.971590,79.163318&key=AIzaSyDZTwEUFdGt92Tm6QV0gdOURgpBo-CRcHc";
        //https://maps.googleapis.com/maps/api/directions/json?origin=Toronto&destination=Montreal &key=YOUR_API_KEY
        System.out.println(url);
        return url;
    }


    public void pay(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter password to confirm payment: ");

    // Set up the input
        final EditText input = new EditText(this);
    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

    // Set up the buttons
        builder.setPositiveButton("Pay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();
                Toast.makeText(getApplicationContext(),"Payment Successful",Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public LatLng findClosest(ArrayList<LatLng> X){
    int i;
    LatLng closestpt = new LatLng(0,0);
    double dist = 100000000;
    for (i=0;i < X.size();i++){
        Location temp = new Location("dummy");
        temp.setLongitude(X.get(i).longitude);
        temp.setLatitude(X.get(i).latitude);
        if (publicLocation.distanceTo(temp)<dist){
            dist = publicLocation.distanceTo(temp);
            closestpt = X.get(i);
        }
    }
        return closestpt;
    }

    public void refresh(View view) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                        }
                        publicLocation=location;
                        Toast.makeText(getApplicationContext(),"refreshed", Toast.LENGTH_LONG).show();
                    }

                });

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(publicLocation.getLatitude(), publicLocation.getLongitude()), 15);
        map.animateCamera(cameraUpdate);



    }

    @Override
    protected void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline!= null){
            currentPolyline.remove();
        }
        currentPolyline = map.addPolyline((PolylineOptions)values[0]);
    }
}
