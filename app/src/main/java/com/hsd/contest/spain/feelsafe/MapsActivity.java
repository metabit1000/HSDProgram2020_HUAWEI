package com.hsd.contest.spain.feelsafe;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.kit.awareness.Awareness;
import com.huawei.hms.kit.awareness.barrier.AwarenessBarrier;
import com.huawei.hms.kit.awareness.barrier.BarrierUpdateRequest;
import com.huawei.hms.maps.CameraUpdate;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.MapView;
import com.huawei.hms.maps.MapsInitializer;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.model.BitmapDescriptor;
import com.huawei.hms.maps.model.BitmapDescriptorFactory;
import com.huawei.hms.maps.model.CircleOptions;
import com.huawei.hms.maps.model.LatLng;

import com.huawei.hms.maps.model.Marker;
import com.huawei.hms.maps.model.MarkerOptions;
import com.huawei.hms.maps.model.Polyline;
import com.huawei.hms.maps.model.PolylineOptions;

import java.util.ArrayList;

import java.util.List;

import com.huawei.hms.kit.awareness.barrier.BarrierStatus;
import com.huawei.hms.kit.awareness.barrier.LocationBarrier;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

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
    private Marker marcador; //origin marker
    private Marker marcadorBusqueda; //destination marker
    private Polyline route;

    double lat = 0.0;
    double lng = 0.0;

    private PendingIntent mPendingIntent;
    private LocationBarrierReceiver mBarrierReceiver;
    private static final String STAY_BARRIER_LABEL = "stay barrier label";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (!hasPermissions(this, RUNTIME_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE);
        }

        checkAndRequestPermissions(); //Awareness Kit

        sos = (ImageButton) findViewById(R.id.SOS);
        settings = (ImageButton) findViewById(R.id.settings);
        search = (ImageButton) findViewById(R.id.search);

        final String barrierReceiverAction = getApplication().getPackageName() + "LOCATION_BARRIER_RECEIVER_ACTION";
        Intent intent = new Intent(barrierReceiverAction);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBarrierReceiver = new LocationBarrierReceiver();
        registerReceiver(mBarrierReceiver, new IntentFilter(barrierReceiverAction));

        sos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //abre la aplicacion para hacer llamada con el telefono en caso de emergencia introducido
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

        //Configuración del mapa
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
        hMap = map;

        hMap.getUiSettings().setZoomGesturesEnabled(true);
        hMap.getUiSettings().setRotateGesturesEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        addDangerousPlaces(); //añado las zonas/barrios peligrosos de la ciudad de Barcelona
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

    private void checkAndRequestPermissions() { //chequear permisos para el Awareness Kit
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
        ArrayList<LatLng> locations = new ArrayList<LatLng>();

        //Localizacion de las zonas peligrosas de la ciudad de Barcelona hardcodeadas (lo correcto/ideal sería obtenerlas de una API)
        locations.add(new LatLng(41.3798432, 2.1682149000000663));
        locations.add(new LatLng(41.3746936, 2.172931));
        locations.add(new LatLng(41.3604232, 2.1317501));
        locations.add(new LatLng(41.3692989, 2.1319386));
        locations.add(new LatLng(41.4147788, 2.2085157));
        locations.add(new LatLng(41.416697, 2.1941002));
        locations.add(new LatLng(41.4236105, 2.1989811));
        locations.add(new LatLng(41.4426765, 2.176823));
        locations.add(new LatLng(41.4614629, 2.1700352));
        locations.add(new LatLng(41.3865438, 2.1788116));
        locations.add(new LatLng(41.426887, 1.788163));
        locations.add(new LatLng(41.440676, 1.866005));
        //locations.add(new LatLng(yourlat, yourlong)); //testing

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        for (int i = 0; i < locations.size(); ++i) {
            hMap.addCircle(new CircleOptions().center(locations.get(i)).radius(radio).radius(500)
                    .fillColor(Color.argb(70, 150, 50, 50))
                    .strokeColor(Color.TRANSPARENT));  //esto con Polygons sería mucho mejor, pero era bastante más trabajo buscar y poner los puntos de donde acaban los barrios "peligrosos"

            AwarenessBarrier stayBarrier = LocationBarrier.stay(locations.get(i).latitude, locations.get(i).longitude, radio, 1000L);
            addBarrier(this, STAY_BARRIER_LABEL, stayBarrier, mPendingIntent); //añado esa zona peligrosa a una locationBarrier del Awareness Kit
        }
    }

    private void agregarMarcador(double lat, double lng) {
        LatLng coordenades = new LatLng(lat, lng);
        CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenades, 14); //pongo la camara del mapa en la posicion donde esta mi ubicacion y el marcador

        if (marcador != null) marcador.remove(); //elimino el marcador por si mi ubicacion cambia
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.marcador);
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, 80, 100, false);
        BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);
        marcador = hMap.addMarker(new MarkerOptions().position(coordenades).title("Mi ubicacion").icon(smallMarkerIcon)); //creo marcador con mi ubicación

        if (GlobalVariables.getSelectedPosition() != -1) { //agregar marcador para la location escogida en la busqueda de SearchActivity
            LatLng coor = GlobalVariables.places.get(GlobalVariables.getSelectedPosition()).location;
            if (marcadorBusqueda == null) {
                marcadorBusqueda = hMap.addMarker(new MarkerOptions().position(coor).title(GlobalVariables.places.get(GlobalVariables.getSelectedPosition()).name).icon(smallMarkerIcon));
            }
            else marcadorBusqueda.remove();

            saferRouteCreation(lat, lng); //creacion de la ruta "segura" (es solo una línea recta por el momento)

            GlobalVariables.setSelectedPosition(-1); //para que no vuelva a entrar hasta que se seleccione un site nuevo
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

        //Manualmente introducir la location para testear
        //location.setLatitude(41.3798432);
        //location.setLongitude(2.1682149000000663);

        actualizarUbicacion(location);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,0,locationListener);
    }

    private void saferRouteCreation(double lat, double lng) {
        LatLng originCoord = new LatLng(lat, lng);
        LatLng destinationCoord = GlobalVariables.places.get(GlobalVariables.getSelectedPosition()).location;

        route = hMap.addPolyline(new PolylineOptions().add(originCoord, destinationCoord).color(Color.RED).width(2));
    }

    final class LocationBarrierReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            BarrierStatus barrierStatus = BarrierStatus.extract(intent);
            String label = barrierStatus.getBarrierLabel();
            int barrierPresentStatus = barrierStatus.getPresentStatus();
            switch (label) {
                case STAY_BARRIER_LABEL:
                    if (barrierPresentStatus == BarrierStatus.TRUE) {
                        Toast.makeText(MapsActivity.this, "¡Estas en zona peligrosa!", Toast.LENGTH_SHORT).show();

                        //Suena sonido y vibra para avisar
                        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        MediaPlayer mySong = MediaPlayer.create(MapsActivity.this, R.raw.warningsound);

                        mySong.start(); //suena sonido
                        vibrator.vibrate(1000); //vibra
                    } else if (barrierPresentStatus == BarrierStatus.FALSE) {
                        Toast.makeText(MapsActivity.this, "¡NO Estas en zona peligrosa!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MapsActivity.this, "Tu posicion es desconocida", Toast.LENGTH_SHORT).show();
                    }
                    break;
                 //se podría hacer en caso de entrar en la zona peligrosa también
            }
        }
    }

    public static void addBarrier(Context context, final String label, AwarenessBarrier barrier, PendingIntent pendingIntent) {
        BarrierUpdateRequest.Builder builder = new BarrierUpdateRequest.Builder();
        BarrierUpdateRequest request = builder.addBarrier(label, barrier, pendingIntent).build();
        Awareness.getBarrierClient(context).updateBarriers(request)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("SUCCESS", "Success al crear Barrier");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e("ERROR", "Error al crear Barrier");
                    }
                });
    }

}
