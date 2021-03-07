package com.pbgn.newsecurebank;



import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.MasterKeys;

import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import java.io.IOException;
import java.security.GeneralSecurityException;



public class MainActivity extends AppCompatActivity {



    // Attributes
    private boolean isConnected;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("PATH", "In MainActivity.");

        // get elements from the interface
        ImageButton enterB = findViewById(R.id.enterB);
        TextView internetWarningTV = findViewById(R.id.internetWarningTV);

        // checking internet connection
        isConnected = isConnectedCheck();
        if(isConnected){
            internetWarningTV.setVisibility(View.GONE);     // hide warning message if connection with internet established
        }

        enterB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // open login page
                Intent startIntent = new Intent(getApplicationContext(), LoginActivity.class);
                Log.d("PATH", "Opening LoginActivity...");
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

}























