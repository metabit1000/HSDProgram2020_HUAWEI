package com.hsd.contest.spain.feelsafe;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.banner.BannerView;
import com.huawei.hms.kit.awareness.barrier.TimeBarrier;

public class SettingsActivity extends AppCompatActivity {

    private ScrollView mScrollView;
    public final int PICK_CONTACT = 2015;

    public static final SparseArray<String> TIME_DESCRIPTION_MAP = new SparseArray<>();

    static {
        TIME_DESCRIPTION_MAP.put(TimeBarrier.TIME_CATEGORY_WEEKDAY, "Today is weekday.");
        TIME_DESCRIPTION_MAP.put(TimeBarrier.TIME_CATEGORY_WEEKEND, "Today is weekend.");
        TIME_DESCRIPTION_MAP.put(TimeBarrier.TIME_CATEGORY_HOLIDAY, "Today is holiday.");
        TIME_DESCRIPTION_MAP.put(TimeBarrier.TIME_CATEGORY_NOT_HOLIDAY, "Today is not holiday.");
        TIME_DESCRIPTION_MAP.put(TimeBarrier.TIME_CATEGORY_MORNING, "Buenos días");
        TIME_DESCRIPTION_MAP.put(TimeBarrier.TIME_CATEGORY_AFTERNOON, "Buenas tardes");
        TIME_DESCRIPTION_MAP.put(TimeBarrier.TIME_CATEGORY_EVENING, "Empieza a anochecer");
        TIME_DESCRIPTION_MAP.put(TimeBarrier.TIME_CATEGORY_NIGHT, "Vaya con cuidado, se ha hecho de noche");
    }

    TextView telfImportante;
    Button cambiar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        telfImportante = (TextView)findViewById(R.id.telf);
        telfImportante.setText(GlobalVariables.telf);

        cambiar = (Button)findViewById(R.id.cambiar);

        HwAds.init(this);

        BannerView bottomBannerView = findViewById(R.id.hw_banner_view);
        AdParam adParam = new AdParam.Builder().build();
        bottomBannerView.loadAd(adParam);

        //getTime();

        cambiar.setOnClickListener(new View.OnClickListener() { //parte de acceder a los contactos
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(i, PICK_CONTACT);
            }
        });
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        if (reqCode == PICK_CONTACT && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
            cursor.moveToFirst();
            int number = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String telf = cursor.getString(number);

            telfImportante.setText(telf);
            GlobalVariables.telf = telf;
        }
    }

}