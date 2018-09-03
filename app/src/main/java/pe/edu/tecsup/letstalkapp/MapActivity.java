package pe.edu.tecsup.letstalkapp;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = MapActivity.class.getSimpleName();

    private GoogleMap mMap;

    private Map<String, Marker> markers = new ArrayMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                initMap();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    private void initMap(){
        Log.d(TAG, "initMap");

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.e(TAG, "currentUser: " + currentUser);

        // setMyLocationEnabled (Button & Current position)
        mMap.setMyLocationEnabled(true);

        // Custom UiSettings
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);    // Controles de zoom
        uiSettings.setCompassEnabled(true); // Brújula
        uiSettings.setMyLocationButtonEnabled(true);    // Show MyLocationButton


        // Set a marker: http://www.bufa.es/google-maps-latitud-longitud
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.d(TAG, "onChildAdded " + dataSnapshot.getKey());

                User user = dataSnapshot.getValue(User.class);
                Log.d(TAG, "addedUser: " + user);

                if(user == null || user.getLatitude() == null || user.getLongitude() == null) {
                    return;
                }

                LatLng latLng = new LatLng(user.getLatitude(), user.getLongitude());
                Log.d(TAG, "LatLng: " + latLng);

                // Marker: https://developers.google.com/maps/documentation/android-api/marker?hl=es-419
                MarkerOptions markerOptions = new MarkerOptions().position(latLng)
                        .title(user.getDisplayName())
                        .snippet(user.getEmail());
                Marker marker = mMap.addMarker(markerOptions);

                // Change style marker
                if(currentUser.getUid().equals(user.getUid())) {
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

                    // Show InfoWindow
                    marker.showInfoWindow();

                    // Set current position camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 10));
                }

                // Remember marker into a list
                markers.put(user.getUid(), marker);

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.d(TAG, "onChildChanged " + dataSnapshot.getKey());

                User user = dataSnapshot.getValue(User.class);
                Log.d(TAG, "changedUser: " + user);

                if(user == null || user.getLatitude() == null || user.getLongitude() == null) {
                    return;
                }

                LatLng latLng = new LatLng(user.getLatitude(), user.getLongitude());
                Log.d(TAG, "LatLng: " + latLng);

                Marker marker = markers.get(user.getUid());

                if(marker == null){
                    return;
                }

                marker.setPosition(latLng);
                marker.setTitle(user.getDisplayName());
                marker.setSnippet(user.getEmail());

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved " + dataSnapshot.getKey());

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.d(TAG, "onChildMoved " + dataSnapshot.getKey());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "onCancelled " + databaseError.getMessage(), databaseError.toException());
            }
        };

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addChildEventListener(childEventListener);

    }

}
