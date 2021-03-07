package com.pbgn.newsecurebank;



import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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



public class AccountsActivity extends AppCompatActivity {



    // Attributes
    private TextView accountsTV;        // interface elements

    private RequestQueue mQueue;        // Volley request queue

    private boolean isConnected;        // internet connection boolean

    private String masterKey;           // key used for encryption

    SharedPreferences spAccounts;       // shared preferences



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);

        Log.d("PATH", "In AccountsActivity.");

        // get elements from the interface
        accountsTV = findViewById(R.id.accountsTV);
        ImageButton refreshB = findViewById(R.id.refreshB);
        ImageButton exitAccountsB = findViewById(R.id.exitAccountsB);
        TextView internetWarningTV3 = findViewById(R.id.internetWarningTV3);

        // checking internet connection
        isConnected = isConnectedCheck();
        if(isConnected){
            internetWarningTV3.setVisibility(View.GONE);     // hide warning message if connection with internet established
        }
        else{
            refreshB.setVisibility(View.GONE);              // hide refresh button
        }

        // get MasterKey from AndroidKeyStore
        try {
            masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        // Encrypted Shared Preferences
        try {
            spAccounts = EncryptedSharedPreferences.create(
                    "SecureLastAccounts",
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

        // if the phone is connected to internet : get data from the mockapi and compare
        if(isConnected){
            getAccountsData();
        }
        // else : get data from shared preferences (=last login data)
        else{
            String lastAccountsInstance = spAccounts.getString("lastAccountsInstance", "");
            accountsTV.append(lastAccountsInstance);
        }

        // refresh button
        refreshB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // clear the old text view
                accountsTV.setText("");
                // fill it back with new data from the API
                getAccountsData();

                Toast.makeText(getApplicationContext(), "Accounts updated..", Toast.LENGTH_SHORT).show();
            }
        });

        // exit button
        exitAccountsB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(getApplicationContext(), LoginActivity.class);
                Log.d("PATH", "Back to LoginActivity...");
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
    private void getAccountsData(){

        // url with data
        String url = "https://60102f166c21e10017050128.mockapi.io/labbbank/accounts";

        // create JSON Request
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,

                new Response.Listener<JSONArray>()
                {
                    @Override
                    public void onResponse(JSONArray response) {

                        Log.d("API", "Accounts Response received.");

                        try {
                            for (int i=0; i<response.length(); i++){

                                // create json object to store each element
                                JSONObject account = response.getJSONObject(i);

                                // get data from this object
                                String id = account.getString("id");
                                String accountName = account.getString("accountName");
                                String amount = account.getString("amount");
                                String iban = account.getString("iban");
                                String currency = account.getString("currency");

                                // add Strings to the text view
                                accountsTV.append("ID : " + id + "\nACCOUNT NAME : " + accountName + "\nAMOUNT : " + amount + "\nIBAN : " + iban + "CURRENCY : " + currency + "\n\n\n");

                                // update shared preferences
                                SharedPreferences.Editor editor = spAccounts.edit();
                                editor.clear();
                                editor.putString("lastAccountsInstance", accountsTV.getText().toString());
                                editor.commit();
                                Log.d("SP", "Accounts SP Updated.");
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
        Log.d("API", "Accounts Request sent.");
        mQueue.add(request);
    }

}