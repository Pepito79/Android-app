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

    public ConnectedThread(BluetoothSocket s) {
        socket = s;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            // On récupère les flux d'entrée et de sortie du socket
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
                String incomingMessage = new String(buffer, 0, bytes);

                Log.d("BT_CONNECTED", "Message reçu : " + incomingMessage);

            } catch (IOException e) {
                Log.e("BT_CONNECTED", "Connexion perdue", e);
                break;
            }
        }
    }

    // Méthode pour envoyer des données au partenaire distant
    public void write(String message) {
        byte[] bytes = message.getBytes();
        try {
            outStream.write(bytes);
        } catch (IOException e) {
            Log.e("BT_CONNECTED", "Erreur lors de l'envoi", e);
        }
    }
}
