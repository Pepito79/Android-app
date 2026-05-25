package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

public class ControlActivity extends AppCompatActivity {

    private LinearLayout controlContainer;
    private BluetoothCommunicationThread btThread;
    private static final String TAG = "ControlActivity";
    private StringBuilder jsonBuilder = new StringBuilder();

    /**
     * Initialise l'interface de controle et configure le recepteur de messages.
     * Recupere l'instance unique du thread de communication et lui injecte un nouveau Handler.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        controlContainer = findViewById(R.id.controlContainer);
        Handler btHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == BluetoothCommunicationThread.MESSAGE_READ) {
                    byte[] readBuf = (byte[]) msg.obj;
                    //msg.arg1 contient le nombre d'octets réellement reçus pour ne lire que la partie utile du buffer
                    String chun = new String(readBuf, 0, msg.arg1);
                    jsonBuilder.append(chun);
                    String currentText = jsonBuilder.toString().trim();

                    //Si le message ne commence pas par '[', on nettoie tout
                    if (!currentText.isEmpty() && !currentText.startsWith("[")) {
                        jsonBuilder.setLength(0);
                        return;
                    }

                    //On ne tente de parser que si on voit ] à la fin
                    if (currentText.endsWith("]")) {
                        try {
                            // On vérifie si c'est un JSON valide
                            new JSONArray(currentText);
                            Log.d(TAG, "JSON complet reçu !");
                            updateUI(currentText);
                            jsonBuilder.setLength(0); // On vide pour la prochaine fois
                        } catch (JSONException e) {
                            // Pas encore complet ou mal formé, on attend la suite sans vider
                        }
                    }
                }
            }
        };
        btThread = BluetoothCommunicationThread.getInstance();
        if (btThread != null) {
            btThread.setHandler(btHandler);
        } else {
            Toast.makeText(this, "Connexion Bluetooth perdue", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Transforme une chaine JSON en composants graphiques.
     * Nettoie le conteneur et utilise un LayoutInflater pour generer dynamiquement
     * une vue pour chaque appareil connecte.
     * @param jsonString La chaine de caracteres au format JSONArray.
     */
    private void updateUI(String jsonString) {
        try {
            JSONArray devices = new JSONArray(jsonString);
            //efface car actualisation sinon risque empilement plusieurs appareils
            controlContainer.removeAllViews();

            // On prépare la vue XML
            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < devices.length(); i++) {
                JSONObject obj = devices.getJSONObject(i);

                String id = obj.getString("ID");
                String name = obj.getString("NAME");
                String brand = obj.getString("BRAND");
                String model = obj.getString("MODEL");
                int state = obj.getInt("STATE");


                int autonomy = obj.optInt("AUTONOMY", -1);
                String data = obj.optString("DATA", "");

                // On affiche le fichier XML item_device.xml
                View deviceView = inflater.inflate(R.layout.item_device, controlContainer, false);

                // On relie les éléments du XML
                TextView tvBrandModel = deviceView.findViewById(R.id.tv_device_brand_model);
                TextView tvName = deviceView.findViewById(R.id.tv_device_name);
                TextView tvInfo = deviceView.findViewById(R.id.tv_device_info);
                Button btn = deviceView.findViewById(R.id.btn_device_toggle);


                tvBrandModel.setText("[" + brand + "-" + model + "]");
                tvName.setText(name);
                tvInfo.setText(autonomy != -1 ? "Autonomy : " + autonomy + "%" : "Data : " + data);

                btn.setText(state == 1 ? "ON" : "OFF");
                btn.setBackgroundColor(state == 1 ? Color.parseColor("#B0BEC5") : Color.LTGRAY);

                // Action du bouton
                btn.setOnClickListener(v -> {
                    if (btThread != null) {
                        // On envoie l'ID (Ta logique compatible avec le serveur)
                        btThread.write(String.valueOf(id).getBytes());
                        Toast.makeText(this, "Envoi ID: " + id, Toast.LENGTH_SHORT).show();
                    }
                });

                // On ajoute le tout sur l'écran
                controlContainer.addView(deviceView);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erreur UI : " + e.getMessage());
        }
    }

    /**
     * Gere la destruction de l'activite.
     * Ferme proprement le socket Bluetooth pour liberer les ressources du telephone.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // Fermeture du socket global
            if (ClientActivity.globalSocket != null) {
                ClientActivity.globalSocket.close();
            }
        } catch (IOException e) {
            Log.e("BT_CLIENT", "Erreur lors de la fermeture du socket");
        }
        Log.d("BT_CLIENT", "Nettoyage ControlActivity terminé");
    }
}
