package com.fmu.bcc2k15.fancontrol.androidfancontrolapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.LogRecord;

public class BluetoothDevices extends AppCompatActivity {

    /**
     * Tag para o Log da Activity BluetoothDevices.
     */
    private static final String TAG = "BluetoothDevices";
    private BluetoothAdapter mBtAdapter;
    private BluetoothSocket mBtSocket;
    private ListView pairedDevicesLV;
    private ListView newDevicesLV;
    private ArrayList<String> pairedDevicesList;
    private ArrayAdapter<String> newDevicesAdapter;
    private static View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paires_filter);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        view = fab.findViewById(R.id.fab);
        pairedDevicesLV = (ListView) findViewById(R.id.pairedListVIew);
        newDevicesLV = (ListView) findViewById(R.id.newDevicesListView);
        Bundle bn = getIntent().getExtras();
        pairedDevicesList = bn.getStringArrayList("paired");
        newDevicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        fillPairedListView(pairedDevicesList);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isDiscovering()) {
                    Log.d(TAG, "FAB: DISCOVERABLE: fab click for discovery");

                    if (mBtAdapter.isDiscovering()) {
                        mBtAdapter.cancelDiscovery();
                    }

                    mBtAdapter.startDiscovery();
                    Snackbar.make(view, "Discovering Devices!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                }
            }
        });

        newDevicesLV.setAdapter(newDevicesAdapter);

        pairedDevicesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (pairedDevicesLV != null) {
                    try {
                        BtConnect(pairedDevicesList.get(position)
                                .substring(pairedDevicesList.get(position).indexOf("\n")
                                        + 1, pairedDevicesList.get(position).length()));
                    Snackbar.make(view, "Connected to " + pairedDevicesList.get(position)
                            .substring(0, pairedDevicesList.get(position).indexOf("\n"))
                            + " (" + pairedDevicesList.get(position)
                            .substring(pairedDevicesList.get(position).indexOf("\n")
                                    + 1, pairedDevicesList.get(position).length()) + ")",
                            Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    } catch (IOException e) {
                        Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        e.printStackTrace();
                    }
                }
            }
        });

        newDevicesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (newDevicesLV != null) {
                    // Comando para ser executado quando um item da lista for clicado.
                    try {
                        BtConnect(newDevicesAdapter.getItem(position).substring(newDevicesAdapter
                                .getItem(position).indexOf("\n")
                                + 1, newDevicesAdapter.getItem(position).length()));
                    } catch (IOException e) {
                        Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        e.printStackTrace();
                    }
                }
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void fillPairedListView(ArrayList<String> devices) {
        ArrayAdapter<String> btNameAdapter = new ArrayAdapter<String>(getBaseContext(),
                android.R.layout.simple_list_item_1, devices);
        btNameAdapter.notifyDataSetChanged();
        pairedDevicesLV.setAdapter(btNameAdapter);
    }

    private void BtConnect(String MacAddress) throws IOException {
        BluetoothDevice nmDevice;

        nmDevice = mBtAdapter.getRemoteDevice(MacAddress);

        if (nmDevice == null){
            throw new IOException();
        } else {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        this.unregisterReceiver(receiver);
    }


    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (newDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    newDevicesAdapter.add(newDevice.getName() + "\n" + newDevice.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (newDevicesAdapter.getCount() == 0) {
                    String noDevice = getResources().getText(R.string.no_devices).toString();
                    newDevicesAdapter.add(noDevice);
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == BLUETOOTH_DICOVABLE_REQUEST) {
////            if (resultCode == BLUETOOTH_DISCOVABLE_TIME) {
//                mBtAdapter.startDiscovery();
//                Snackbar.make(view, "Discovering Devices!", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            } else {
//                Snackbar.make(view, "Discovering Devices Failed!", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        }
    }

    public static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bn = msg.getData();
            byte[] data = bn.getByteArray("data");
            String dataString = new String(data);

            if (dataString.equals("---N")) {
                Snackbar.make(view, "Ocorreu um erro durante a conexão.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            } else if (dataString.equals("---S")) {
                Snackbar.make(view, "Conectado.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            } else {

            }
        }
    };
}
