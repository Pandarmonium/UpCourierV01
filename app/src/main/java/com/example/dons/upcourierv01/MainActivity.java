package com.example.dons.upcourierv01;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    //Declaramos las variables para mas adelante obtener
    // y dar el comportamiento a los botones
    private Button btnClientLogIn;
    private Button btnCourierLogIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnClientLogIn = (Button) findViewById(R.id.btnMainRegisClient);
        btnCourierLogIn = (Button) findViewById(R.id.btnMainRegisCourier);

        //Comportamiento para Client, se redirige a la otra activity
        btnClientLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,RegisterClientActivity.class);
                startActivity(intent);
                finish();
            }
        });

        //Comportamiento para Courier
        btnCourierLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,RegisterCourierActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
