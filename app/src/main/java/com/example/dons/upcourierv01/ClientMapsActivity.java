package com.example.dons.upcourierv01;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ClientMapsActivity extends FragmentActivity implements OnMapReadyCallback {


    private GoogleMap mMap;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient mFusedLocationClient;
    private LatLng pickUpLocation;

    private Button mClientLogout, mRequest, mSettings;
    private Marker pickupMarker;
    private Boolean requestBol = false;

    private String clientDestination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mClientLogout = (Button) findViewById(R.id.btnLogOutCourier);
        mRequest = (Button) findViewById(R.id.btnRequest);
        mSettings = (Button) findViewById(R.id.btnSettingsCourier);
        mClientLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                //Volvemos al menu principal
                Intent intent = new Intent(ClientMapsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBol){
                    requestBol = false;
                    geoQuery.removeAllListeners();
                    courierLocationRef.removeEventListener(courierLocationRefListener);

                    if (courierFoundID != null){
                        DatabaseReference courierRef = FirebaseDatabase.getInstance().getReference().child("Users")
                                .child("Couriers").child(courierFoundID).child("clientRequest");
                        courierRef.removeValue();
                        courierFoundID = null;
                    }

                    courierFound = false;
                    mRadius = 1;

                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("clientRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userID);

                    if (pickupMarker != null){
                        pickupMarker.remove();
                    }

                    if (mCourierMarker != null){
                        mCourierMarker.remove();
                    }

                    mRequest.setText("Llamar Mensajero");

                } else {
                    requestBol = true;
                    //Guardamos el courierID junto con el clientID y la ubicación
                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("clientRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userID, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    //Añadimos un marker para que el mensajero pueda visualizar la ubicación del request
                    pickUpLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Recojer Aqui")
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));

                    mRequest.setText("Llamando a tu mensajero");

                    getClosestDriver();
                }
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ClientMapsActivity.this, ClientSettingsActivity.class);
                startActivity(intent);
                return;

            }
        });

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NotNull Place place) {
                // TODO: Get info about the selected place.
                clientDestination = place.getName().toString();

            }

            @Override
            public void onError(@NotNull Status status) {
            }
        });

    }

    private int mRadius = 1;
    private Boolean courierFound = false;
    private String courierFoundID;

    GeoQuery geoQuery;

    private void getClosestDriver() {
        DatabaseReference courierLocation = FirebaseDatabase.getInstance().getReference().child("couriersAvailable");
        GeoFire geoFire = new GeoFire(courierLocation);
        //Para hallar el courier mas cercano, iremos buscando en un radio respecto a la solicitud.
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUpLocation.latitude, pickUpLocation.longitude), mRadius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!courierFound && requestBol){
                    //Si se ha encontrado un courier
                    courierFound = true;
                    courierFoundID = key;


                    //Añadimos un log para ver el mensaje por consola
                    Log.d("UpCourierTest","Courier encontrado satisfactoriamente, con la clave: "+courierFoundID);

                    DatabaseReference courierRef = FirebaseDatabase.getInstance().getReference().child("Users")
                            .child("Couriers").child(courierFoundID).child("clientRequest");
                    String clientID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    HashMap map = new HashMap();
                    map.put("clientRideId", clientID);
                    map.put("clientDestination", clientDestination);

                    courierRef.updateChildren(map);
                    getCourierLocation();
                    mRequest.setText("Buscando al mensajero mas cercano");
                }
            }
            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
                if(!courierFound) {
                    //Si no encontramos un courier dentro del radio, incrementamos
                    mRadius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });
    }

    private Marker mCourierMarker;
    private DatabaseReference courierLocationRef;
    private ValueEventListener courierLocationRefListener;

    private void getCourierLocation() {
        courierLocationRef = FirebaseDatabase.getInstance().getReference()
                .child("couriersWorking").child(courierFoundID).child("l");
        courierLocationRefListener = courierLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Mensajero encontrado");
                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng courierLatLng = new LatLng(locationLat, locationLng);

                    if(mCourierMarker != null){
                        mCourierMarker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickUpLocation.latitude);
                    loc1.setLongitude(pickUpLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(courierLatLng.latitude);
                    loc2.setLongitude(courierLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance < 100){
                        mRequest.setText("El Mensajero Llego");
                    }
                    else{
                        mRequest.setText("Mensajero encontrado: " + String.valueOf(distance));
                    }

                    mCourierMarker = mMap.addMarker(new MarkerOptions().position(courierLatLng)
                            .title("Mensajero Aqui").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Método para obtener la ubicación cada cierto tiempo
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //Mejor ubicacion posible

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED){

            } else {
                checkLocationPermission();
            }
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext()!= null){
                    mLastLocation = location;
                    //Latitud Longitud
                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                    //Zoom para la camara
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
                }
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("Location permission")
                        .setMessage("Debe otorgar los permisos de ubicación")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(ClientMapsActivity.this,
                                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(ClientMapsActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(),"Porfavor, otorgue los permisos",Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

}
