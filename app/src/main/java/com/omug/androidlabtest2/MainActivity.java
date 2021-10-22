package com.omug.androidlabtest2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,  GoogleMap.OnMarkerClickListener{
    GoogleMap mMap;
    private Toolbar mToolbar;
    private LocationDatabase locationDatabase;
    private List<Location> locations;
    private int pos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeVies();
        displayLocationList();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private void displayLocationList() {
        locationDatabase = LocationDatabase.getDatabase(MainActivity.this);
        new RetrieveTask(this).execute();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        addMarkersToMap();
        mMap.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener) this);
    }

    private void addMarkersToMap() {
        for (Location location : locations) {
            mMap.addMarker(new MarkerOptions()
                    .position(location.getLocation())
                    .title(location.getTitle())
                    .snippet(location.getSubtitle())
            );
        }
    }

    public void onResetMap() {
        // Clear the map because we don't want duplicates of the markers.
        mMap.clear();
        addMarkersToMap();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        for (Location location : locations) {

            Marker currentMark = mMap.addMarker(new MarkerOptions()
                    .position(location.getLocation())
                    .title(location.getTitle())
                    .snippet(location.getSubtitle())
            );
            Log.w("Click", "is "+marker.getTitle().equalsIgnoreCase(currentMark.getTitle()));
            if(marker.getTitle().equalsIgnoreCase(currentMark.getTitle())){

                startActivityForResult(
                        new Intent(MainActivity.this, AddLocationActivity.class).putExtra("location", location), 100);
                return true;
            }
        }

        return true;
    }

    //este metodo carga la informacion de las localizaciones en la lista
    private static class RetrieveTask extends AsyncTask<Void, Void, List<Location>> {
        private WeakReference<MainActivity> activityReference;
        // only retain a weak reference to the activity
        RetrieveTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected List<Location> doInBackground(Void... voids) {
            if (activityReference.get() != null)
                return activityReference.get().locationDatabase.personDao().getAll();
            else
                return null;
        }

        @Override
        protected void onPostExecute(List<Location> locations) {
            if (locations != null && locations.size() > 0) {
                activityReference.get().locations.clear();
                activityReference.get().locations.addAll(locations);
                activityReference.get().onResetMap();

            }
        }
    }

    private void initializeVies() {
        mToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setOnMenuItemClickListener(listener);
        locations = new ArrayList<>();
    }

    private Toolbar.OnMenuItemClickListener listener = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(@NonNull MenuItem item) {
            switch (item.getItemId()){
                case R.id.menu_add:
                    startActivityForResult(new Intent(MainActivity.this, AddLocationActivity.class), 100);
                    break;
                default:
            }
            return false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode > 0) {
            if (resultCode == 1) {
                locations.add((Location) data.getSerializableExtra("location"));
            } else if (resultCode == 2) {
                locations.set(pos, (Location) data.getSerializableExtra("location"));
            } else if (resultCode == 3) {
                locations.remove(pos);
            }
            displayLocationList();
        }
    }

    @Override
    protected void onDestroy() {
        locationDatabase.cleanUp();
        super.onDestroy();
    }
}