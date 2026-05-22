package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ControlActivity extends AppCompatActivity {
    private StringBuilder jsonBuffer = new StringBuilder();
    private ConnectedThread connectedThread;
    private LinearLayout container;
    private Map<String, JSONObject> deviceMap = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        container = findViewById(R.id.container);

        if (ClientActivity.globalSocket != null) {
            Log.d("BT_CLIENT","Ecoute en cours du socket");
            listenToBluetooth();
        }
    }

    private void listenToBluetooth() {
        connectedThread = new ConnectedThread(ClientActivity.globalSocket) {
            @Override
            public void run() {
                byte[] bytesBuffer = new byte[8192];
                int bytes;
                try {
                    InputStream in = ClientActivity.globalSocket.getInputStream();
                    while (!Thread.currentThread().isInterrupted()) {
                        bytes = in.read(bytesBuffer);
                        if (bytes > 0) {
                            String partialData = new String(bytesBuffer, 0, bytes);
                            jsonBuffer.append(partialData);

                            String currentContent = jsonBuffer.toString().trim();

                            // On vérifie si on a reçu le début '[' et la fin ']'
                            if (currentContent.startsWith("[") && currentContent.endsWith("]")) {
                                final String finalJson = currentContent;
                                runOnUiThread(() -> {
                                    updateMapAndUI(finalJson);
                                    // Une fois affiché, on vide le buffer pour le prochain envoi
                                    jsonBuffer.setLength(0);
                                });
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e("BT_CLIENT", "Connexion perdue", e);
                }
            }
        };
        connectedThread.start();
    }
    private void updateMapAndUI(String jsonData) {
        try {
            // On tente de parser et de remplir la Map d'abord
            JSONArray array = new JSONArray(jsonData);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                deviceMap.put(obj.getString("ID"), obj);
            }
            // Mise à jour de l'affichage
            container.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);

            for (Map.Entry<String, JSONObject> entry : deviceMap.entrySet()) {
                JSONObject obj = entry.getValue();
                View deviceView = inflater.inflate(R.layout.item_device, container, false);

                TextView tvBrandModel = deviceView.findViewById(R.id.tv_device_brand_model);
                TextView tvName = deviceView.findViewById(R.id.tv_device_name);
                TextView tvInfo = deviceView.findViewById(R.id.tv_device_info);
                Button btn = deviceView.findViewById(R.id.btn_device_toggle);

                tvBrandModel.setText("[" + obj.getString("BRAND") + "-" + obj.getString("MODEL") + "]");
                tvName.setText(obj.getString("NAME"));

                int autonomy = obj.optInt("AUTONOMY", -1);
                tvInfo.setText(autonomy != -1 ? "Autonomy : " + autonomy + "%" : "Data : " + obj.optString("DATA"));

                int state = obj.getInt("STATE");
                String id = entry.getKey();

                btn.setText(state == 1 ? "ON" : "OFF");
                btn.setBackgroundColor(state == 1 ? Color.parseColor("#B0BEC5") : Color.LTGRAY);

                btn.setOnClickListener(v -> {
                    if (connectedThread != null) {
                        connectedThread.write("TOGGLE:" + id + ":" + (state == 1 ? "OFF" : "ON"));
                    }
                });

                container.addView(deviceView);
            }
        } catch (JSONException e) {
            Log.e("BT_CLIENT", "Erreur JSON ignorée" + e.getMessage());
        }
    }
}
