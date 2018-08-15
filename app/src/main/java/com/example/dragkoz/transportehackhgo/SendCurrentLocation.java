package com.example.dragkoz.transportehackhgo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.ksoap2.HeaderProperty;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Un {@link Service} que notifica la cantidad de memoria disponible en el sistema
 */
public class SendCurrentLocation extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = SendCurrentLocation.class.getSimpleName();

    private String response = "";

    TimerTask timerTask;

    private String startHour = "";
    private String finalHour = "";

    private String lat = "", lng = "";
    private int currentBatteryLevel = 0;

    private GoogleApiClient mGoogleApiClient;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 2000;

    public SendCurrentLocation() {

    }

    public void setStartHour(String hour){
        this.startHour = hour;
    }

    public void setFinalHour(String hour){
        this.finalHour = hour;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        Log.d(TAG, "Service created...");


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.w("===============","onStartComand");
        Log.w(TAG, "Service started...");

        Timer timer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {

                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = registerReceiver(null, ifilter);
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                currentBatteryLevel = (int) (level / (float)scale);

                Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int min = c.get(Calendar.MINUTE);
                int sec = c.get(Calendar.SECOND);

                String date = String.valueOf(hour) + ":" +
                        String.valueOf(min) + ":" +
                        String.valueOf(sec);

                /*Toast.makeText(SendCurrentLocation.this, "Level of battery: " + currentBatteryLevel +
                        " at " + date,
                        Toast.LENGTH_SHORT).show();*/

                //connectToWS(level);

                Intent localIntent = new Intent(Constants.ACTION_RUN_SERVICE)
                        .putExtra(Constants.ACTION_SEND_COORDS, "Enviando coordenadas...");

                //Emitir el intent a la actividad
                LocalBroadcastManager.
                        getInstance(SendCurrentLocation.this).sendBroadcast(localIntent);
                Log.w("ejecutando Servicio", ""+lat);

                mGoogleApiClient.connect();
            }
        };

        timer.scheduleAtFixedRate(timerTask, 0, 2000);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        timerTask.cancel();
        Intent localIntent = new Intent(Constants.ACTION_EXIT_SERVICE);

        // Emitir el intent a la actividad
        LocalBroadcastManager.
                getInstance(SendCurrentLocation.this).sendBroadcast(localIntent);
        Log.w("===============","onDesroy");
        Log.w(TAG, "Servicio destruido...");
    }

    public void connectToWS(){
        Log.w("===============","connectToWS");
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void onConnected(Bundle bundle) {
       // Log.w("===============","onConectes");
       // Log.w(TAG, "onConnected() from CommAct");
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (location != null) {
            lat = String.valueOf(location.getLatitude());
            lng = String.valueOf(location.getLongitude());


            Log.i(TAG, "Lat: " + lat);
            Log.i(TAG, "Lng: " + lng);

            mGoogleApiClient.disconnect();
            //Call method to connect with WS
            connectToWS();
        }
        else{
            lat = "0";
            lng = "0";
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection suspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
        mGoogleApiClient.disconnect();
    }
}