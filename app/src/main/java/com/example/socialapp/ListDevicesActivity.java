package com.example.socialapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Set;

public class ListDevicesActivity extends AppCompatActivity {
    private ListView listPairedDevices, listAvalDevices;
    private ArrayAdapter<String> adapterPaired, adapterAval;
    private Context context;
    private ProgressBar progressScan;
    private BluetoothAdapter bluetoothAdapter;


    private final BroadcastReceiver deviceListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice intentDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                try {
                    if (intentDevice != null && intentDevice.getBondState() != BluetoothDevice.BOND_BONDED)
                        adapterAval.add(intentDevice.getName() + "\n" + intentDevice.getAddress());
                } catch (SecurityException e) {
                    Log.e("BondStateSec", e.toString());
                }
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


    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (bluetoothAdapter != null)
                bluetoothAdapter.cancelDiscovery();
        }catch(SecurityException se){
            Log.e("OnDestroySec", se.toString());
        }

        unregisterReceiver(deviceListener);
    }


    private void initDevices(){
        listAvalDevices = findViewById(R.id.list_aval_devices);
        listPairedDevices = findViewById(R.id.list_paired_devices);

        adapterPaired = new ArrayAdapter<>(context, R.layout.device_list_item);
        adapterAval = new ArrayAdapter<>(context, R.layout.device_list_item);

        listPairedDevices.setAdapter(adapterPaired);
        listAvalDevices.setAdapter(adapterAval);

        progressScan = findViewById(R.id.progress_devices);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        getUsername().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                try{ bluetoothAdapter.setName(task.getResult()); }
                catch(SecurityException e){ Log.e("SetNameSec", e.toString());}
            }
        });

        Set<BluetoothDevice> pairedDevices = null;
        try {
            pairedDevices = bluetoothAdapter.getBondedDevices();
        }catch(SecurityException e){ Log.e("BondedDevicesSec", e.toString());}

        listPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onDeviceClickStart(view);
            }
        });

        listAvalDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onDeviceClickStart(view);
            }
        });

        try {
            if (pairedDevices!= null && pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices)
                    adapterPaired.add(device.getName() + "\n" + device.getAddress());
            }
        }catch (SecurityException e){
            Log.e("AddDevicesScan", e.toString());
        }

        IntentFilter intentFilterFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(deviceListener, intentFilterFound);
        IntentFilter intentFilterFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(deviceListener, intentFilterFinished);
    }


    private void onDeviceClickStart(View view){
        try{
            bluetoothAdapter.cancelDiscovery();
        }catch (SecurityException e){
            Log.e("StartCommSec", e.toString());
        }

        String info = ((TextView) view).getText().toString();
        String address = info.substring(info.length() - 17);

        Intent intent = new Intent(ListDevicesActivity.this, ChatActivity.class);
        intent.putExtra("address", address);
        setResult(RESULT_OK, intent);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_scan_devices:
                scanDevices();
                return true;
            case R.id.bluetooth_enabled:
                enableBluetooth();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void enableBluetooth() {
        try {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        } catch (SecurityException e) {
            Log.e("EnableBluetooth", e.toString());
        }
    }

    private void scanDevices() {
        progressScan.setVisibility(View.VISIBLE);
        adapterAval.clear();

        setTitle(R.string.scanning);

        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            Log.e("ScanDevices", e.toString());
        }
    }

    protected static Task<String> getUsername(){
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference(firebaseAuth.getUid());
        final TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserProfile userProfile = snapshot.getValue(UserProfile.class);
                taskCompletionSource.setResult(userProfile.getUsername());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("The read failed: " + error.getCode());
                taskCompletionSource.setException(error.toException());
            }
        });

        return taskCompletionSource.getTask();
    }
}