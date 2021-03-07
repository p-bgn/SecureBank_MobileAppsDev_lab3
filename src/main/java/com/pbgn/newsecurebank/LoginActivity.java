package com.pbgn.newsecurebank;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.biometrics.BiometricManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;



public class LoginActivity extends AppCompatActivity {



    // Attributes

    private RequestQueue mQueue;        // Volley request queue

    private EditText firstNamePT;       // interface elements
    private EditText lastNamePT;
    private EditText idPT;
    private String loginFirstName;
    private String loginLastName;
    private String loginID;

    private boolean isConnected;        // internet connection boolean

    private String masterKey;           // key used for encryption

    SharedPreferences spLogins;         // shared preferences



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d("PATH", "In LoginActivity.");

        // get elements from the interface
        firstNamePT = findViewById(R.id.firstNamePT);
        lastNamePT = findViewById(R.id.lastNamePT);
        idPT = findViewById(R.id.idPT);
        ImageButton fingerPrintB = findViewById(R.id.fingerPrintB);
        ImageButton exitLoginB = findViewById(R.id.exitLoginB);
        TextView internetWarningTV2 = findViewById(R.id.internetWarningTV2);

        // checking internet connection
        isConnected = isConnectedCheck();
        if(isConnected){
            internetWarningTV2.setVisibility(View.GONE);     // hide warning message if connection with internet established
        }

