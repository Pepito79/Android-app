package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

// Classe pour Monitoring sur le serveur
public class MonitoringActivity extends AppCompatActivity {

    private TextView tvDataDisplay;
    private ConnectedThread btThread;
    private final Handler handler = new Handler();
    // Structure pour stocker la data reçue
    private Map<String, JSONObject> serverDeviceMap = new HashMap<>();
    private  final String url = "http://happyresto.enseeiht.fr/smartHouse/api/v1/devices/15";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor); // Assure-toi que le XML existe

        tvDataDisplay = findViewById(R.id.tvStatus);

        // On récupère le socket bluetooth
        if(ServeurActivity.globalSocket != null) {
            btThread = new ConnectedThread(ServeurActivity.globalSocket,this);
            btThread.start();
            startRepeatingFetch();

        }
    }

    /**
     * Reçoit l'ordre du client (via ConnectedThread) et lance le POST
     */
    public void handleClientAction(String command) {
        String[] parts = command.split(":");
        if (parts.length == 3) {
            String deviceId = parts[1];
            String targetState = parts[2].equals("ON") ? "1" : "0";

            Log.d("BT_SERVER", "Commande reçue : " + command);
            updateDeviceOnAPI(deviceId, targetState);
        }
    }

    /**
     * Envoie la modification au serveur
     */
    private void updateDeviceOnAPI(String id, String state) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("VOLLEY_POST", "Succès : " + response);
                    // On rafraîchit immédiatement pour synchroniser serveur et client
                    fetchData();
                },
                error -> Log.e("VOLLEY_POST", "Erreur : " + error.getMessage())
        )
        {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("ID", id);
                params.put("STATE", state);
                return params;
            }
        };
        queue.add(postRequest);
    }

    private void startRepeatingFetch() {
        // Handler utilisé ici pour échanger data entre thread + répéter chaque 5 secondes
        handler.post(new Runnable() {
            @Override
            public void run() {
                fetchData();
                // Exrecution chaque 5 seconde en background
                handler.postDelayed(this, 5000);
            }
        });
    }

    private void fetchData() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject device = response.getJSONObject(i);
                            String id = device.getString("ID"); // On utilise l'ID comme clé
                            serverDeviceMap.put(id, device);
                        }

                        // On construit l'affichage à partir de la Map
                        StringBuilder builder = new StringBuilder();
                        for (JSONObject device : serverDeviceMap.values()) {
                            String name = device.getString("NAME");
                            int state = device.getInt("STATE");

                            builder.append(name).append(" : ")
                                    .append(state == 1 ? "ON" : "OFF")
                                    .append("\n");
                        }

                        // Mise à jour de l'écran du serveur
                        tvDataDisplay.setText(builder.toString());

                        // On envoie  le JSONArray complet au client pour l'afficher
                        if (btThread != null) {
                            btThread.write(response.toString());
                        }

                    } catch (JSONException e) {
                        Log.e("JSON_PARSE", "Erreur : " + e.getMessage());
                    }
                },
                error -> Log.e("VOLLEY", "Erreur réseau : " + error.getMessage())
        );

        queue.add(jsonArrayRequest);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (btThread != null) {
            btThread.interrupt();
        }
    }
}
