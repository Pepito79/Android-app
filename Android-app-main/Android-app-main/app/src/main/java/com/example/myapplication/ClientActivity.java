package com.example.myapplication;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

// Pas besoin de redemander la permission pour les appareils bluetooth
@SuppressLint("MissingPermission")
public class ClientActivity extends AppCompatActivity {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    public static BluetoothSocket globalSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_waiting);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // On recherche du serveur dans les appareils déjà appairés (paired devices)
        BluetoothDevice serverDevice = null;
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                // On cherche l'appareil avec ce nom
                if ("IoTCommanderServer".equals(device.getName())) {
                    serverDevice = device;//on le sauvegarde
                    break;
                }
            }
        }

        // On tente de se connecter au serveur
        if (serverDevice != null) {
            ConnectionThread connectThread = new ConnectionThread(serverDevice);
            connectThread.start();
        } else {
            Toast.makeText(this, "Serveur introuvable. Avez-vous appairé les téléphones ?", Toast.LENGTH_LONG).show();
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        // on save le socket en tant que variable global pour pouvoir le reutiliser après
        globalSocket = socket;

        //On crée un thread qui va s'executer en arrière-plan pour ne pas bloquer l'UI on donne a ce thread le socket pour qu
        //etablir le tunel avec le serveur et un handler pour l'instant null pour permettre la communication entre ce thread et controlActivity
        BluetoothCommunicationThread commThread = BluetoothCommunicationThread.getInstance(socket, null);
        commThread.start();

        runOnUiThread(() -> {
            // La connection est établis donc on passe a ControlActivity pour l'affichage des appareils
            Intent intent = new Intent(this, ControlActivity.class);
            startActivity(intent);
            finish(); // On ferme l'activité actuelle
        });
    }

    // Thread de connexion (en arrière-plan pour ne pas bloquer l'écran) pendant la connection ble après il se ferme
    public class ConnectionThread extends Thread {
        private final BluetoothSocket socket;

        public ConnectionThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e("BT_CLIENT", "Erreur de création du socket", e);
            }
            socket = tmp;
        }

        public void run() {
            //On arrete de rechercher encore des appareils
            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect(); // Le code se bloque ici en attendant que le serveur décroche
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("BT_CLIENT", "Erreur lors de la fermeture du socket", e1);
                }
                return;
            }

            // Si on arrive ici, la connexion a réussi !
            manageConnectedSocket(socket);
        }
    }
}