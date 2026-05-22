package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MonitoringActivity extends AppCompatActivity {

    private LinearLayout deviceContainer;
    private static final String URL = "http://happyresto.enseeiht.fr/smartHouse/api/v1/devices/1";
    private static final String TAG = "MonitoringActivity";

    //Utilisé pour le rafraichissement de la page
    private Handler refreshHandler = new Handler();
    private Runnable runnableCode;
    //Fil de communication bleuthooth
    private BluetoothCommunicationThread btThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //  relais entre  java et XML
        setContentView(R.layout.activity_monitoring); 

        deviceContainer = findViewById(R.id.lla);

        // 1. Initialiser le Handler Bluetooth pour recevoir les requêtes du Client (
        Handler btHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == BluetoothCommunicationThread.MESSAGE_READ) {
                    byte[] readBuf = (byte[]) msg.obj;
                    // On convertit les octets reçus en String (ID de l'appareil)
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.d(TAG, "Ordre reçu du client pour le device ID : " + readMessage);

                    try {
                        int deviceId = Integer.parseInt(readMessage.trim());
                        // Le serveur exécute l'action demandée par le client
                        sendPostRequest(deviceId);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Message Bluetooth invalide reçu : " + readMessage);
                    }
                }
            }
        };

        // 2. Lier le thread Bluetooth à cette activité avant le handler etait nul on lui donne le vrai handler pour qu'il sache ou envoyer
        btThread = BluetoothCommunicationThread.getInstance();
        if (btThread != null) {
            btThread.setHandler(btHandler);
        }

        // 3. Tâche de rafraîchissement régulier (Volley)
        runnableCode = new Runnable() {
            @Override
            public void run() {
                //reactualise la liste des appareils
                refreshDevices();
                //toutes les 10s
                refreshHandler.postDelayed(this, 10000);
            }
        };
    }

    private Response.Listener<JSONArray> requestSuccessListener() {
        return response -> {
            Log.d(TAG, response.toString());

            // ON ENVOI DES DONNÉES AU CLIENT VIA BLUETOOTH
            if (btThread != null) {
                btThread.write(response.toString().getBytes());
            }
            //récuperation des données
            for (int i = 0; i < response.length(); i++) {
                try {
                    JSONObject device = response.getJSONObject(i);
                    String brand    = device.getString("BRAND");
                    String model    = device.getString("MODEL");
                    String name     = device.getString("NAME");
                    int autonomy    = device.getInt("AUTONOMY");
                    int state       = device.getInt("STATE");
                    String data     = device.optString("DATA", "");
                    int deviceId    = device.getInt("ID");

                    boolean on = (state == 1);
                    String donne = "";
                    if (autonomy != -1) donne += "Autonomy : " + autonomy + "% ";
                    if (!data.isEmpty()) donne += "Data : " + data;

                    View deviceView = createDeviceView(name, brand + " " + model, donne, on);
                    deviceContainer.addView(deviceView);

                } catch (JSONException e) {
                    Log.e(TAG, "Erreur parsing JSON : " + e.getMessage());
                }
            }
        };
    }

    private Response.ErrorListener requestErrorListener() {
        return error -> Log.e(TAG, "Erreur Volley : " + error.getMessage());
    }

    private void refreshDevices() {
        if (deviceContainer == null) return;
        deviceContainer.removeAllViews(); // vide la liste des appareils
        RequestQueue queue = Volley.newRequestQueue(this); //pour que les requêtes s'execute dans l'ordre
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, //juste lire
                URL,
                null,
                requestSuccessListener(),
                requestErrorListener()
        );
        queue.add(request); //on ajoute la requete a la liste
    }

    // Requête POST exécutée par le Serveur suite à un ordre du Client
    private void sendPostRequest(int deviceId) {
        String postUrl = "http://happyresto.enseeiht.fr/smartHouse/api/v1/devices/";
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest sr = new StringRequest(Request.Method.POST, postUrl,
                response -> {
                    Log.d(TAG, "POST OK : " + response);
                    // On force un rafraîchissement immédiat pour mettre à jour l'affichage et le client
                    refreshDevices();
                },
                error -> Log.e(TAG, "POST erreur : " + error.getMessage())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("deviceId", String.valueOf(deviceId));//id
                params.put("houseId", "1"); // Ton numéro de maison
                params.put("action", "turnOnOff");//action
                return params;
            }

            @Override
            public Map<String, String> getHeaders()  {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");//application/x-www-form-urlencoded est le format standard d'Internet
                return headers;
            }
        };
        queue.add(sr);
    }

    public RelativeLayout createDeviceView(String name, String info, String donne, boolean on) {
        RelativeLayout layout = new RelativeLayout(this);
        layout.setPadding(0, 10, 0, 10);

        TextView tvInfo = new TextView(this);
        tvInfo.setId(View.generateViewId());
        tvInfo.setText("[" + info + "]");
        tvInfo.setTextSize(18);
        RelativeLayout.LayoutParams paramsInfo = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsInfo.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layout.addView(tvInfo, paramsInfo);

        TextView tvDonne = new TextView(this);
        tvDonne.setId(View.generateViewId());
        tvDonne.setText(donne);
        tvDonne.setTextSize(13);
        RelativeLayout.LayoutParams paramsDonne = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsDonne.addRule(RelativeLayout.BELOW, tvInfo.getId());
        layout.addView(tvDonne, paramsDonne);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(18);
        RelativeLayout.LayoutParams paramsName = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsName.addRule(RelativeLayout.RIGHT_OF, tvInfo.getId());
        paramsName.setMargins(10, 0, 0, 0);
        layout.addView(tvName, paramsName);

        Button btnState = new Button(this);
        btnState.setText(on ? "ON" : "OFF");

        //BOUTON NON-PRESSABLE SUR LE SERVEUR
        btnState.setEnabled(false);

        RelativeLayout.LayoutParams paramsButton = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsButton.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        paramsButton.addRule(RelativeLayout.CENTER_VERTICAL);
        layout.addView(btnState, paramsButton);

        return layout;
    }

    //Quand on quitte l'ecran ca met en pause le rafraichissement
    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(runnableCode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.post(runnableCode);
    }
}
