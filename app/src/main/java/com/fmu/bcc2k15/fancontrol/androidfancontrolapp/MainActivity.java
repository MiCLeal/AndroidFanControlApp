package com.fmu.bcc2k15.fancontrol.androidfancontrolapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    /**
     * Tag para o Log da MainActivity
     */
    private static final String TAG = "MainActivity";
    private static final int BLUETOOTH_REQUEST = 1;
    private static final int SELECT_BLUETOOTH_REQUEST = 2;
    private static final int SHOW_BLUETOOTH_LIST = 3;

    /**
     * ArrayList do tipo String com os nomes dos dispositivos pareados.
     */
    private Set<BluetoothDevice> pairedDevices;
    private View views;
    private BluetoothAdapter mBtAdapter;

    private CThread connect;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        SeekBar speedSeekBar = (SeekBar) findViewById(R.id.speedSeekBar);
        TextView txtTemperature = (TextView) findViewById(R.id.txtTemperature);
        TextView txtConnectedTo = (TextView) findViewById(R.id.txtConnectedTo);
        setBluetoothAdapter();

        txtConnectedTo.setText(R.string.not_connected);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBtAdapter == null) {
                    Log.d(TAG, "BTADAPTER: Null");
                    Snackbar.make(v, "This Device don't support Bluetooth!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    if (!mBtAdapter.isEnabled()) {
                        Log.d(TAG, "mBtAdapter: Turned ON");
                        Intent btTurnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(btTurnOn, BLUETOOTH_REQUEST);
                    } else {
                        Log.d(TAG, "mBtAdapter: Turned OFF");
                        mBtAdapter.disable();
                        Snackbar.make(v, "Bluetooth turned OFF!", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }
            }
        });

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        views = fab.findViewById(R.id.fab);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Método para definir o adaptador Bluetooth padrão do dispositivo.
     */
    private void setBluetoothAdapter() {
        Log.d(TAG, "Definindo Adaptador Bluetooth padrão.");
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Método para o Menu de Navegaçõa.
     *
     * @param item Item selecionado
     * @return true para mostrar o item selecionado
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_bluetooth_devices) {
            // Comandos para executar quando o menu de Dispositivos for clicado.
            if (mBtAdapter != null) {
                if (mBtAdapter.isEnabled()) {
                    Log.d(TAG, "Navigation: BT Devices Clicked: Adapter status ON");

//                    pairedDevices = mBtAdapter.getBondedDevices();
//
//                    ArrayList<String> pairedAdapter = new ArrayList<String>();
//
//                    for (BluetoothDevice device : pairedDevices) {
//                        pairedAdapter.add(device.getName() + "\n" + device.getAddress());
//                    }
//
//                    Bundle bn = new Bundle();
//                    bn.putStringArrayList("paired", pairedAdapter);
                    Intent paired = new Intent(this, BluetoothDevices.class);
//                    paired.putExtras(bn);
                    startActivityForResult(paired, SELECT_BLUETOOTH_REQUEST);
                } else {
                    Log.d(TAG, "Navigation: BT Devices Clicked: Adapter status OFF");
                    Snackbar.make(views, "Your need turn Bluetooth ON", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            } else {
                Log.d(TAG, "Navigation: BTADAPTER: Null");
                Snackbar.make(views, "This Device don't support Bluetooth!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        } else if (id == R.id.nav_temperature) {
            Log.d(TAG, "Navigation: Temperature Clicked");
            Intent intentTemperature = new Intent("temperature_filter");
            startActivity(intentTemperature);
        } else if (id == R.id.nav_manage) {
            Log.d(TAG, "Navigation: Manage Clicked");
        } else if (id == R.id.nav_share) {
            Log.d(TAG, "Navigation: Share Clicked");
        } else if (id == R.id.nav_send) {
            Log.d(TAG, "Navigation: Send Clicked");
        }

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BLUETOOTH_REQUEST) {
            if (resultCode == RESULT_OK) {
                pairedDevices = mBtAdapter.getBondedDevices();
                Snackbar.make(views, "Bluetooth turned ON!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            } else {
                Snackbar.make(views, "Bluetooth turned ON failed!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }

        if (requestCode == SELECT_BLUETOOTH_REQUEST) {
            if (resultCode == RESULT_OK) {
                try {
                    connect = new CThread(data.getStringExtra("devAddress"));
                    connect.start();
                } catch (Exception e) {
                    Log.d(TAG, "Connect: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