        // get MasterKey from the AndroidKeyStore
        try {
            masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        // Encrypted Shared Preferences
        try {
            spLogins = EncryptedSharedPreferences.create(
                    "SecureLastLogin",
                    masterKey,
                    getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        // create new request secured with TLS
        try {
            mQueue = Volley.newRequestQueue(this,  new HurlStack(null, new TLSSocketFactory(masterKey)));
            Log.d("API", "Request Queue created.");
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // create Biometric Manager & check if it is useable
        androidx.biometric.BiometricManager biometricManager = androidx.biometric.BiometricManager.from(this);
        switch(biometricManager.canAuthenticate()){
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d("BIOMETRIC", "Biometric Sensor OK.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(getApplicationContext(), "This device doesn't have any FingerPrint sensor." +
                        "\nThis app only uses FingerPrint to Login." +
                        "\nPlease try with another device.", Toast.LENGTH_LONG).show();
                Log.d("BIOMETRIC", "Biometric Sensor not OK (No Sensor).");
                fingerPrintB.setVisibility(View.GONE);      // hide biometric button
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(getApplicationContext(), "The FingerPrint sensor is currently unavailable." +
                        "\nThis app only uses FingerPrint to Login." +
                        "\nPlease try later.", Toast.LENGTH_LONG).show();
                Log.d("BIOMETRIC", "Biometric Sensor not OK (Sensor Unavailable).");
                fingerPrintB.setVisibility(View.GONE);      // hide biometric button
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(getApplicationContext(), "You don't have any FingerPrint saved on you device." +
                        "\nThis app only uses FingerPrint to Login." +
                        "\nPlease save a FingerPrint on your device.", Toast.LENGTH_LONG).show();
                Log.d("BIOMETRIC", "Biometric Sensor not OK (No FingerPrint).");
                fingerPrintB.setVisibility(View.GONE);      // hide biometric button
                break;
        }

        // create executor
        Executor executor = ContextCompat.getMainExecutor(this);

        // create callback : is the finger print correct?
        BiometricPrompt biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Error during FingerPrint test.\nPlease try again...", Toast.LENGTH_SHORT).show();
                Log.d("BIOMETRIC", "Biometric Test not OK (Error).");
            }
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                Log.d("BIOMETRIC", "Biometric Test OK.");

                // if the phone is connected to internet : get data from the mockapi and compare
                if(isConnected==true){
                    getConfigsData();
                    // clear edit texts
                    firstNamePT.setText("");
                    lastNamePT.setText("");
                    idPT.setText("");
                }
                // if he isn't : compare with shared preferences (=last login data)
                else{
                    // is the user found in the response
                    boolean found = false;
                    // get data from the encrypted shared preferences
                    String lastLoginFirstName = spLogins.getString("lastLoginFirstName", "");
                    String lastLoginLastName = spLogins.getString("lastLoginLastName", "");
                    String lastLoginID = spLogins.getString("lastLoginID", "");
                    // compare with entered login
                    if(loginFirstName.equals(lastLoginFirstName) && loginLastName.equals(lastLoginLastName) && loginID.equals(lastLoginID)){
                        // update boolean
                        found = true;
                        // open accounts activity
                        Intent accountsIntent = new Intent(getApplicationContext(), AccountsActivity.class);
                        Log.d("PATH", "Opening AccountsActivity...");
                        startActivity(accountsIntent);
                    }
                    // if user not found : log and error message to user
                    if(found==false){
                        Toast.makeText(getApplicationContext(), "Authentification Error.\nPlease enter last login parameters.", Toast.LENGTH_SHORT).show();
                        Log.d("API", "Config Response : User not found.");
                    }
                    // clear edit texts
                    firstNamePT.setText("");
                    lastNamePT.setText("");
                    idPT.setText("");
                }
            }
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "FingerPrint not recognized.\nPlease try again...", Toast.LENGTH_SHORT).show();
                Log.d("BIOMETRIC", "Biometric Test not OK (Wrong FP).");
            }
        });

        // create Biometric Dialog Box
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("FingerPrint Test")
                .setDescription("Use fingerprint to Login")
                .setNegativeButtonText("Cancel")
                .build();

        // login button
        fingerPrintB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get login data
                loginFirstName = firstNamePT.getText().toString();
                loginLastName = lastNamePT.getText().toString();
                loginID = idPT.getText().toString();

                // launch finger print check
                biometricPrompt.authenticate(promptInfo);
            }
        });

        // exit button
        exitLoginB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // go back to main activity
                Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
                Log.d("PATH", "Back to MainActivity...");
                startActivity(startIntent);
            }
        });
    }



    // checks for internet connection (wifi, 3/4G)
    private boolean isConnectedCheck(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        // true if connected (by wifi or by 3/4G)
        if( (wifiInfo!=null && wifiInfo.isConnected()) || (mobileInfo!=null && mobileInfo.isConnected()) ){
            Log.d("CONNECTION","Device connected to Internet.");
            return true;
        }
        // false if not
        else{
            Log.d("CONNECTION","Device not connected to Internet.");
            return false;
        }
    }



    // get data from the url with Volley library
    private void getConfigsData(){

        // url with data
        String url = "https://60102f166c21e10017050128.mockapi.io/labbbank/config";

        // create Json Request
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,

                new Response.Listener<JSONArray>()
                {
                    @Override
                    public void onResponse(JSONArray response) {

                        Log.d("API", "Config Response received.");

                        // is the user found in the response
                        boolean found = false;

                        try {

                            for (int i=0; i<response.length(); i++) {

                                // create json object to store each element
                                JSONObject obj = response.getJSONObject(i);

                                // get data from this object
                                String tempID = obj.getString("id");
                                String tempFirstName = obj.getString("name");
                                String tempLastName = obj.getString("lastname");

                                // compare with entered login data
                                if (loginFirstName.equals(tempFirstName) && loginLastName.equals(tempLastName) && loginID.equals(tempID)){

                                    found = true;

                                    Log.d("API", "Config Response : User found.");

                                    // update shared preferences
                                    SharedPreferences.Editor editor = spLogins.edit();
                                    editor.clear();
                                    editor.putString("lastLoginFirstName", loginFirstName);
                                    editor.putString("lastLoginLastName", loginLastName);
                                    editor.putString("lastLoginID", loginID);
                                    editor.commit();
                                    Log.d("SP", "Config SP Updated.");

                                    // open accounts activity
                                    Intent accountsIntent = new Intent(getApplicationContext(), AccountsActivity.class);
                                    Log.d("PATH", "Opening AccountsActivity...");
                                    startActivity(accountsIntent);
                                }
                            }

                            // if user not found : log and error message to user
                            if(found==false){
                                Toast.makeText(getApplicationContext(), "Authentification Error.\nPlease try again.", Toast.LENGTH_SHORT).show();
                                Log.d("API", "Config Response : User not found.");
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("API", "Error : " + error.toString());
                    }
                }
        );
        Log.d("API", "Config Request sent.");
        mQueue.add(request);
    }


}