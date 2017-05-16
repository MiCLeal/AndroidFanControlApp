package com.fmu.bcc2k15.fancontrol.androidfancontrolapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Michael Anthony on 15/05/2017.
 */

public class CThread extends Thread {

    private static final String APP_NAME = "Fan Control App";
    private final String TAG = "CThread";
    private String btDevAddress = null;
    private boolean server;
    private boolean running = false;
    private UUID insecUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothServerSocket btServerSocket;
    private BluetoothSocket btSocket;

    CThread() {
        this.server = true;
    }

    CThread(String devAddress) {
        Log.d(TAG, "Client Connection");
        this.server = false;
        this.btDevAddress = devAddress;
    }

    public void run() {
        this.running = true;
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (this.server) {
            try {
                btServerSocket = btAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, insecUUID);
                btSocket = btServerSocket.accept();

                if (btSocket != null) {
                    btServerSocket.close();
                }
            } catch (IOException e) {
                Log.d(TAG, "Connect: Server: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            try {
                BluetoothDevice btDevice = btAdapter.getRemoteDevice(btDevAddress);
                btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(insecUUID);
                btAdapter.cancelDiscovery();

                if (btSocket != null) {
                    btSocket.connect();
                }
            } catch (IOException e) {
                Log.d(TAG, "Connect: Client: " + e.getMessage());
                e.printStackTrace();
            }
        }

    }
}
