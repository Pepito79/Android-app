package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        Button btnClient = findViewById(R.id.btn_client);
        Button btnServer = findViewById(R.id.btn_server);

        btnClient.setOnClickListener(v -> {
            // On passe au mode Client
            Intent intent = new Intent(RoleSelectionActivity.this, SecondActivity.class);
            startActivity(intent);
            finish(); // On ferme cette activité pour ne pas revenir en arrière
        });

        btnServer.setOnClickListener(v -> {
            // On passe au mode Serveur
            Intent intent = new Intent(RoleSelectionActivity.this, ServerActivity.class);
            startActivity(intent);
            finish();
        });
    }
}