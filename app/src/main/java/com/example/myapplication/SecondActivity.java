package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;

public class SecondActivity extends AppCompatActivity {
    private LinearLayout deviceContainer;

    // Réception des données du serveur
    private final Handler clientHandler = new Handler(Looper.getMainLooper(), msg -> {
        byte[] readBuf = (byte[]) msg.obj;
        String json = new String(readBuf, 0, msg.arg1);
        try {
            updateUI(new JSONArray(json));
        } catch (Exception e) { e.printStackTrace(); }
        return true;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second); // Utilise votre ScrollView/device_list_container

        deviceContainer = findViewById(R.id.device_list_container);
        findViewById(R.id.btn_retour).setOnClickListener(v -> finish());

        // On branche le flux Bluetooth sur cette activité
        BluetoothCommunicationManager.updateHandler(clientHandler);
    }

    private void updateUI(JSONArray devices) throws Exception {
        deviceContainer.removeAllViews();
        for (int i = 0; i < devices.length(); i++) {
            ajouterDevice(devices.getJSONObject(i));
        }
    }

    private void ajouterDevice(JSONObject dev) throws Exception {
        int id = dev.getInt("ID");
        int state = dev.getInt("STATE");

        // Création dynamique de la ligne
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(20, 20, 20, 20);
        row.setBackgroundColor(Color.parseColor("#EEEEEE"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 5, 0, 5);
        row.setLayoutParams(params);

        TextView tv = new TextView(this);
        tv.setText(dev.getString("NAME") + "\n" + dev.getString("BRAND"));
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        Button btn = new Button(this);
        btn.setText(state == 1 ? "ON" : "OFF");
        btn.setTextColor(state == 1 ? Color.parseColor("#008577") : Color.RED);

        // Envoi Bluetooth de l'ordre
        btn.setOnClickListener(v -> BluetoothCommunicationManager.send("TOGGLE:" + id));

        row.addView(tv);
        row.addView(btn);
        deviceContainer.addView(row);
    }
}