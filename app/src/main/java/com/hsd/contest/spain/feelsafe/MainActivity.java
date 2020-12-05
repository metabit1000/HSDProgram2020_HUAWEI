package com.hsd.contest.spain.feelsafe;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.huawei.hmf.tasks.Task;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.banner.BannerView;
import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.HuaweiMobileServicesUtil;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService;

import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Result";

    EditText email;
    EditText password;
    Button signin;
    ImageButton signinHuawei;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println("¿Tengo servicios de Huawei? " + isHwService()); //para saber si se habian instalado correctamente en mi dispositivo Samsung

        email = (EditText)findViewById(R.id.email);
        password = (EditText)findViewById(R.id.password);
        signin =(Button)findViewById(R.id.button);
        signinHuawei =(ImageButton)findViewById(R.id.imageButton);

        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mail = email.getText().toString();
                String pass = password.getText().toString();;

                Pattern pattern = Patterns.EMAIL_ADDRESS;
                if (pattern.matcher(mail).matches()) {
                    if (mail.equals("test@gmail.com") && pass.equals("test") ) { //hardcodeado; para poder entrar en la aplicación sin Huawei ID
                        startActivity(new Intent(MainActivity.this, MapsActivity.class));
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Introduzca correctamente sus credenciales", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "El correo no es correcto", Toast.LENGTH_SHORT).show();
               }
            }
        });

        signinHuawei.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Account Kit
                HuaweiIdAuthParams authParams = new HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM).setIdToken().createParams();
                HuaweiIdAuthService service = HuaweiIdAuthManager.getService(MainActivity.this, authParams);
                startActivityForResult(service.getSignInIntent(), 8888);
            }
        });

        HwAds.init(this);
        AdParam adParam = new AdParam.Builder().build();

        BannerView topBannerView = new BannerView(this);
        topBannerView.setAdId("testw6vs28auh3");
        topBannerView.setBannerAdSize(BannerAdSize.BANNER_SIZE_SMART);
        topBannerView.loadAd(adParam);

        RelativeLayout rootView = findViewById(R.id.root_view);
        rootView.addView(topBannerView);

    }

    public String isHwService() {
        int res = HuaweiMobileServicesUtil.isHuaweiMobileServicesAvailable(this);
        boolean ret = false;

        if (res == ConnectionResult.SUCCESS) {
            ret = true;
        }

        return ret ? "sí" : "no";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Process the authorization result to obtain an ID token from AuthHuaweiId.
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 8888) {
            Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data);
            if (((Task) authHuaweiIdTask).isSuccessful()) {
                // The sign-in is successful, and the user's HUAWEI ID information and ID token are obtained.
                AuthHuaweiId huaweiAccount = authHuaweiIdTask.getResult();
                Log.i(TAG, "idToken:" + huaweiAccount.getIdToken());
                startActivity(new Intent(MainActivity.this, MapsActivity.class)); //to mapsActivity
            } else {
                // The sign-in failed. No processing is required. Logs are recorded to facilitate fault locating.
                Log.e(TAG, "sign in failed : " +((ApiException)authHuaweiIdTask.getException()).getStatusCode());
                Toast.makeText(MainActivity.this, "No se ha podido autenticar correctamente, vuelva a intentarlo", Toast.LENGTH_SHORT).show();
            }
        }
    }
}