package com.example.myapplication;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothCommunicationManager {
    private static ConnectedThread connectedThread;

    /** initialise la communication après la connexion réussie */
    public static void startCommunication(BluetoothSocket socket, Handler handler) {
        if (connectedThread != null) connectedThread.cancel();
        connectedThread = new ConnectedThread(socket, handler);
        connectedThread.start();
    }

    /** Permet de changer d'Activity sans couper le lien Bluetooth */
    public static void updateHandler(Handler newHandler) {
        if (connectedThread != null) connectedThread.setHandler(newHandler);
    }

    /** Envoie une chaîne de caractères vers l'autre appareil */
    public static void send(String message) {
        if (connectedThread != null) connectedThread.write(message.getBytes());
    }

    private static class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inStream;
        private final OutputStream outStream;
        private Handler handler;

        public ConnectedThread(BluetoothSocket socket, Handler handler) {
            this.socket = socket;
            this.handler = handler;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { Log.e("BT_MANAGER", "Erreur flux", e); }
            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void setHandler(Handler handler) { this.handler = handler; }

        public void run() {
            byte[] buffer = new byte[8192]; // Buffer large pour les JSON de l'API
            int bytes;
            while (true) {
                try {
                    bytes = inStream.read(buffer);
                    if (handler != null) {
                        handler.obtainMessage(0, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) { break; }
            }
        }

        public void write(byte[] bytes) {
            try { outStream.write(bytes); } catch (IOException e) { Log.e("BT_MANAGER", "Erreur envoi"); }
        }

        public void cancel() {
            try { socket.close(); } catch (IOException e) { }
        }
    }
}