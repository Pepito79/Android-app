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

    public static synchronized BluetoothCommunicationThread getInstance(BluetoothSocket socket, Handler handler) {
        if (instance == null && socket != null) {
            instance = new BluetoothCommunicationThread(socket, handler);
        }
        return instance;
    }

    public static synchronized BluetoothCommunicationThread getInstance() {
        return instance;
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        while (true) {
            try {
                //il se met en pause et attend
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

    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            Log.e("BT_COMM", "Erreur envoi", e);
        }
    }
}
