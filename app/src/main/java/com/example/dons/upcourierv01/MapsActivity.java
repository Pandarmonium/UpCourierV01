package com.example.dons.upcourierv01;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient mFusedLocationClient;

    private Button mCourierLogout;

    private String clientId = "";

    private Boolean isLogginOut = false;

    private LinearLayout mClientInfo;

    private ImageView mClientProfileImg;

    private TextView mClientName, mClientPhone, mClientDestination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mClientInfo = (LinearLayout) findViewById(R.id.clientInfo);
        mClientProfileImg = (ImageView) findViewById(R.id.imgClientProfileDesc);
        mClientName = (TextView) findViewById(R.id.courierClientName);
        mClientPhone = (TextView) findViewById(R.id.courierClientPhone);
        mClientDestination = (TextView) findViewById(R.id.courierClientDestination);
        mCourierLogout = (Button) findViewById(R.id.btnLogOutCourier);

        mCourierLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLogginOut = true;
                disconnectCourier();
                FirebaseAuth.getInstance().signOut();
                //Volvemos al menu principal
                Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        getAssignedClient();
    }

    private void getAssignedClient() {
        String courierID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignClientRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Couriers").child(courierID).child("clientRequest").child("clientRideId");
        assignClientRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    clientId = dataSnapshot.getValue().toString();
                    getAssignedClientPickupLocation();
                    getAssignedClientDestination();
                    getAssignedClientInfo();
                } else {
                    clientId = "";
                    if(pickupMarker != null){
                        pickupMarker.remove();
                    }
                    if (assignedClientPickupLocationRefListener != null){
                        assignedClientPickupLocationRef.removeEventListener(assignedClientPickupLocationRefListener);
                    }

                    mClientInfo.setVisibility(View.GONE);
                    mClientName.setText("");
                    mClientPhone.setText("");
                    mClientDestination.setText("Destino: ---");
                    mClientProfileImg.setImageResource(R.mipmap.ic_default_user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedClientDestination() {
        String courierID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignClientRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Couriers").child(courierID).child("clientRequest").child("clientDestination");
        assignClientRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String clientDestination = dataSnapshot.getValue().toString();
                    mClientDestination.setText("Destino: "+clientDestination);
                } else {
                    mClientDestination.setText("Destino: ---");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedClientInfo() {
        mClientInfo.setVisibility(View.VISIBLE);
        DatabaseReference mClientDatabase = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Clients").child(clientId);
        mClientDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("clientName") != null) {
                        mClientName.setText(map.get("clientName").toString());
                    }

                    if(map.get("clientPhone") != null) {
                        mClientPhone.setText(map.get("clientPhone").toString());
                    }

                    if(map.get("clientImageUrl") != null){
                        Glide.with(getApplication()).load(map.get("clientImageUrl").toString()).into(mClientProfileImg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    Marker pickupMarker;
    private DatabaseReference assignedClientPickupLocationRef;
    private ValueEventListener assignedClientPickupLocationRefListener;

    private void getAssignedClientPickupLocation() {
        assignedClientPickupLocationRef = FirebaseDatabase.getInstance().getReference().
                child("clientRequest").child(clientId).child("l");

        assignedClientPickupLocationRefListener = assignedClientPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && !clientId.equals("")) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng courierLatLng = new LatLng(locationLat, locationLng);
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(courierLatLng)
                            .title("Ubicación de Recojo").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));
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

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED){
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            } else {
                checkLocationPermission();
            }
        }

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

                    //Obtenemos el id del usuario conectado
                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("couriersAvailable");
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("couriersWorking");

                    //Almacenamos nuestra latitud y longitud
                    GeoFire geoFireAvailable = new GeoFire(refAvailable);
                    GeoFire geoFireWorking = new GeoFire(refWorking);

                    switch (clientId){
                        case "":
                            geoFireWorking.removeLocation(userID);
                            geoFireAvailable.setLocation(userID, new GeoLocation(location.getLatitude(),location.getLongitude()));
                            break;

                        default:
                            geoFireAvailable.removeLocation(userID);
                            geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(),location.getLongitude()));
                            break;

                    }
                }
            }
        }
    };

    private void disconnectCourier () {
        //Obtenemos el id del usuario conectado
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("couriersAvailable");
        //Almacenamos nuestra latitud y longitud
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userID);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isLogginOut){
            disconnectCourier();
        }
    }

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("Location permission")
                        .setMessage("Debe otorgar los permisos de ubicación")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    final int LOCATION_REQUEST_CODE = 1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_REQUEST_CODE:{
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
