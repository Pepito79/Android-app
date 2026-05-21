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

        // Lier avec la vue XML
        CardView cardClient = findViewById(R.id.cardTelecomande);
        CardView cardServeur = findViewById(R.id.cardServeur);

        // Action lors du click pour choisir le rôle de client
        cardClient.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, ClientActivity.class);
                    startActivity(intent);
                }


        );


        // Action lors du click pour choisir le rôle de serveur
        cardServeur.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, ServeurActivity.class);
                    startActivity(intent);
                }
        );
    }
}