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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

        getUsername().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                bluetoothAdapter.setName(task.getResult());
            }
        });


        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        listAvalDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                //Intent intent = new Intent(ListDevicesActivity.this, ChatActivity.class);
                Intent intent = new Intent();
                intent.putExtra("address", address);
                setResult(RESULT_OK, intent);
                finish();
                //startActivity(intent);
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

    @SuppressLint("MissingPermission")
    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 240);
            startActivity(discoverableIntent);
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

    protected static Task<String> getUsername(){
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
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