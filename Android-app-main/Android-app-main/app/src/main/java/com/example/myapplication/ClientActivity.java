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
// SuprressLint pour éviter d'avoir le warning
@SuppressLint("MissingPermission")
public class ClientActivity extends AppCompatActivity {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    public static BluetoothSocket globalSocket;

    /**
     * Initialise l'activite et lance la recherche du serveur Bluetooth.
     * Utilise le BluetoothAdapter pour filtrer les appareils deja appaires (Bonded Devices).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_waiting);

        // L'adapter gère l'accès au matériel Bluetooth, la liste des appareils appairés
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Recherche du serveur dans les appareils déjà appairés (paired devices)
        BluetoothDevice serverDevice = null;
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                // On cherche l'appareil avec ce nom
                if ("IoTCommanderServer".equals(device.getName())) {
                    //On le sauvegarde
                    serverDevice = device;
                    break;
                }
            }
        }

        // On tente de se connecter au serveur
        if (serverDevice != null) {
            ConnectionThread connectThread = new ConnectionThread(serverDevice);
            connectThread.start();
        } else {
            // Affichage d'un toast en bas  pour l'experience client
            Toast.makeText(this, "Serveur introuvable. Avez-vous appairé les téléphones ?", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Transitionne vers l'etat connecte.
     * Initialise le Singleton de communication et bascule l'affichage vers ControlActivity.
     * @param socket Le socket Bluetooth etabli et pret a l'emploi.
     */
    private void manageConnectedSocket(BluetoothSocket socket) {
        // On sauvegarde le socket en tant que variable global pour pouvoir le reutiliser après
        globalSocket = socket;

        //On crée un thread qui va s'executer en arrière-plan pour ne pas bloquer l'UI
        BluetoothCommunicationThread commThread = BluetoothCommunicationThread.getInstance(socket, null);
        commThread.start();

        runOnUiThread(() -> {
            // La connection est établie donc on passe a ControlActivity pour l'affichage des appareils
            Intent intent = new Intent(this, ControlActivity.class);
            startActivity(intent);
            // On termine l'activité actuelle
            finish();
        });
    }

    /**
     * Thread dedie a la tentative de connexion initiale.
     * Effectue l'operation bloquante socket.connect() en dehors du thread principal (UI Thread).
     */
    public class ConnectionThread extends Thread {
        private final BluetoothSocket socket;
        /**
         * Constructeur du thread de connexion.
         * Cree le socket RFCOMM en utilisant l'UUID définit
         * @param device Le peripherique Bluetooth cible.
         */
        public ConnectionThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            try {
                // Prépare le canal de communication (Socket) avec l'identifiant unique (UUID) du serveur
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e("BT_CLIENT", "Erreur de création du socket", e);
            }
            socket = tmp;
        }
        /**
         * Execution du thread : arrete la decouverte (Discovery) pour maximiser
         * les performances et tente la connexion physique.
         */
        public void run() {
            //On arrete de rechercher des appareils
            bluetoothAdapter.cancelDiscovery();
            try {
                // Le code se bloque ici en attendant que le serveur décroche
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("BT_CLIENT", "Erreur lors de la fermeture du socket", e1);
                }
                return;
            }

            // On laisse cette fonction s'occuper du reste
            manageConnectedSocket(socket);
        }
    }
}