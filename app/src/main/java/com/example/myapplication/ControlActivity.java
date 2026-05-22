package com.example.myapplication;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class ControlActivity extends AppCompatActivity {

    private ConnectedThread connectedThread;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        container = findViewById(R.id.container);

        if (ClientActivity.globalSocket != null) {
            listenToBluetooth();
        }
    }

    private void listenToBluetooth() {
        connectedThread = new ConnectedThread(ClientActivity.globalSocket) {
            @Override
            public void run() {
                byte[] buffer = new byte[8192];
                int bytes;
                try {
                    // On récupère le flux depuis le socket global
                    InputStream in = ClientActivity.globalSocket.getInputStream();
                    while (true) {
                        bytes = in.read(buffer);
                        if (bytes > 0) {
                            String jsonStr = new String(buffer, 0, bytes);
                            runOnUiThread(() -> parseAndDisplay(jsonStr));
                        }
                    }
                } catch (IOException e) {
                    Log.e("BT_CLIENT", "Erreur lecture", e);
                }
            }
        };
        connectedThread.start();
    }

    private void parseAndDisplay(String jsonData) {
        try {
            JSONArray array = new JSONArray(jsonData);
            container.removeAllViews();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                // 1. Ligne horizontale (Gris clair)
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setBackgroundColor(Color.parseColor("#E0E0E0"));
                row.setPadding(40, 40, 40, 40);

                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, 4); // Séparateur gris foncé
                row.setLayoutParams(rowParams);

                // 2. Textes (Vertical)
                LinearLayout textLayout = new LinearLayout(this);
                textLayout.setOrientation(LinearLayout.VERTICAL);
                textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvTitle = new TextView(this);
                String title = "[" + obj.getString("BRAND") + "-" + obj.getString("MODEL") + "] " + obj.getString("NAME");
                tvTitle.setText(title);
                tvTitle.setTypeface(null, Typeface.BOLD);
                tvTitle.setTextColor(Color.parseColor("#333333"));

                TextView tvInfo = new TextView(this);
                int autonomy = obj.optInt("AUTONOMY", -1);
                String data = obj.optString("DATA", "");
                String infoStr = (autonomy != -1) ? "Autonomy : " + autonomy + "%" : "Data : " + data;
                tvInfo.setText(infoStr);
                tvInfo.setTextColor(Color.parseColor("#666666"));

                textLayout.addView(tvTitle);
                textLayout.addView(tvInfo);

                // 3. Bouton
                Button btn = new Button(this);
                int state = obj.getInt("STATE");
                String id = obj.getString("ID");
                btn.setText(state == 1 ? "ON" : "OFF");

                // Style du bouton
                btn.setLayoutParams(new LinearLayout.LayoutParams(250, ViewGroup.LayoutParams.WRAP_CONTENT));
                btn.setBackgroundColor(Color.parseColor("#B0BEC5")); // Gris bleuté comme l'image

                btn.setOnClickListener(v -> {
                    if (connectedThread != null) {
                        // Envoi de la commande simple au serveur
                        String action = (state == 1) ? "OFF" : "ON";
                        connectedThread.write("TOGGLE:" + id + ":" + action);
                    }
                });

                row.addView(textLayout);
                row.addView(btn);
                container.addView(row);
            }
        } catch (JSONException e) {
            Log.e("BT_CLIENT", "JSON mal formé");
        }
    }
}