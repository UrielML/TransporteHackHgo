package com.example.dragkoz.transportehackhgo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.HeaderProperty;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class CheckAlarma extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = CheckAlarma.class.getSimpleName();

    private String response = "";

    TimerTask timerTask;
    public String idc, idu;
    public boolean starAlarm=true;

    private String startHour = "";
    private String finalHour = "";

    private String lat = "", lng = "";
    private int currentBatteryLevel = 0;

    private GoogleApiClient mGoogleApiClient;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 2000;

    public CheckAlarma() {

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
        idc= intent.getStringExtra("id_colonia");
        idu= intent.getStringExtra("Id_Usuario");
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

                /*Toast.makeText(CheckAlarma.this, "Level of battery: " + currentBatteryLevel +
                        " at " + date,
                        Toast.LENGTH_SHORT).show();*/

                //connectToWS(level);

                Intent localIntent = new Intent(Constants.ACTION_RUN_SERVICE)
                        .putExtra(Constants.ACTION_SEND_COORDS, "Enviando coordenadas...");

                //Emitir el intent a la actividad
                LocalBroadcastManager.
                        getInstance(CheckAlarma.this).sendBroadcast(localIntent);
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
                getInstance(CheckAlarma.this).sendBroadcast(localIntent);
        Log.w("===============","onDesroy");
        Log.w(TAG, "Servicio destruido...");
    }

    public int connectToWS() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
            String NAMESPACE = "urn:WSAlarma";
            String METHOD = "WSAlarma";
            final String URL = "http://estadia.factury.mx/WS22/CheckAlarma.php?wsdl";
            final String SOAP_ACTION = ""; /*NAMESPACE + "/" + METHOD*/

            SoapObject req = new SoapObject(NAMESPACE, METHOD);
            req.addProperty("Id_Colonia",""+idc);
            req.addProperty("Id_Usuario", ""+idu);

            Log.i(TAG, "Id_Colonia: " + req.getProperty("Id_Colonia"));

            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER10);
            envelope.encodingStyle = SoapSerializationEnvelope.ENC2003;
            envelope.dotNet = true;
            envelope.setOutputSoapObject(req);

            HttpTransportSE ht = new HttpTransportSE(URL, 20000);
            ht.debug = true;

            try {
                ArrayList<HeaderProperty> headerPropertyArrayList = new ArrayList<>();
                headerPropertyArrayList.add(new HeaderProperty("Connection", "close"));

                ht.call(SOAP_ACTION, envelope, headerPropertyArrayList);

                SoapPrimitive result = (SoapPrimitive) envelope.getResponse();
                response = result.toString();

                System.out.println("Response from CrearCamion: " + response);
                System.out.println("ResponseDump: " + ht.responseDump);

                if (response.length() > 0)
                    if(response.equals("-1"))
                        return -1;
                    else if(response.equals("-2"))
                        return -2;
                    else if(response.equals("-3"))
                        return -3;
                    else
                        return 1;
                else
                    return -1;

            } catch (SocketTimeoutException e) {
                return -10;
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            }
        } else
            return -4;
        return -5;
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
            int respons = connectToWS();
            if (respons == 1) {
                    suenaAlarma();

            }
        }
        else{
            lat = "0";
            lng = "0";
            mGoogleApiClient.disconnect();
        }
    }

    private void suenaAlarma() {
        if(starAlarm==true){
            try {
                JSONArray array = new JSONArray(response);
                for (int i = 0; i < array.length(); i++){
                    JSONObject obj = array.getJSONObject(i);
                    Log.w("check",obj.getString("Alarma"));
                    if(obj.getString("Alarma").equals("1")){
                        createNotification();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{

        }
    }

    public void createNotification(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(intent);
        builder.setTicker(getResources().getString(R.string.custom_notification));
        builder.setSmallIcon(R.drawable.ic_launcher_background);
        builder.setAutoCancel(true);
        Notification notification = builder.build();
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification);
        final String time = DateFormat.getTimeInstance().format(new Date()).toString();
        final String text = getResources().getString(R.string.collapsed, time);
        contentView.setTextViewText(R.id.textView, text);
        notification.contentView = contentView;
        if (Build.VERSION.SDK_INT >= 16) {
            // Inflate and set the layout for the expanded notification view
            RemoteViews expandedView =
                    new RemoteViews(getPackageName(), R.layout.notification_expanded);
            notification.bigContentView = expandedView;
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(0, notification);
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
