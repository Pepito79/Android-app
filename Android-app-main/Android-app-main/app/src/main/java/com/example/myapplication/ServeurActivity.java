package com.example.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.UUID;


public class ServeurActivity extends AppCompatActivity {

    // Identifiant unique (UUID) identique a celui du client pour permettre la liaison
    private  static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //Un nom spécifique pour le serveur
    private static  final String NAME = "IoTCommanderServer";
    // Socket statique pour maintenir la connexion entre les activites
    public static BluetoothSocket globalSocket;

    /**
     * Initialise l'activite et lance le thread d'ecoute du serveur.
     * Affiche l'ecran d'attente (activity_server_waiting).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_waiting);

        // Lancement du serveur
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
    }

    /**
     * Gere le passage du mode "Attente" au mode "Connecte".
     * Initialise le flux de communication et bascule vers l'ecran de Monitoring.
     * @param socket Le socket Bluetooth recu lors de la connexion du client.
     */
    private void manageConnectedSocket(BluetoothSocket socket) {
        // On sauvegarde le socket
        globalSocket = socket;

        //  pareil que pour client
        BluetoothCommunicationThread commThread = BluetoothCommunicationThread.getInstance(socket, null);

        // Lancement du Thread d'écoute en arrière-plan
        commThread.start();

        //on ouvre monitoring globalement jusque la c'est pareil que pour le client
        runOnUiThread(() -> {
            Intent intent = new Intent(ServeurActivity.this, MonitoringActivity.class);
            startActivity(intent);
        });
    }



    /**
     * Thread dedie a l'ecoute des connexions entrantes.
     * Le serveur "bloque" sur la methode accept() jusqu'a ce qu'un client se connecte.
     */
    public class AcceptThread extends Thread {
        // Socket serveur qui permettera d'écouter les connexions entrantes
        private final BluetoothServerSocket serverSocket;

        /**
         * Constructeur : Initialise le BluetoothServerSocket en utilisant le protocole RFCOMM.
         */
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                // L'adapter permet d'obtenir le ServerSocket
                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e("BT_SERVER", "Erreur lors de l'ouverture du serveur", e);
            }
            serverSocket = tmp;
        }

        /**
         * Boucle principale du thread. Attend une connexion client.
         * Une fois la connexion etablie, ferme le serveur pour ne plus accepter d'autres clients.
         */
        @Override
        public void run () {
            BluetoothSocket socket = null ;
            // On boucle tant qu'il n'y a aucune connexion
            while(true){
                try {
                    // On attend une connexion
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e("BT_SERVER", "Erreur lors de l'acceptation de la connexion", e);
                    break;
                }

                if(socket != null){
                    manageConnectedSocket(socket);
                    // On ferme le serveur socket et on communique a travers le socket
                    try {
                        serverSocket.close();
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    break;
                }

            }
        }
    }

}


