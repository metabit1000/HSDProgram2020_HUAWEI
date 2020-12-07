package com.hsd.contest.spain.feelsafe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.banner.BannerView;
import com.huawei.hms.kit.awareness.Awareness;
import com.huawei.hms.kit.awareness.barrier.TimeBarrier;
import com.huawei.hms.kit.awareness.capture.AmbientLightResponse;
import com.huawei.hms.kit.awareness.capture.TimeCategoriesResponse;
import com.huawei.hms.kit.awareness.capture.WeatherStatusResponse;
import com.huawei.hms.kit.awareness.status.AmbientLightStatus;
import com.huawei.hms.kit.awareness.status.TimeCategories;
import com.huawei.hms.kit.awareness.status.WeatherStatus;
import com.huawei.hms.kit.awareness.status.weather.Situation;
import com.huawei.hms.kit.awareness.status.weather.WeatherSituation;

public class SettingsActivity extends AppCompatActivity {

    public final int PICK_CONTACT = 2015;

    TextView telfImportante;
    Button cambiar;
    TextView time;
    TextView weather;
    TextView lightIntensity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        telfImportante = (TextView) findViewById(R.id.telf);
        telfImportante.setText(GlobalVariables.getTelf());

        cambiar = (Button) findViewById(R.id.cambiar);
        time = (TextView) findViewById(R.id.timeCategories);
        weather = (TextView) findViewById(R.id.weatherStatus);
        lightIntensity = (TextView) findViewById(R.id.lightIntensity);

        //Añado anuncio abajo
        HwAds.init(this);
        BannerView bottomBannerView = findViewById(R.id.hw_banner_view);
        AdParam adParam = new AdParam.Builder().build();
        bottomBannerView.loadAd(adParam);

        cambiar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //Cambiar telefono importante
                Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(i, PICK_CONTACT);
            }
        });

        //Obtengo info del Awareness Kit
        getLightIntensity();
        getTimeCategories();
        getWeatherStatus();
    }

    protected void onResume() {
        super.onResume();

        //En caso de volver a entrar a la pantalla que los datos se actualicen
        getLightIntensity();
        getTimeCategories();
        getWeatherStatus();
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) { //Acceso a los contactos del telefono para cambiar el telefono de emergencias
        super.onActivityResult(reqCode, resultCode, data);
        if (reqCode == PICK_CONTACT && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
            cursor.moveToFirst();
            int number = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String telf = cursor.getString(number);

            telfImportante.setText(telf);
            GlobalVariables.setTelf(telf);
        }
    }

    private void getLightIntensity() {
        Awareness.getCaptureClient(this).getLightIntensity()
                .addOnSuccessListener(new OnSuccessListener<AmbientLightResponse>() {
                    @Override
                    public void onSuccess(AmbientLightResponse ambientLightResponse) {
                        AmbientLightStatus ambientLightStatus = ambientLightResponse.getAmbientLightStatus();
                        Log.e("SUCCESS:","La intensidad luminosa es " + ambientLightStatus.getLightIntensity());


                        //Ponerla en la pantalla
                        lightIntensity.setText(Float.toString(ambientLightStatus.getLightIntensity()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e("ERROR: ", "Error al conseguir la intensidad luminosa.", e);

                        lightIntensity.setText("Error");
                    }
                });
    }

    private void getTimeCategories() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Awareness.getCaptureClient(this).getTimeCategories()
                .addOnSuccessListener(new OnSuccessListener<TimeCategoriesResponse>() {
                    @Override
                    public void onSuccess(TimeCategoriesResponse timeCategoriesResponse) {
                        TimeCategories timeCategories = timeCategoriesResponse.getTimeCategories();
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int timeCode : timeCategories.getTimeCategories()) {
                            if (timeCode == TimeBarrier.TIME_CATEGORY_MORNING)
                                stringBuilder.append("Buenos días, puede salir con tranquilidad a la calle.");
                            else if (timeCode == TimeBarrier.TIME_CATEGORY_AFTERNOON)
                                stringBuilder.append("Buenas tardes, puede salir con tranquilidad a la calle.");
                            else if (timeCode == TimeBarrier.TIME_CATEGORY_EVENING)
                                stringBuilder.append("Buenas tardes, va a empezar a anochecer.");
                            else if (timeCode == TimeBarrier.TIME_CATEGORY_NIGHT)
                                stringBuilder.append("Buenas noches, tenga cuidado si debe salir a la calle.");
                            else
                                stringBuilder.append(""); //Los otros códigos no interesan para mi App
                        }
                        time.setText(stringBuilder.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        time.setText("Error");
                    }
                });
    }

    private void getWeatherStatus() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Awareness.getCaptureClient(this).getWeatherByDevice()
                .addOnSuccessListener(new OnSuccessListener<WeatherStatusResponse>() {
                    @Override
                    public void onSuccess(WeatherStatusResponse weatherStatusResponse) {
                        WeatherStatus weatherStatus = weatherStatusResponse.getWeatherStatus();
                        WeatherSituation weatherSituation = weatherStatus.getWeatherSituation();
                        Situation situation = weatherSituation.getSituation();
                        String weatherInfoStr =
                                "Ciudad:" + weatherSituation.getCity().getName() + "\n" +
                                "El tiempo hoy: " + getWeather(situation.getWeatherId()) + "\n" +
                                "La temperatura es: " + situation.getTemperatureC() + "℃" + "\n" +
                                "La velocidad del viento es: " + situation.getWindSpeed() + "km/h" + "\n" +
                                "La humedad es de: " + situation.getHumidity() + "%";

                        weather.setText(weatherInfoStr);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        weather.setText("Error");
                    }
                });
    }

    private String getWeather(int id) { //algunos posibles valores que retorna el WeatherId
        String result = "No hay info disponible";
        switch (id) {
            case 1:
                result = "Hace sol";
                break;
            case 2:
                result = "Hace mayormente sol";
                break;
            case 3:
                result = "Hace poco sol";
                break;
            case 4:
                result = "Algunas nubes a la vista";
                break;
            case 5:
                result = "Hace algo de sol, pero con nubes";
                break;
            case 6:
                result = "Mayormente nublado";
                break;
            case 7:
                result  = "Está nublado";
                break;
            case 18:
                result = "Está lloviendo";
                break;
            default:
                break;
        }
        return result;
    }

}