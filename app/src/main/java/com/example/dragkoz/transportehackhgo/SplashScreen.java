package com.example.dragkoz.transportehackhgo;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;

public class SplashScreen extends AppCompatActivity {
    public static final int segundos=2;
    public static final int milisegundos=segundos*1000;
    public static final int delay=2;
    private ProgressBar progessBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        progessBar=(ProgressBar) findViewById(R.id.progressBar);
        progessBar.setMax(maximoProgreso());
        empezarAnim();

    }
    public void empezarAnim(){
        new CountDownTimer(milisegundos, 1000){
            @Override
            public void onTick(long millisUntilFinished) {
                progessBar.setProgress(establecerProgres(millisUntilFinished));
            }
            @Override
            public void onFinish() {
                Intent nuevoFrom = new Intent(SplashScreen.this, MainActivity.class);
                startActivity(nuevoFrom);
                finish();
            }
        }.start();
}
public int establecerProgres(long miliseconds){
        return (int)((milisegundos-miliseconds)/1000);
}
public int maximoProgreso(){ return segundos-delay; }
}