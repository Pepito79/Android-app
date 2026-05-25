package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * Point d'entree de l'application.
 * Gere l'affichage du menu principal, la demande de permissions Bluetooth
 * et la navigation vers les roles Client/Serveur.
 */
public class MainActivity extends AppCompatActivity {
    // Écran principal
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Autorisation pour que l'application puisse scanner les appareils bluetooth
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
            }, 1);
        }
        // Lier avec la vue XML lors du click sur telecommande cela ratache l'action au composant
        CardView cardClient = findViewById(R.id.cardTelecomande);
        CardView cardServeur = findViewById(R.id.cardServeur);

        // Le click permet de basculer sur l'activité ClienActivity
        cardClient.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, ClientActivity.class);
                    startActivity(intent);
                }


        );

        // Le click permet de basculer sur le ServeurActivity
        cardServeur.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, ServeurActivity.class);
                    startActivity(intent);
                }
        );
    }
}