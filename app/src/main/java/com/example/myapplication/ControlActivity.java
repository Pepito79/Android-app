package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
                    // Revoir condition boucle
                    while (!Thread.currentThread().isInterrupted()) {
                        bytes = in.read(bytesBuffer);
                        if (bytes > 0) {
                            String partialData = new String(bytesBuffer, 0, bytes);
                            // Revoir stringBuilder utilité
                            jsonBuffer.append(partialData);

                            String currentContent = jsonBuffer.toString().trim();

                            // Cas 1 : Update réussi par le serveur
                            if (currentContent.contains("CONFIRM:UPDATE_SUCCESS")) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ControlActivity.this, "Mise à jour réussie !", Toast.LENGTH_SHORT).show();
                                });
                                jsonBuffer.setLength(0); // On vide le buffer
                            }

                            // Cas 2 réception du JSON complet
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
            JSONArray array = new JSONArray(jsonData);

            // On vide la Map pour s'assurer d'avoir les données les plus fraîches de l'API
            deviceMap.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                deviceMap.put(obj.getString("ID"), obj);
            }

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

                // L'UI se met à jour ici selon le "state" reçu du serveur
                btn.setText(state == 1 ? "ON" : "OFF");
                btn.setBackgroundColor(state == 1 ? Color.parseColor("#4CAF50") : Color.LTGRAY); // Vert si ON

                btn.setOnClickListener(v -> {
                    if (connectedThread != null) {
                        // On envoie l'ordre inverse au serveur
                        String targetState = (state == 1) ? "OFF" : "ON";
                        connectedThread.write("TOGGLE:" + id + ":" + targetState);

                        // Optionnel : Désactiver le bouton temporairement pour éviter le multi-clic
                        btn.setEnabled(false);
                        btn.setText("...");
                    }
                });

                container.addView(deviceView);
            }
        } catch (JSONException e) {
            Log.e("BT_CLIENT", "Erreur JSON : " + e.getMessage());
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectedThread != null) {
            connectedThread.interrupt();
        }
        // On ferme aussi le connexion bt pour eviter d'utiliser des ressources
        try {
            if (ClientActivity.globalSocket != null) {
                ClientActivity.globalSocket.close();
            }
        } catch (IOException e) {
            Log.e("BT_CLIENT", "Erreur fermeture socket");
        }

        Log.d("BT_CLIENT", "Nettoyage ControlActivity terminé");
    }


}
