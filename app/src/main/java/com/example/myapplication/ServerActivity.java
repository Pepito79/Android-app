package com.example.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerActivity extends AppCompatActivity {
    private static final String TAG = "SERVER_ACTIVITY";
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-82eb-11ed-a1eb-0242ac120002");
    private static final String NAME = "SmartHouseServer";
    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread acceptThread;
    private TextView statusText;

    // Handler pour traiter les messages reçus du client via Bluetooth
    private final Handler serverHandler = new Handler(Looper.getMainLooper(), msg -> {
        try {
            byte[] readBuf = (byte[]) msg.obj;
            String commande = new String(readBuf, 0, msg.arg1);
            Log.d(TAG, "Commande reçue du client : " + commande);

            // Vérification de sécurité sur le format de la commande "TOGGLE:ID"
            if (commande.startsWith("TOGGLE:") && commande.contains(":")) {
                String[] parts = commande.split(":");
                if (parts.length >= 2) {
                    int deviceId = Integer.parseInt(parts[1]);
                    toggleDeviceState(deviceId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement de la commande Bluetooth", e);
        }
        return true;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Activer le mode EdgeToEdge
        EdgeToEdge.enable(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        // 2. Gérer les Insets pour éviter que le texte ne soit caché par les barres système
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.status_text), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusText = findViewById(R.id.status_text);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            statusText.setText("Bluetooth non supporté sur cet appareil");
            return;
        }

        // Lancer le thread qui attend la connexion
        statusText.setText("En attente de connexion client...");
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                // Nécessite la permission BLUETOOTH_CONNECT sur Android 12+
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (SecurityException | IOException e) {
                Log.e(TAG, "Échec de création du ServerSocket", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    if (mmServerSocket == null) break;
                    socket = mmServerSocket.accept(); // Bloquant
                } catch (IOException e) {
                    break;
                }

                if (socket != null) {
                    // Connexion réussie
                    runOnUiThread(() -> statusText.setText("Client Connecté ! Envoi des données..."));

                    // On démarre la communication
                    BluetoothCommunicationManager.startCommunication(socket, serverHandler);

                    // Envoi initial des données
                    fetchDevicesAndSendToClient();

                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    private void fetchDevicesAndSendToClient() {
        String url = "http://happyresto.enseeiht.fr/smartHouse/api/v1/devices/31";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    // Envoi sécurisé via le manager
                    BluetoothCommunicationManager.send(response.toString());
                },
                error -> Log.e(TAG, "Erreur Volley Fetch: " + error.toString())
        );
        queue.add(request);
    }

    private void toggleDeviceState(int deviceId) {
        String url = "http://happyresto.enseeiht.fr/smartHouse/api/v1/devices";
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    // Mise à jour automatique après l'action
                    fetchDevicesAndSendToClient();
                },
                error -> Log.e(TAG, "Erreur Volley Toggle: " + error.toString())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("deviceId", String.valueOf(deviceId));
                params.put("houseId", "31");
                params.put("action", "turnOnOff");
                return params;
            }
        };
        queue.add(req);
    }
}