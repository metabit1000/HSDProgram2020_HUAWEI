package com.hsd.contest.spain.feelsafe;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.kit.awareness.Awareness;
import com.huawei.hms.kit.awareness.capture.CapabilityResponse;
import com.huawei.hms.kit.awareness.capture.LocationResponse;
import com.huawei.hms.kit.awareness.status.CapabilityStatus;
import com.huawei.hms.maps.CameraUpdate;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.MapView;
import com.huawei.hms.maps.MapsInitializer;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.model.BitmapDescriptor;
import com.huawei.hms.maps.model.BitmapDescriptorFactory;
import com.huawei.hms.maps.model.CameraPosition;
import com.huawei.hms.maps.model.Circle;
import com.huawei.hms.maps.model.CircleOptions;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.Marker;
import com.huawei.hms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "Resultado: ";

    private static final String[] RUNTIME_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET};

    private static final int PERMISSION_REQUEST_CODE = 940;
    private final String[] mPermissionsOnHigherVersion = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION};
    private final String[] mPermissionsOnLowerVersion = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
            "com.huawei.hms.permission.ACTIVITY_RECOGNITION"};

    private static final int REQUEST_CODE = 100;

    private HuaweiMap hMap;
    private MapView mMapView;

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    ImageButton sos;
    ImageButton settings;
    ImageButton search;

    private Location location; //mi ubicacion
    private Marker marcador;
    private Marker marcadorBusqueda;

    double lat = 0.0;
    double lng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (!hasPermissions(this, RUNTIME_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE);
        }

        checkAndRequestPermissions(); //to awareness kit

        sos = (ImageButton) findViewById(R.id.SOS);
        settings = (ImageButton) findViewById(R.id.settings);
        search = (ImageButton) findViewById(R.id.search);

        sos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", GlobalVariables.getTelf(), null)));
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MapsActivity.this, SettingsActivity.class));
            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MapsActivity.this, SearchActivity.class));
            }
        });

        mMapView = findViewById(R.id.mapView);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        MapsInitializer.setApiKey("CgB6e3x9MPbI8iITjvfTfjI82nRwFI0Y7vFnXcFUeM8TdYtCWhs6L6JP+417gXvM4kZaC2pEr5lUp5uKUU/SxZxo");
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this); //get map instance
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onMapReady(HuaweiMap map) {
        //get map instance in a callback method
        Log.d(TAG, "onMapReady: ");
        hMap = map;

        //get my location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        /*Awareness.getCaptureClient(this).getLocation() //no acaba de funcionarme este kit...error 1212
                .addOnSuccessListener(new OnSuccessListener<LocationResponse>() {
                    @Override
                    public void onSuccess(LocationResponse locationResponse) {
                        Location location = locationResponse.getLocation();
                        Log.e(TAG,"My location is; Longitude:" + location.getLongitude()
                                + ",Latitude:" + location.getLatitude());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "get location failed", e);
                    }
                });*/

        //LatLng ubicacion = new LatLng(location.getLongitude(), location.getLatitude());

        addDangerousPlaces();
        miUbicacion(); //actualiza la ubicación del telefono automaticamente
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsDoNotGrant = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (String permission : mPermissionsOnHigherVersion) {
                if (ActivityCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsDoNotGrant.add(permission);
                }
            }
        } else {
            for (String permission : mPermissionsOnLowerVersion) {
                if (ActivityCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsDoNotGrant.add(permission);
                }
            }
        }

        if (permissionsDoNotGrant.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    permissionsDoNotGrant.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private void addDangerousPlaces() {
        double radio = 700.0;
        ArrayList<LatLng > locations = new ArrayList<LatLng >();

        //Localizacion de las zonas peligrosas de la ciudad de Barcelona hardcodeadas (lo suyo sería bajarlas de una API)
        locations.add(new LatLng (41.3798432, 2.1682149000000663));
        locations.add(new LatLng(41.3746936,2.172931));
        locations.add(new LatLng(41.3604232,2.1317501));
        locations.add(new LatLng(41.3692989,2.1319386));
        locations.add(new LatLng(41.4147788,2.2085157));
        locations.add(new LatLng(41.416697,2.1941002));
        locations.add(new LatLng(41.4236105,2.1989811));
        locations.add(new LatLng(41.4426765,2.176823));
        locations.add(new LatLng(41.4614629,2.1700352));
        locations.add(new LatLng(41.3865438,2.1788116));
        locations.add(new LatLng(41.426887, 1.788163));
        locations.add(new LatLng(41.440676, 1.866005));

        for (int i = 0; i < locations.size(); ++i) {
            hMap.addCircle(new CircleOptions().center(locations.get(i)).radius(radio).radius(500)
                    .fillColor(Color.argb(70, 150, 50, 50))
                    .strokeColor(Color.TRANSPARENT));  //esto con Polygons sería mucho mejor, pero era bastante más trabajo buscar y poner los puntos de donde acaban los barrios "peligrosos"
        }
    }


    private void agregarMarcador(double lat, double lng) {
        LatLng coordenades = new LatLng(lat, lng);
        CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenades, 14);
        if (marcador != null) marcador.remove();
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.marcador);
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, 80, 100, false);
        BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);
        marcador = hMap.addMarker(new MarkerOptions().position(coordenades).title("My location").icon(smallMarkerIcon));

        if (GlobalVariables.getSelectedPosition() != -1) { //agregar marcador para la location escogida en la busqueda
            LatLng coor = GlobalVariables.places.get(GlobalVariables.getSelectedPosition()).location;
            if (marcadorBusqueda == null) {
                marcadorBusqueda = hMap.addMarker(new MarkerOptions().position(coor).title("SearchedLocation").icon(smallMarkerIcon));
            }
            else marcadorBusqueda.remove();

            GlobalVariables.setSelectedPosition(-1);
        }

        hMap.animateCamera(miUbicacion);
    }

    private void actualizarUbicacion(Location location) {
        LatLng ubi = new LatLng(0,0);
        if (location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();
            System.out.println(lat+" ,"+lng);
            agregarMarcador(lat,lng);
            ubi = new LatLng(lat,lng);
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            actualizarUbicacion(location);
            //vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            //mySong = MediaPlayer.create(MapsActivity.this, R.raw.song);

            //alg.core(mMap,location,vibrator, mySong);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void miUbicacion() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        System.out.println("UBI COORD: " + location.getLongitude() + " :" + location.getLatitude());

        actualizarUbicacion(location);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,0,locationListener);
    }

}
