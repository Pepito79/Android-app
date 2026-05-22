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

    // Definition de l'ID qui sera utilisé par le client pour se connecter au serveur
    private  static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static  final String NAME = "IoTCommanderServer";
    public static BluetoothSocket globalSocket;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_waiting);

        // Lancement du serveur
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
    }


    private void manageConnectedSocket(BluetoothSocket socket) {

        // On sauvegarde le socket
        globalSocket = socket;
        ConnectedThread myConnectedThread = new ConnectedThread(socket);
        myConnectedThread.start();
        runOnUiThread(() -> {
            Intent intent = new Intent(ServeurActivity.this , MonitoringActivity.class);
            startActivity(intent);
        });
    }


    // Création d'un thread pour ne pas bloquer l'UI lors de l'attente de connextion
    public class AcceptThread extends Thread {
        // Socket serveur qui permettera d'écouter les connexions entrantes
        private final BluetoothServerSocket serverSocket;
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


