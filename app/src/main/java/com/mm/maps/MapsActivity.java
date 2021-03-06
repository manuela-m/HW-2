package com.mm.maps;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback,GoogleMap.OnMarkerClickListener,GoogleMap.OnMapLongClickListener, SensorEventListener {

    private GoogleMap mMap;
    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;
    List<Marker> markerList;
    List<LatLng> latLngList;
    FloatingActionButton mBUTTONdot;
    FloatingActionButton mBUTTONx;
    ViewGroup mMainView;
    ViewGroup mAccelerationLabel;

    private SensorManager sensorManager;
    Sensor accelerometr;
    TextView mParameters;

    boolean accelerometrFlag=false;
    boolean buttonsFlag=false;
    Button buttonIN, buttonOUT,clearButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBUTTONdot =findViewById(R.id.fab_dot);
        mBUTTONdot.setVisibility(View.INVISIBLE);
        mBUTTONx =findViewById(R.id.fab_x);
        mBUTTONx.setVisibility(View.INVISIBLE);
        mMainView=findViewById(R.id.mainView);
        mAccelerationLabel=findViewById(R.id.accelerationLabel);
        mAccelerationLabel.setVisibility(View.INVISIBLE);
        mParameters=findViewById(R.id.accelerationParms);
        buttonIN=findViewById(R.id.buttonIN);
        buttonOUT=findViewById(R.id.buttonOUT);
        clearButton=findViewById(R.id.clear_button);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient= LocationServices.getFusedLocationProviderClient(this);

        markerList = new ArrayList<>();
        latLngList=new ArrayList<>();
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometr=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MapsActivity.this, accelerometr,SensorManager.SENSOR_DELAY_NORMAL);



        buttonIN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.moveCamera(CameraUpdateFactory.zoomIn());
            }
        });

        buttonOUT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.moveCamera(CameraUpdateFactory.zoomOut());
            }
        });

        mBUTTONdot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (accelerometrFlag && buttonsFlag) {
                    displayAnimationAcceleration(mAccelerationLabel);
                    accelerometrFlag=false;
                } else if(accelerometrFlag==false && buttonsFlag==true){
                    hideAnimationAcceleration(mAccelerationLabel);
                    accelerometrFlag=true;
                }}
        });

        mBUTTONx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!accelerometrFlag && buttonsFlag) {

                    hideAnimation2(mBUTTONdot);
                    hideAnimation2(mBUTTONx);
                    hideAnimationAcceleration(mAccelerationLabel);
                }else if(accelerometrFlag&&buttonsFlag)
                {
                    hideAnimation2(mBUTTONdot);
                    hideAnimation2(mBUTTONx);

                }

                buttonsFlag=false;

            }

        });

        clearButton.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                for (int i=0;i<markerList.size();i++){
                    markerList.get(i).remove();

                }
                latLngList.clear();
                mMap.clear();

            }



        });



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

        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMapLongClickListener( this);

        restoreDataJson();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){

        fusedLocationClient.requestLocationUpdates(mLocationRequest,locationCallback,null);
    }

    @Override
    protected void onDestroy() {
        saveDataJson(latLngList);
        super.onDestroy();

    }

    @Override
    public void onMapLoaded() {

        Log.i(MapsActivity.class.getSimpleName(),"MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){ // jeżeli applikacja ma możliwość używać location services
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        Task<Location> lastLocation=fusedLocationClient.getLastLocation();
        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null && mMap != null){
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),location.getLongitude())).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).title(getString(R.string.last_known_loc_msg)));

                }
            }
        });


        createLocationCallback();
        createLocationRequest();
        startLocationUpdates();
    }

    private void createLocationCallback(){
        locationCallback=new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                if(locationResult!=null){
                    if(gpsMarker !=null)gpsMarker.remove();

                    Location location = locationResult.getLastLocation();
                    gpsMarker=mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(),location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                            .alpha(0.8f).title("Twoja lokalizacja"));
                }

            }

        };


    }

    private void createLocationRequest(){
        mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }


    @Override
    public void onMapLongClick(LatLng latLng) {


        LatLng latLngPosition = new LatLng(latLng.latitude, latLng.longitude);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(latLngPosition)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                .alpha(0.8f)
                .title(String.format("Position: (%.2f, %.2f)", latLng.latitude, latLng.longitude)));

        markerList.add(marker);
        latLngList.add(latLngPosition);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        displayAnimation2(mBUTTONdot);
        displayAnimation2(mBUTTONx);
        buttonsFlag=true;
        accelerometrFlag=true;
        return false;
    }

    public void displayAnimation2(View button){
        button.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                button.getHeight(),
                0);
        animate.setDuration(500);
        animate.setFillAfter(true);
        button.startAnimation(animate);


    }
    public void displayAnimationAcceleration(View button){
        button.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                -button.getHeight(),
                0);
        animate.setDuration(500);
        animate.setFillAfter(true);
        button.startAnimation(animate);

    }

    public void hideAnimation2(View button){
        button.setVisibility(View.INVISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                0,
                button.getHeight()+200f);
        animate.setDuration(500);
        animate.setFillAfter(true);
        button.startAnimation(animate);
        button.setVisibility(View.GONE);


    }
    public void hideAnimationAcceleration(View button){
        button.setVisibility(View.INVISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                0,
                -button.getHeight()-200f);
        animate.setDuration(500);
        animate.setFillAfter(true);
        button.startAnimation(animate);
        button.setVisibility(View.GONE);


    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mParameters.setText("x: "+event.values[0]+" y: "+ event.values[1]);


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(accelerometr !=null)sensorManager.registerListener(this,accelerometr,100000);

    }

    @Override
    protected void onPause() {
        super.onPause();

        if(accelerometr !=null)sensorManager.unregisterListener(this,accelerometr);


    }



    private void saveDataJson(List<LatLng> latLngsList){
        Gson gson = new Gson();
        String to_save = gson.toJson(latLngsList);
        FileOutputStream fileOutputStream;

        try {
            fileOutputStream = openFileOutput("List.json", MODE_PRIVATE);
            FileWriter writer = new FileWriter(fileOutputStream.getFD());
            writer.write(to_save);
            writer.close();

        } catch (FileNotFoundException e){
            Log.e("Maps", "Error: "+ e.getMessage());
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    private void restoreDataJson(){
        FileInputStream fileInputStream;
        Gson gson = new Gson();
        int DEFAULT_BUFFER_SIZE = 10000;
        String json;;

        try{
            fileInputStream = openFileInput("List.json");
            FileReader reader = new FileReader((fileInputStream.getFD()));
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;

            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0){
                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }

            reader.close();
            json = builder.toString();
            Type collectionType = new TypeToken<List<LatLng>>(){}.getType();
            List<LatLng> o = gson.fromJson(json, collectionType);

            if(o != null){
                markerList.clear();
                latLngList.clear();

                for(LatLng l: o) {

                    LatLng latLngPosition = new LatLng(l.latitude, l.longitude);
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(latLngPosition)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                            .alpha(0.8f)
                            .title("Zapamietane");

                    try {
                        markerList.add(mMap.addMarker(markerOptions));
                        latLngList.add(latLngPosition);

                    } catch (Exception e){
                        Log.e("HW2lab", e.getMessage());
                    }
                }

            } else { }
        } catch (IOException e){
            Log.e("Maps", e.getMessage());
        }
    }


}
