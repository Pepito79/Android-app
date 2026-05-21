package com.example.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class ClientActivity extends AppCompatActivity {
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-82eb-11ed-a1eb-0242ac120002");
    private BluetoothAdapter adapter;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client); // Utilise votre ListView device_list

        adapter = BluetoothAdapter.getDefaultAdapter();
        ListView listV = findViewById(R.id.device_list);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listV.setAdapter(arrayAdapter);

        // Récupération des devices appairés (Section 2.1)
        for (BluetoothDevice d : adapter.getBondedDevices()) {
            devices.add(d);
            arrayAdapter.add(d.getName() + "\n" + d.getAddress());
        }

        listV.setOnItemClickListener((p, v, pos, id) -> connect(devices.get(pos)));
    }

    private void connect(BluetoothDevice device) {
        new Thread(() -> {
            try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();

                // On prépare la com sans Handler pour l'instant (on le fera dans SecondActivity)
                BluetoothCommunicationManager.startCommunication(socket, null);

                runOnUiThread(() -> {
                    startActivity(new Intent(this, SecondActivity.class));
                    finish(); // Ferme le sélecteur
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Connexion impossible", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}