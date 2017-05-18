package com.bq.robotic.droid2ino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.bq.robotic.droid2ino.utils.Droid2InoConstants;
import com.fmu.bcc2k15.fancontrol.androidfancontrolapp.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
        mState = Droid2InoConstants.STATE_NONE;
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
        mHandler.obtainMessage(Droid2InoConstants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Retorna o estado atual da conexão.
     * @return Estado atual da conexão.
     */
    public synchronized int getState() {
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

        setState(Droid2InoConstants.STATE_LISTEN);

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device) {

        if (mState == Droid2InoConstants.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(Droid2InoConstants.STATE_CONNECTING);
    }

    public synchronized void connnected(BluetoothSocket socket, BluetoothDevice device) {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        Message msg = mHandler.obtainMessage(Droid2InoConstants.MESSAGE_DEVICE_NAME);
        Bundle bn = new Bundle();
        bn.putString(Droid2InoConstants.DEVICE_NAME, device.getName());
        msg.setData(bn);
        mHandler.sendMessage(msg);

        setState(Droid2InoConstants.STATE_CONNECTED);
    }

    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(Droid2InoConstants.STATE_NONE);
    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != Droid2InoConstants.STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }

        r.write(out);
    }

    public OutputStream getBTOutputStream() {
        ConnectedThread r;
        synchronized (this) {
            if (mState != Droid2InoConstants.STATE_CONNECTED) {
                return null;
            }

            r = mConnectedThread;
        }

        return r.getMmOutputStream();
    }

    public InputStream getBTInputStream() {
        ConnectedThread r;

        synchronized (this) {
            if (mState != Droid2InoConstants.STATE_CONNECTED) {
                return null;
            }

            r = mConnectedThread;
        }

        return r.getMmInputStream();
    }

    /**
     * Identifica que a tentativa de conexão falhou e notifica a UI.
     */
    public void connectionFailed() {
        Message msg = mHandler.obtainMessage(Droid2InoConstants.MESSAGE_TOAST);
        Bundle bn = new Bundle();
        bn.putString(Droid2InoConstants.TOAST, mContext.getString(R.string.connecting_bluetooth_error));
        msg.setData(bn);
        mHandler.sendMessage(msg);

        BluetoothConnection.this.start();
    }

    /**
     * Identifica que a conexão se perdeu e notifica a UI.
     */
    public void connectionLost() {
        Message msg = mHandler.obtainMessage(Droid2InoConstants.MESSAGE_TOAST);
        Bundle bn = new Bundle();
        bn.putString(Droid2InoConstants.TOAST, mContext.getString(R.string.connecting_bluetooth_lost));
        msg.setData(bn);
        mHandler.sendMessage(msg);

        BluetoothConnection.this.start();
    }

    private class AcceptThread extends Thread {

        // ServerSocket Local
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = btAdpter.listenUsingRfcommWithServiceRecord(Droid2InoConstants.SOCKET_NAME, Droid2InoConstants.MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket listen() failed", e);
            }

            mmServerSocket = tmp;
        }

        public void run() {
            setName(Droid2InoConstants.ACCEPT_THREAD_NAME);

            BluetoothSocket socket = null;

            if(mmServerSocket == null) {
                Log.e(TAG, "mmServerSocket in run of AcceptThread = null");
                return;
            }

            while (mState != Droid2InoConstants.STATE_CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket accept() failed", e);
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Some type error", e);
                    break;
                }

                if (socket != null){
                    synchronized (BluetoothConnection.this) {
                        switch (mState) {
                            case Droid2InoConstants.STATE_LISTEN:
                                case Droid2InoConstants.STATE_CONNECTING:
                                    connnected(socket, socket.getRemoteDevice());
                                    break;
                                case Droid2InoConstants.STATE_NONE:
                                    case Droid2InoConstants.STATE_CONNECTED:
                                        try {
                                            socket.close();
                                        } catch (IOException e) {
                                            Log.e(TAG, "Could not close unwanted socket", e);
                                        }
                                        break;
                        }
                    }
                }

            }
        }

        public void cancel() {
            try {
                if (mmServerSocket != null) {
                    mmServerSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket close() of server failed.", e);
            }
        }
    }

    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(Droid2InoConstants.MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed.", e);
            }

            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName(Droid2InoConstants.CONNECT_THREAD_NAME);

            btAdpter.cancelDiscovery();

            if (mmSocket == null) {
                Log.e(TAG, "mmSocket in run of ConnectThread = null");
                return;
            }

            try {
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "error connecting the socket in run method of connect thread: " + e);
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }

                connectionFailed();
                return;
            }

            synchronized (BluetoothConnection.this) {
                mConnectThread = null;
            }

            connnected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket close() of server failed.", e);
            }
        }
    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final StringBuffer readMessage;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "crete ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            readMessage = new StringBuffer();

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            int startIndex = -1;
            int endIndex = -1;
            String message;

            while (isDuplexConnection) {
                try {
                    bytes = mmInStream.read(buffer);

                    readMessage.append(new String(buffer, 0, bytes));

                    startIndex = readMessage.indexOf(Droid2InoConstants.START_READ_DELIMITER);
                    endIndex = readMessage.indexOf(Droid2InoConstants.END_READ_DELIMITER);

                    Log.d(TAG, "readMessage: " + readMessage);

                    if ((startIndex != -1) && (endIndex != -1) && (startIndex < endIndex)) {
                        message = readMessage.substring(startIndex + 2, endIndex);

                        mHandler.obtainMessage(Droid2InoConstants.MESSAGE_READ, bytes, -1, message).sendToTarget();

                        readMessage.delete(0, endIndex + 1);
                    }

                    startIndex = -1;
                    endIndex = -1;
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothConnection.this.start();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

        public void write(byte[] out) {
            try {
                mmOutStream.write(out);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Droid2InoConstants.MESSAGE_WRITE, -1, -1, out).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public InputStream getMmInputStream() {
            return mmInStream;
        }

        public OutputStream getMmOutputStream() {
            return mmOutStream;
        }
    }
}