package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // A partir de android 12 il faut une autorisation pour que l'application puisse scanner les appareils bluetooth
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
            }, 1);
        }
        // Lier avec la vue XML quand tu clique sur telecommande ca ratache ton action a cardClient
        CardView cardClient = findViewById(R.id.cardTelecomande);
        CardView cardServeur = findViewById(R.id.cardServeur);

        // Le clique permet d'ouvrir directement ClientActivity
        cardClient.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, ClientActivity.class);
                    startActivity(intent);
                }


        );


        // Le clique permet d'ouvrir directement ServeurActivity
        cardServeur.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, ServeurActivity.class);
                    startActivity(intent);
                }
        );
    }
}