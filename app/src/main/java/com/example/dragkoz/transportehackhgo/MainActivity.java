package com.example.dragkoz.transportehackhgo;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
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

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{

    protected GoogleApiClient mGoogleApiClient;
    private String TAG = "LogIn";
    private String response;
    private boolean Loginnig = true;
    int checkInHour, checkOutHour, checkInMin, checkOutMin;
    private int triesToConnect = 0;
    double lat, lng;
    int value=0;
    EditText loginUsuario,loginSomeword;
    Button btnLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
        btnLog=findViewById(R.id.buttonLogin);
        btnLog.setOnClickListener(this);
    }

    @Override
    public void onClick(View v){
        if(v==btnLog){
            new WSLogIn().execute();
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
        SharedPreferences sp = getSharedPreferences("loginData", Context.MODE_PRIVATE);
        EditText nombre = (EditText) findViewById(R.id.loginUsuario);
        EditText someword = (EditText) findViewById(R.id.loginSomeword);
        if((sp.getString("username", null) == null && sp.getString("someword", null) == null)
                || (!sp.getString("username", null).equals(nombre.getText().toString()) &&
                !sp.getString("someword", null).equals(someword.getText().toString()))){

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("username", nombre.getText().toString());
            editor.putString("someword", someword.getText().toString());
            editor.apply();
        }
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
            String NAMESPACE = "urn:WSAutntica";
            String METHOD = "Autntica";
            final String URL = "https://bitgeekenvironments.net:443/redVigilante/WebServices/AutenticaUsuario.php?wsdl";
            final String SOAP_ACTION = ""; /*NAMESPACE + "/" + METHOD*/

            SoapObject req = new SoapObject(NAMESPACE, METHOD);
            EditText nombre = (EditText) findViewById(R.id.loginUsuario);
            EditText someword = (EditText) findViewById(R.id.loginSomeword);
            req.addProperty("nickName",""+nombre.getText());
            req.addProperty("password", ""+someword.getText());

            Log.i(TAG, "nickName: " + req.getProperty("nickName"));
            Log.i(TAG, "password: " + req.getProperty("password"));

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
                //Save data


                System.out.println("Response from CrearCamion: " + response);
                System.out.println("ResponseDump: " + ht.responseDump);

                if (response.length() > 0)
                    if(response.equals("-1"))
                        return -1;
                    else if(response.equals("-2"))
                        return -2;
                    else if(response.equals("-3"))
                        return -3;
                    else if(response.equals("-8"))
                        return -8;
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
            p = new ProgressDialog(MainActivity.this);
            p.setCanceledOnTouchOutside(false);
            p.setMessage("Cargando Datos...");
        }

        protected void onPreExecute() {
            p.show();
            SharedPreferences sp = getSharedPreferences("loginData", Context.MODE_PRIVATE);
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
                processJSON();

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

                    Intent intentService = new Intent(MainActivity.this, SendCurrentLocation.class);
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

                            Intent intentService = new Intent(MainActivity.this, SendCurrentLocation.class);

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

                    Intent intentService = new Intent(MainActivity.this, SendCurrentLocation.class);
                    startService(intentService);
                } else if (checkInHour == 0){
                    setTracker();

                    Intent intentService = new Intent(MainActivity.this, SendCurrentLocation.class);
                    startService(intentService);
                }


            }
            else if (response == -8)
                Toast.makeText(MainActivity.this, "Usuario o Contraseña erronea",
                        Toast.LENGTH_SHORT).show();
            else if (response == -5)
                Toast.makeText(MainActivity.this, "No pudimos conectarnos a Internet :(",
                        Toast.LENGTH_SHORT).show();
            else if (response == -10 && triesToConnect < 2){
                new MainActivity.WSLogIn().execute();
                triesToConnect++;
            }
            else
                Toast.makeText(MainActivity.this, "Hemos tenido un problema. " +
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
                    Intent intent = new Intent(this, Inicio.class);
                    intent.putExtra("Id_Usuarios", ""+obj.getString("Id_Usuarios"));
                    intent.putExtra("Id_Personas", ""+obj.getString("Id_Personas"));
                    intent.putExtra("Id_DGrupo", ""+obj.getString("Id_DGrupo"));
                    intent.putExtra("Id_TUsuarios", ""+obj.getString("Id_TUsuarios"));
                    intent.putExtra("Id_Grupo", ""+obj.getString("Id_Grupo"));
                    startActivity(intent);
                    //Toast.makeText(this,"logrado", Toast.LENGTH_SHORT).show();
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
        MainActivity.ResponseReceiver receiver =
                new MainActivity.ResponseReceiver();
        MainActivity.ResponseReceiver receiverN = new MainActivity.ResponseReceiver();
        // Registrar el receiver y su filtro
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }
}
