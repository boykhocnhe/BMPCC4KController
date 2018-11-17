/**
 APP: BMPCC4K Control
 Author: André Rodrigues
 Company: Media Environment
 Function: Bluetooth controller for the Blackmagic Pocket Cinema Camera 4K
 */

package br.com.mediaenvironment.bmpcc4kcontrol.bmpcc4kcontroller;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;

    public TextView tv_status;
    public ImageView iv_bluetooth;
    public Button bt_turnOn, bt_turnOff, bt_discoverable, bt_discover, bt_startConnection, bt_rec;
    public ListView lv_newDevices, lv_paired;
    public BluetoothAdapter mBTAdapter;
    public ArrayList<BluetoothDevice> mPairedDevices = new ArrayList<>();
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter, mPairedListAdapter;


    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBTAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBTAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };




    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());

                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lv_newDevices.setAdapter(mDeviceListAdapter);

                mPairedDevices = new ArrayList<>(mBTAdapter.getBondedDevices());
                mPairedListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mPairedDevices);
                lv_paired.setAdapter(mPairedListAdapter);
            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
        //mBluetoothAdapter.cancelDiscovery();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_status               = findViewById(R.id.tv_status);
        iv_bluetooth            = findViewById(R.id.iv_bluetooth);
        bt_turnOn               = findViewById(R.id.bt_turnOn);
        bt_turnOff              = findViewById(R.id.bt_turnOff);
        bt_discoverable         = findViewById(R.id.bt_discoverable);
        bt_discover             = findViewById(R.id.bt_discover);
        bt_startConnection      = findViewById(R.id.bt_startConnection);
        bt_rec                  = findViewById(R.id.bt_rec);
        lv_paired               = findViewById(R.id.lv_pairedDevices);
        lv_newDevices           = findViewById(R.id.lv_newDevices);
        mBTDevices              = new ArrayList<>();

        //Broadcast when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        //adapter
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        lv_newDevices.setOnItemClickListener(MainActivity.this);
        lv_paired.setOnItemClickListener(MainActivity.this);


        /** Checking if the device has a bluetooth device and it's state */
        //check if bluetooth is available or not
        if (mBTAdapter == null){
            tv_status.setText("Bluetooth is not available");
        }
        else {
            tv_status.setText("Bluetooth is available");
        }

        //set image according to bluetooth status(on/off)
        if (mBTAdapter.isEnabled()){
            iv_bluetooth.setImageResource(R.drawable.ic_action_bton);
        }
        else {
            iv_bluetooth.setImageResource(R.drawable.ic_action_btoff);
        }

        //Turn On BT Function
        bt_turnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBTAdapter.isEnabled()){
                    Log.d(TAG, "Turning on BT");

                    //intent to on bluetooth
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);

                    IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                    registerReceiver(mBroadcastReceiver1, BTIntent);
                }
                else {
                    Log.d(TAG, "Bluetooth is already on");
                }
            }
        });

        //Discover BT Function
        bt_discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

                if(mBTAdapter.isDiscovering()){
                    mBTAdapter.cancelDiscovery();
                    Log.d(TAG, "btnDiscover: Canceling discovery.");

                    //check BT permissions in manifest
                    checkBTPermissions();

                    mBTAdapter.startDiscovery();
                    IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
                }
                if(!mBTAdapter.isDiscovering()){

                    //check BT permissions in manifest
                    checkBTPermissions();

                    mBTAdapter.startDiscovery();
                    IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
                }
            }
        });

        //Discoverable BT Function
        bt_discoverable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");

                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);

                IntentFilter intentFilter = new IntentFilter(mBTAdapter.ACTION_SCAN_MODE_CHANGED);
                registerReceiver(mBroadcastReceiver2,intentFilter);
            }
        });

        //off btn click
        bt_turnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBTAdapter.isEnabled()){
                    mBTAdapter.disable();
                    iv_bluetooth.setImageResource(R.drawable.ic_action_btoff);
                }
                else {
                    Log.d(TAG, "Bluetooth is already off");
                }
            }
        });
    }



    public void enableDisableBT(){
        if(mBTAdapter == null){
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if(!mBTAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if(mBTAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBTAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }


    public void btnEnableDisable_Discoverable(View view) {
        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(mBTAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2,intentFilter);

    }

    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBTAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if(!mBTAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions();

            mBTAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     *
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        if (adapterView.getId() == R.id.lv_pairedDevices){
            Log.d(TAG, "adapter view id " + adapterView.getId());
            Log.d(TAG, "R ID " + R.id.lv_pairedDevices);

        }

        //first cancel discovery because its very memory intensive.
//        mBTAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device. ");


//        String deviceName = mBTDevices.get(i).getName();
//        String deviceAddress = mBTDevices.get(i).getAddress();
//
//        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
//        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with " + deviceName);

            if(mBTDevices.get(i).getBondState() == BluetoothDevice.BOND_BONDED){
                Log.d(TAG, "BONDED - Bond State: " + mBTDevices.get(i).getBondState());

                //Unbond device before bond again
                unpairDevice(mBTDevices.get(i));
            } else {
                Log.d(TAG, "NOT BONDED - Bond State: " + mBTDevices.get(i).getBondState());

                //Create a bond with the device
                mBTDevices.get(i).createBond();
            }

//            mBTDevices.get(i).createBond();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

            Log.d(TAG, "BOND REMOVED: " + device.getName());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Log.d(TAG, "CAN'T REMOVE BOND: " + device.getName());
        }
    }
}
