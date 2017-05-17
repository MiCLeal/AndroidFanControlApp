package com.fmu.bcc2k15.fancontrol.androidfancontrolapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.fmu.bcc2k15.fancontrol.androidfancontrolapp.utils.Constants;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Michael Anthony on 15/05/2017.
 */

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 *
 * based on http://developer.android.com/resources/samples/BluetoothChat/index.html
 *
 */
public class BluetoothConnection {

    private static final String TAG = "BluetoothConnection";

    // Campos
    private final BluetoothAdapter btAdpter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private Context mContext;
    private boolean isDuplexConnection = true;

    /**
     * Construtor. Prepara uma nova sessão para BluetoothConnection.
     * @param context O context UI da Activity.
     * @param handler Um Handler para mandar menssagem de volta para a UI da Activity.
     */
    public BluetoothConnection(Context context, Handler handler) {
        btAdpter = BluetoothAdapter.getDefaultAdapter();
        mState = Constants.STATE_NONE;
        mHandler = handler;
        mContext = context;
    }

    /**
     * Define o estado atual da conexão.
     * @param state Um inteiro definindo o estado da conexão.
     */
    private synchronized void setState(int state) {
        mState = state;

        // Dê um novo estado para o Handler então a Activity UI pode atualizar.
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Retorna o estado atual da conexão.
     * @return Estado atual da conexão.
     */
    private synchronized int getState() {
        return mState;
    }

    /**
     * Quando a conexão está só recebendo dados do dispositivo, não nas duas direções
     * o Thread não deve "escutar" o ImputStream.
     * @return
     */
    public boolean isDuplexConnection() {
        return isDuplexConnection;
    }

    /**
     * Define se a conexão é full duplex ou não.
     * @param isDuplexConnection true para full duplex e false para não.
     */
    public void setDuplexConnection(boolean isDuplexConnection) {
        this.isDuplexConnection = isDuplexConnection;
    }

    public synchronized void start() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(Constants.STATE_LISTEN);

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device) {

        if (mState == Constants.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(Constants.STATE_CONNECTING);
    }


    private class AcceptThread extends Thread {

        // ServerSocket Local
        private final BluetoothServerSocket mmServerSocket;

        public void cancel() {
            try {
                if (mmServerSocket != null) {
                    mmServerSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: cancel() Exception: " + e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread {

        private BluetoothDevice mDevice;

        ConnectThread(BluetoothDevice device) {
            mDevice = device;
        }

        public void cancel() {

        }
    }

    private class ConnectedThread extends Thread {

        public void cancel() {

        }
    }
}