package com.example.myapplication;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class BluetoothCommunicationThread extends Thread {
    public static final int MESSAGE_READ = 1;
    private static BluetoothCommunicationThread instance;
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Handler mHandler;

    /**
     * Initialise les flux d'entree et de sortie a partir du socket Bluetooth.
     */
    private BluetoothCommunicationThread(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        mHandler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e("BT_COMM", "Erreur flux", e);
        }
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    /**
     * Cree ou recupere l'instance unique du thread de communication.
     * Le mot-cle synchronized garantit qu'un seul thread cree l'objet (Thread Safety).
     * @return L'instance unique de BluetoothCommunicationThread.
     */
    public static synchronized BluetoothCommunicationThread getInstance(BluetoothSocket socket, Handler handler) {
        if (instance == null && socket != null) {
            instance = new BluetoothCommunicationThread(socket, handler);
        }
        return instance;
    }

    /**
     * Recupere l'instance existante sans avoir besoin de fournir le socket.
     * Permet d'appeler write() depuis n'importe quelle activite.
     */
    public static synchronized BluetoothCommunicationThread getInstance() {
        return instance;
    }

    /**
     * Met a jour le Handler cible pour la mise a jour de l'UI.
     * Permet de rediriger les messages Bluetooth vers une nouvelle Activite.
     */
    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    /**
     *  Écoute en continu le flux entrant, copie les données reçues et les envoie à l'interface via le Handler.
     */
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        while (true) {
            try {
                //On se met en pause et on attend
                bytes = mmInStream.read(buffer);
                if (mHandler != null) {
                    //on envoie une COPIE du buffer pour éviter les collisions
                    byte[] copy = Arrays.copyOf(buffer, bytes);
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, copy).sendToTarget();
                }
            } catch (IOException e) {
                break;
            }
        }
    }

    /**
     * Envoie des donnees brutes (octets) vers l'appareil distant.
     * @param bytes Le tableau d'octets a transmettre via le flux de sortie.
     */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            Log.e("BT_COMM", "Erreur envoi", e);
        }
    }
}
