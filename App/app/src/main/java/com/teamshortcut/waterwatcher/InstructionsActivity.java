package com.teamshortcut.waterwatcher;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

public class InstructionsActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;

    /*Bluetooth Variables*/
    private ConnectionService connectionService; //The Android service that handles all Bluetooth communications
    public static String TARGET_ADDRESS; //MAC address of the micro:bit

    @SuppressLint("HandlerLeak") //TODO: remove
    private Handler messageHandler = new Handler() { //Handles messages from the ConnectionService, and is where BLE activity is handled
        @Override
        public void handleMessage(Message msg){
            Bundle bundle; //The data the message contains
            String serviceUUID = "";
            String characteristicUUID = "";
            String descriptorUUID = "";
            byte[] bytes = null;

            switch (msg.what){
                case ConnectionService.GATT_CONNECTED: //Once a device has connected...
                    connectionService.discoverServices(); //...discover its services
                    break;
                case ConnectionService.GATT_DISCONNECTED:
                    Toast.makeText(getApplicationContext(), "Device was disconnected.", Toast.LENGTH_LONG).show();
                    break;
                case ConnectionService.GATT_SERVICES_DISCOVERED:
                    bundle = msg.getData();
                    ArrayList<String> stringGattServices = bundle.getStringArrayList("GATT_SERVICES_LIST");

                    //Sometimes only generic services are initially found
                    if (stringGattServices == null || !stringGattServices.contains(ConnectionService.ACCELEROMETERSERVICE_SERVICE_UUID) || !stringGattServices.contains(ConnectionService.UARTSERVICE_SERVICE_UUID)){
                        //If the required services aren't found, refresh and retry service discovery
                        connectionService.refreshDeviceCache();
                        connectionService.discoverServices();
                    }
                    break;
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() { //The Android service for the ConnectionService class
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            connectionService = ((ConnectionService.LocalBinder) service).getService();
            connectionService.setActivityHandler(messageHandler); //Assigns messageHandler to handle all messages from this service

            if (!connectionService.isConnected()){
                if (connectionService.connect(TARGET_ADDRESS)){ //Try to connect to the BLE device chosen in the device selection activity
                    Log.d("BLE Connected", "Successfully connected from InstructionsActivity");
                }
                else{
                    Log.e("BLE Failed to connect", "Failed to connect from InstructionsActivity");
                    Toast.makeText(getApplicationContext(), "Failed to connect", Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connectionService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Sets up toolbar and navigation bar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.baseline_menu_white_24);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        MenuItem current = navigationView.getMenu().getItem(3);
        current.setChecked(true);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                menuItem.setChecked(true);
                Intent intent;

                switch (menuItem.getItemId()){
                    case R.id.device_select_drawer_item:
                        //Device Select activity will be launched, so disconnect from the current device and stop the Connection Service
                        connectionService.disconnect();
                        Intent connectionServiceIntent = new Intent(InstructionsActivity.this, ConnectionService.class);
                        stopService(connectionServiceIntent);

                        intent = new Intent(InstructionsActivity.this, DeviceSelectActivity.class);
                        startActivity(intent);
                        finish();
                        break;
                    case R.id.graph_drawer_item:
                        intent = new Intent(InstructionsActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                        break;
                    case R.id.settings_drawer_item:
                        intent = new Intent(InstructionsActivity.this, SettingsActivity.class);
                        startActivity(intent);
                        finish();
                        break;
                    case R.id.instructions_drawer_item:
                        drawerLayout.closeDrawers();
                        break;
                }

                return true;
            }
        });

        //Read intent data from previous activity
        Intent intent = getIntent();
        String address = intent.getStringExtra("DEVICEADDRESS"); //TODO: change key to constant
        Log.i("Intent Extras", "Address: "+address);

        TARGET_ADDRESS = address; //The MAC address of the device to connect to should be the chosen one passed from the device selection activity

        //Start the ConnectionService and BLE communications
        Intent connectionServiceIntent = new Intent(this, ConnectionService.class);
        bindService(connectionServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            unbindService(serviceConnection);
        }
        catch (Exception e){

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()){
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}