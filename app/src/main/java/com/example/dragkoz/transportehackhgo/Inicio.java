package com.example.dragkoz.transportehackhgo;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Inicio extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{
    protected GoogleApiClient mGoogleApiClient;
    private String TAG = "Inicio";
    private String response;
    private boolean Loginnig = true;
    int checkInHour, checkOutHour, checkInMin, checkOutMin;
    private int triesToConnect = 0;
    double lat, lng;
    int value=0;
    ImageButton btnpanic;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        Intent intentService = new Intent(Inicio.this, SendCurrentLocation.class);
        startService(intentService);

        Intent intentServiceCA = new Intent(Inicio.this, CheckAlarma.class);
        intentServiceCA.putExtra("id_colonia", ""+getIntent().getStringExtra("id_colonia"));
        intentServiceCA.putExtra("Id_Usuario", ""+getIntent().getStringExtra("Id_Usuario"));
        startService(intentServiceCA);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
        Log.w("id",""+getIntent().getStringExtra("id_colonia"));
        btnpanic= findViewById(R.id.btnPanic);
        btnpanic.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v == btnpanic){
            new WSLogIn().execute();
            EnviarMensaje();

        }
    }

    private void EnviarMensaje (){
        PackageManager pm = this.getPackageManager();

        if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
                !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)) {
            Toast.makeText(this, "Lo sentimos, tu dispositivo probablemente no pueda enviar SMS...", Toast.LENGTH_SHORT).show();
        }
        try {
            SmsManager sms = SmsManager.getDefault();
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(this, Locale.getDefault());
            addresses = geocoder.getFromLocation(lat, lng, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName();
            sms.sendTextMessage("7712625355",null,"Se solicitán unidades en  \n dirección: "+address+". coordenadas: lat: "+lat+", long: "+lng,null,null);
            //sms.sendTextMessage("7712625355",null,"ubicacion: lat:"+lat+", long: "+lng,null,null);

            Toast.makeText(this, "Sent.", Toast.LENGTH_SHORT).show();
        }

        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Mensaje no enviado, datos incorrectos.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected() from PromotorAct");
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();
            Log.i(TAG, "Lat: " + lat);
            Log.i(TAG, "Lng: " + lng);
        }
        else{
            lat = 0;
            lng = 0;
            Log.i(TAG, "Lat: " + lat);
            Log.i(TAG, "Lng: " + lng);
        }

        if(Loginnig){
            //Call WS to download itinerario

        }

        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "No se pudo obtener tu ubicación. " +
                "Revisa los sistemas de GPS de tu dispositivo.", Toast.LENGTH_SHORT).show();
        mGoogleApiClient.disconnect();
    }

    public int dlog() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
            String NAMESPACE = "urn:WSAlarma";
            String METHOD = "WSAlarma";
            final String URL = "http://estadia.factury.mx/WS22/WSAlarma.php?wsdl";
            final String SOAP_ACTION = ""; /*NAMESPACE + "/" + METHOD*/

            SoapObject req = new SoapObject(NAMESPACE, METHOD);
            EditText nombre = (EditText) findViewById(R.id.loginUsuario);
            EditText someword = (EditText) findViewById(R.id.loginSomeword);
            req.addProperty("Valor",""+getIntent().getStringExtra("id_colonia"));

            Log.i(TAG, "Valor: " + req.getProperty("Valor"));

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

    public class WSLogIn extends AsyncTask<Void, Void, Void> {
        int response;

        ProgressDialog p;

        public WSLogIn() {
            p = new ProgressDialog(Inicio.this);
            p.setCanceledOnTouchOutside(false);
            p.setMessage("Cargando Datos...");
        }

        protected void onPreExecute() {
            //p.show();
        }

        @Override
        protected Void doInBackground(Void... unused) {
            response= dlog();
            return null;
        }

        public void onPostExecute(Void unused) {
            p.dismiss();

            System.out.println("Response: " + response);

            if (response == 1) {
                Calendar c = Calendar.getInstance();
                final int currentHour = c.get(Calendar.HOUR_OF_DAY);
                final int currentMin = c.get(Calendar.MINUTE);

                //Sup hour,min
                checkInHour = 21;
                checkInMin = 59;

                Log.i(TAG, "currentHour: "+currentHour);
                Log.i(TAG, "currentMin: "+currentMin);
                Log.i(TAG, "checkInHour: "+checkInHour);
                Log.i(TAG, "checkInMin: "+checkInMin);

                if(checkInHour == 0 && currentHour != 0){
                    Log.i(TAG, "Didn't need calculate time to activate tracker");
                    setTracker();

                    Intent intentService = new Intent(Inicio.this, SendCurrentLocation.class);
                    //Calculate minutes of tracking enabled
                    int minsToLastCheckOut = checkOutHour * 60 + checkOutMin;

                    if(checkOutHour != 0 && checkOutMin != 0) {
                        int diff = minsToLastCheckOut - (currentHour * 60 - currentMin * 60);
                        intentService.putExtra("howManyMin", diff);
                    }

                    startService(intentService);
                }
                else {
                    //Start location tracking waiting for active it
                    int minutesLapsed = currentHour * 60 + currentMin;
                    int minutesForFirstCheckIn = checkInHour * 60 + checkInMin - 60; //Minus some mins

                    int diff = minutesLapsed - minutesForFirstCheckIn;

                    Log.i(TAG, "Minutes lapsed: "+minutesLapsed);
                    Log.i(TAG, "Minutes for first check in: "+minutesForFirstCheckIn);
                    Log.i(TAG, "Minutes lapsed: "+diff);
                    Log.i(TAG, "Activate tracker? "+(diff >= 0));

                    //minlapsed = 100, minForFirst = 140 (200)
                    if(diff > 0)
                        diff = 0;
                    else
                        diff = diff * -1 * 1000; //transform to milisec

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Started service!!!!");

                            setTracker();

                            Intent intentService = new Intent(Inicio.this, SendCurrentLocation.class);

                            int minsToLastCheckOut = checkOutHour * 60 + checkOutMin;

                            if(checkOutHour != 0 && checkOutMin != 0) {
                                int diff = minsToLastCheckOut - (currentHour * 60 - currentMin * 60);
                                intentService.putExtra("howManyMin", diff);
                            }

                            startService(intentService);
                        }
                    }, diff);
                }


                if(((checkInHour - 1) <= currentHour) &&
                        (checkInMin) <= currentMin ){
                    setTracker();

                    Intent intentService = new Intent(Inicio.this, SendCurrentLocation.class);
                    startService(intentService);
                } else if (checkInHour == 0){
                    setTracker();

                    Intent intentService = new Intent(Inicio.this, SendCurrentLocation.class);
                    startService(intentService);
                }

                Toast.makeText(Inicio.this, "¡Misión cumplida!", Toast.LENGTH_SHORT).show();
            }
            else if (response == -5)
                Toast.makeText(Inicio.this, "No pudimos conectarnos a Internet :(",
                        Toast.LENGTH_SHORT).show();
            else if (response == -10 && triesToConnect < 2){
                new Inicio.WSLogIn().execute();
                triesToConnect++;
            }
            else
                Toast.makeText(Inicio.this, "Hemos tenido un problema. " +
                        "Intenta de nuevo ;)", Toast.LENGTH_SHORT).show();

            Loginnig = false;
        }
    }

    static class ResponseReceiver extends BroadcastReceiver {
        // Sin instancias
        ResponseReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {

        }

    }

    public void processJSON(){

        try {
            JSONArray array = new JSONArray(response);
            for (int i = 0; i < array.length(); i++){
                JSONObject obj = array.getJSONObject(i);
                if(!obj.getString("Id_Colonia").equals("")){
                    Intent intent = new Intent(this, Inicio.class);
                    intent.putExtra("id_colonia", ""+obj.getString("Id_Colonia"));
                    startActivity(intent);
                    //Toast.makeText(this,"logrado", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setTracker(){
        // Filtro de acciones que serán alertadas
        IntentFilter filter = new IntentFilter(Constants.ACTION_SEND_COORDS);

        filter.addAction(Constants.ACTION_RUN_SERVICE);
        filter.addAction(Constants.ACTION_EXIT_SERVICE);
        filter.addAction(Constants.ACTION_SEND_COORDS);

        // Crear un nuevo ResponseReceiver
        Inicio.ResponseReceiver receiver =
                new Inicio.ResponseReceiver();
        Inicio.ResponseReceiver receiverN = new Inicio.ResponseReceiver();
        // Registrar el receiver y su filtro
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }
}
