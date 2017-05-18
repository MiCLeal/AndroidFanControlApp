package com.fmu.bcc2k15.fancontrol.androidfancontrolapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.bq.robotic.droid2ino.BluetoothConnection;

public class BluetoothDevices extends AppCompatActivity {

    /**
     * Tag para o Log da Activity BluetoothDevices.
     */
    private static final String TAG = "BluetoothDevices";
    private BluetoothAdapter mBtAdapter;
    private ListView pairedDevicesLV;
    private ListView newDevicesLV;
    private ArrayAdapter<String> pairedDevicesAdapter;
    private ArrayAdapter<String> newDevicesAdapter;
    private View view;
    private BluetoothConnection connection;

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
        pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        for (BluetoothDevice device : mBtAdapter.getBondedDevices()) {
            pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
        }
        newDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        fillPairedListView(pairedDevicesAdapter);

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
                        Log.d(TAG, "PairedList: On Click");
                        String item = (String) parent.getAdapter().getItem(position);
                        BtConnect(item.substring(0, item.indexOf("\n")), item
                                .substring(item.indexOf("\n") + 1, item.length()));
//                    Snackbar.make(view, "Connected to " + pairedDevicesList.get(position)
//                            .substring(0, pairedDevicesList.get(position).indexOf("\n"))
//                            + " (" + pairedDevicesList.get(position)
//                            .substring(pairedDevicesList.get(position).indexOf("\n")
//                                    + 1, pairedDevicesList.get(position).length()) + ")",
//                            Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    } catch (Exception e) {
                        Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        Log.d(TAG, "PairedList: On Click: " + e.getMessage());
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
                        String item = (String) parent.getAdapter().getItem(position);
                        BtConnect(item.substring(0, item.indexOf("\n")), item
                                .substring(item.indexOf("\n") + 1, item.length()));
                    } catch (Exception e) {
                        Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        e.printStackTrace();
                    }
                }
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void fillPairedListView(ArrayAdapter<String> devices) {

        devices.notifyDataSetChanged();
        pairedDevicesLV.setAdapter(devices);
    }

    private void BtConnect(String name, String macAddress) {
        Intent rMain = new Intent();
        rMain.putExtra("devName", name);
        rMain.putExtra("devAddress", macAddress);
        setResult(RESULT_OK, rMain);
        finish();
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

    }

    public static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        }
    };
}
