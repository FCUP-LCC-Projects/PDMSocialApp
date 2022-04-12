package com.example.socialapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class ListDevicesActivity extends AppCompatActivity {
    private ListView listPairedDevices, listAvalDevices;
    private ArrayAdapter<String> adapterPaired, adapterAval;
    private Context context;
    private ProgressBar progressScan;
    private BluetoothAdapter bluetoothAdapter;


    private BroadcastReceiver deviceListener = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice intentDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (intentDevice.getBondState() != BluetoothDevice.BOND_BONDED)
                    adapterAval.add(intentDevice.getName() + "\n" + intentDevice.getAddress());
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                progressScan.setVisibility(View.GONE);
                if(adapterAval.getCount()==0)
                    Toast.makeText(context, "No new devices found.", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context, "Click on device to start chat", Toast.LENGTH_SHORT).show();

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_devices);
        context = this;

        initDevices();
    }

    @SuppressLint("MissingPermission")
    private void initDevices(){
        listAvalDevices = findViewById(R.id.list_aval_devices);
        listPairedDevices = findViewById(R.id.list_paired_devices);

        adapterPaired = new ArrayAdapter<String>(context, R.layout.device_list_item);
        adapterAval = new ArrayAdapter<String>(context, R.layout.device_list_item);

        listPairedDevices.setAdapter(adapterPaired);
        listAvalDevices.setAdapter(adapterAval);

        progressScan = findViewById(R.id.progress_devices);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        listAvalDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length()-17);

                Intent intent = new Intent();
                intent.putExtra("address", address);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        if(pairedDevices.size() >0 && pairedDevices != null){
            for(BluetoothDevice device : pairedDevices)
                adapterPaired.add( device.getName() + "\n" + device.getAddress());
        }

        IntentFilter intentFilterFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(deviceListener, intentFilterFound);
        IntentFilter intentFilterFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(deviceListener, intentFilterFinished);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_scan_devices:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @SuppressLint("MissingPermission")
    private void scanDevices(){
        progressScan.setVisibility(View.VISIBLE);
        adapterAval.clear();

        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }
}