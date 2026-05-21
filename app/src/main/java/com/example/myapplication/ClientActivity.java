package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ClientActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        // Lier avec la vue XML
        Button btnChangerDeVue = findViewById(R.id.btnRetour);

        // Action lors du click
        btnChangerDeVue.setOnClickListener( v -> {
                   finish();
                }

        );
    }
}