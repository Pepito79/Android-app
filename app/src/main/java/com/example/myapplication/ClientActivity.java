package com.example.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class ClientActivity extends AppCompatActivity {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    public static BluetoothSocket globalSocket;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_waiting);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Recherche du serveur dans les paired devices

        BluetoothDevice serverDevice = null;
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size()> 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("IoTCommanderServer")) {
                    serverDevice = device;
                    break;
                }
            }
        }

        // On tente de se connecter au seveur
        if (serverDevice != null) {
            ConnectionThread connectThread = new ConnectionThread(serverDevice);
            connectThread.start();
        }


    }
    private void manageConnectedSocket(BluetoothSocket socket) {
        globalSocket = socket;
        runOnUiThread(() -> {
            // Direction l'écran de contrôle client
            Intent intent = new Intent(this, ControlActivity.class);
            startActivity(intent);
        });
    }

    // Thread de connexion
    public  class ConnectionThread extends Thread {
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

        public void run(){

            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
            }catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("BT_CLIENT", "Erreur lors de la fermeture du socket", e1);
                }
                return;
            }
            manageConnectedSocket(socket);
        }
        }

    }
