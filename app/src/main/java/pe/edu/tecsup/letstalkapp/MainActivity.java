package pe.edu.tecsup.letstalkapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private FusedLocationProviderClient fusedLocationProviderClient;

    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /**
         * Recuperamos el usuario de FirebaseAuth
         */
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.e(TAG, "currentUser: " + currentUser);

        /**
         * Guardamos el objeto user
         */
        final User user = new User();
        user.setUid(currentUser.getUid());
        user.setDisplayName(currentUser.getDisplayName());
        user.setEmail(currentUser.getEmail());
        user.setPhotoUrl((currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null));
        Log.e(TAG, "user: " + user);

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.child(user.getUid()).setValue(user);


        /**
         * Get users list from Firebase
         */
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final UserRVAdapter userRVAdapter = new UserRVAdapter();
        recyclerView.setAdapter(userRVAdapter);

        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.d(TAG, "onChildAdded " + dataSnapshot.getKey());

                User user = dataSnapshot.getValue(User.class);
                Log.d(TAG, "addedUser: " + user);

                List<User> users = userRVAdapter.getUsers();
                users.add(0, user);
                userRVAdapter.notifyDataSetChanged();

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.d(TAG, "onChildChanged " + dataSnapshot.getKey());

                User changedUser = dataSnapshot.getValue(User.class);
                Log.d(TAG, "changedUser: " + changedUser);

                // Actualizando adapter datasource
                List<User> users = userRVAdapter.getUsers();
                int index = users.indexOf(changedUser); // Necesario implementar User.equals()
                if(index != -1){
                    users.set(index, changedUser);
                }
                userRVAdapter.notifyDataSetChanged();

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

        DatabaseReference usersRef2 = FirebaseDatabase.getInstance().getReference("users");
        usersRef2.addChildEventListener(childEventListener);


        /**
         * Periodical request LocationUpdates
         */
        // Verify permissions
        if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        // https://developer.android.com/training/location/receive-location-updates.html
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                if(locationResult.getLastLocation() == null){
                    return;
                }

                Log.d(TAG, "onLocationResult by " + locationResult.getLastLocation().getProvider());

                LatLng latLng = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                Log.d(TAG, "LatLng: " + latLng);

                user.setLatitude(locationResult.getLastLocation().getLatitude());
                user.setLongitude(locationResult.getLastLocation().getLongitude());

                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                usersRef.child(user.getUid()).setValue(user);

                // Set current position into adapter
                userRVAdapter.setMyLocation(locationResult.getLastLocation());
                userRVAdapter.notifyDataSetChanged();
            }
        };

        LocationRequest locationRequest = new LocationRequest()
                .setInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);


        // Change displayname
        if(currentUser.getDisplayName() == null || currentUser.getDisplayName().length() == 0){

            final EditText edittext = new EditText(this);

            new AlertDialog.Builder(this)
                    .setTitle("Danos tu nombre")
                    .setView(edittext)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            String displayname = edittext.getText().toString();

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayname).build();
                            currentUser.updateProfile(profileUpdates);

                            user.setDisplayName(displayname);
                            Log.e(TAG, "user: " + user);

                            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                            usersRef.child(user.getUid()).child("displayName").setValue(displayname);

                        }
                    }).show();

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                callLogout(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void callLogout(View view){
        Log.d(TAG, "Sign out user");
        FirebaseAuth.getInstance().signOut();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        finish();
    }

    public void showMap(View view){
        startActivity(new Intent(this, MapActivity.class));
    }

}
