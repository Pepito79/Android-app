package com.example.myapplication;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {
    private final BluetoothSocket socket;
    private final InputStream inStream;
    private final OutputStream outStream;

    // Référence vers l'activité serveur (optionnelle)
    private MonitoringActivity serverActivity;

    // Constructeur pour le Client (sans activité serveur)
    public ConnectedThread(BluetoothSocket s) {
        this(s, null);
    }

    // Constructeur pour le Serveur (avec référence à MonitoringActivity)
    public ConnectedThread(BluetoothSocket s, MonitoringActivity activity) {
        this.socket = s;
        this.serverActivity = activity;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e("BT_CONNECTED", "Erreur lors de la création des flux", e);
        }

        inStream = tmpIn;
        outStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[4096]; // Espace de stockage pour le message
        int bytes;

        // On écoute en permanence le flux d'entrée
        while (true) {
            try {
                // Lecture des données entrantes (méthode bloquante)
                bytes = inStream.read(buffer);
                if (bytes > 0) {
                    String incomingMessage = new String(buffer, 0, bytes).trim();
                    Log.d("BT_CONNECTED", "Message reçu : " + incomingMessage);

                    // Si on est sur le serveur et qu'on reçoit un TOGGLE
                    if (serverActivity != null && incomingMessage.startsWith("TOGGLE:")) {
                        serverActivity.handleClientAction(incomingMessage);
                    }
                }

            } catch (IOException e) {
                Log.e("BT_CONNECTED", "Connexion perdue", e);
                break;
            }
        }
    }

    // Méthode pour envoyer des données au partenaire distant
    public void write(String message) {
        // On ajoute un caractère de fin pour aider le buffer du destinataire
        String messageWithEnding = message.endsWith("\n") ? message : message + "\n";
        byte[] bytes = messageWithEnding.getBytes();
        try {
            outStream.write(bytes);
            outStream.flush(); // On force l'envoi
        } catch (IOException e) {
            Log.e("BT_CONNECTED", "Erreur lors de l'envoi", e);
        }
    }
}
