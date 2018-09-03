package pe.edu.tecsup.letstalkapp;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class UserRVAdapter extends RecyclerView.Adapter<UserRVAdapter.ViewHolder> {

    private static final String TAG = UserRVAdapter.class.getSimpleName();

    private List<User> users;

    public List<User> getUsers() {
        return users;
    }

    private Location myLocation;

    public void setMyLocation(Location myLocation) {
        this.myLocation = myLocation;
    }

    public UserRVAdapter(){
        this.users = new ArrayList<>();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView userImage;
        TextView displaynameText;
        TextView emailText;
        TextView distanceText;

        ViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_picture);
            displaynameText = itemView.findViewById(R.id.user_displayname);
            emailText = itemView.findViewById(R.id.user_email);
            distanceText = itemView.findViewById(R.id.user_distance);
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        Context context = holder.itemView.getContext();

        final User user = users.get(position);
        Log.d(TAG, "user: " + user);


        holder.displaynameText.setText(user.getDisplayName());
        holder.emailText.setText(user.getEmail());

        // Distance calculate
        if (myLocation != null) {

            LatLng latLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
            Log.d(TAG, "getLastKnownLocation LatLng: " + latLng);

            if (user.getLatitude() != null && user.getLongitude() != null) {

                Location userLocation = new Location("dummyprovider");
                userLocation.setLatitude(user.getLatitude());
                userLocation.setLongitude(user.getLongitude());

                LatLng userLatLng = new LatLng(user.getLatitude(), user.getLongitude());
                Log.d(TAG, "userLatLng LatLng: " + userLatLng);

                float distancia = userLocation.distanceTo(myLocation);

                DecimalFormat decimalFormat = new DecimalFormat("#.## kms");
                holder.distanceText.setText(decimalFormat.format(distancia / 1000));

            }

        }

    }

    @Override
    public int getItemCount() {
        return users.size();
    }


}
