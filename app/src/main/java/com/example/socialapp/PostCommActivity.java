package com.example.socialapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PostCommActivity extends AppCompatActivity implements ListDevicesFragment.OnDeviceClickListener{
    TextView messageText;
    ProgressBar progressBar;
    WifiManager wifiManager;
    WifiP2pManager wifiP2pManager;
    WifiP2pManager.Channel channel;

    BroadcastReceiver receiver;
    IntentFilter intentFilter;

    ListDevicesFragment fragment;
    String connectedDevice;

    ServerThread serverThread;
    ClientThread clientThread;
    SendReceiveThread sendReceiveThread;


    public class ServerThread extends Thread {
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
                sendReceiveThread = new SendReceiveThread(socket);
                Log.d("ServerThread", "Beginning comm thread");
                sendReceiveThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public class ClientThread extends Thread {
        Socket socket;
        String hostAddress;

        ClientThread(InetAddress hostAddress) {
            this.hostAddress = hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAddress, 8888), 500);
                sendReceiveThread = new SendReceiveThread(socket);
                sendReceiveThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class SendReceiveThread extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceiveThread(Socket socket) {
            this.socket = socket;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (socket != null) {
                // Listen for message
                // We received something
                handler.obtainMessage(CommCodes.MESSAGE_READ, -1, -1, inputStream)
                        .sendToTarget();

            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
                handler.obtainMessage(CommCodes.MESSAGE_WRITE, -1, -1, bytes).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Handler handler = new Handler(msg -> {
        if(msg.what == CommCodes.MESSAGE_READ){
            Log.d("POST_READ", "Receiving posts");
            InputStream inputStream = (InputStream) msg.obj;
            JSONParser jsonParser = new JSONParser();
            try {
                JSONArray jsonArray = (JSONArray) jsonParser.parse(new InputStreamReader(inputStream, "UTF-8"));
                IOUtils.writeFiletToIStorage(this, IOUtils.parsePostList(jsonArray));
            } catch (IOException exception) {
                exception.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finish();
        }
        else if(msg.what == CommCodes.MESSAGE_WRITE){
            Log.d("POST_WRITE", "Sending posts");
        }
        return true;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_comm);

        init();
    }

    private void init(){
        messageText = findViewById(R.id.post_comm_text);
        messageText.setText("Search for Devices");
        progressBar = findViewById(R.id.post_comm_progress);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        fragment = ListDevicesFragment.newInstance();
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        receiver = new WifiDirectBroadcastReceiver(wifiP2pManager, channel, this, fragment);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    @Override
    public void onDeviceClick( WifiP2pDevice wifiP2pDevice) {
        WifiP2pConfig config = new WifiP2pConfig();
        connectedDevice = wifiP2pDevice.deviceName;
        config.deviceAddress = wifiP2pDevice.deviceAddress;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),
                        "Connected to " + wifiP2pDevice.deviceName, Toast.LENGTH_SHORT).show();

                messageText.setText("Sending and Receiving Posts...");
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(),
                        "Connection failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register the broadcast receiver
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the broadcast receiver
        unregisterReceiver(receiver);
    }


    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupOwnerAddress = info.groupOwnerAddress;

            if (info.groupFormed && info.isGroupOwner) {
                Log.d("State", "Host");
                serverThread = new ServerThread();
                serverThread.start();
                String message = IOUtils.readJSONFromIStorage(PostCommActivity.this);
                Log.d("POST_SEND", message);
                if(message!=null && sendReceiveThread!=null)
                    sendReceiveThread.write(message.getBytes());
            } else if (info.groupFormed) {
                Log.d("State", "Client");
                clientThread = new ClientThread(groupOwnerAddress);
                clientThread.start();
                String message = IOUtils.readJSONFromIStorage(PostCommActivity.this);
                Log.d("POST_SEND", message);
                if(message!=null && sendReceiveThread!=null)
                    sendReceiveThread.write(message.getBytes());
            }
        }
    };

    private void discoverDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Successfully started discovering
                messageText.setText("Discovery Started");
            }

            @Override
            public void onFailure(int reason) {
                // Failed to start discovering
                messageText.setText("Discovery failed to Start");
            }
        });
        getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                .add(R.id.post_comm_container,
                        fragment, "ListDevices")
                .addToBackStack("ListDevices")
                .commit();
    }

    private void toggleWifi(){
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
            Toast.makeText(this,"Wifi On", Toast.LENGTH_SHORT);
        } else {
            wifiManager.setWifiEnabled(true);
            Toast.makeText(this,"Wifi Off", Toast.LENGTH_SHORT);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.search_devices) {// iniciar devicelist e scanear por disps pairados
            discoverDevices();
            item.setVisible(false);
            return true;
        } else if (itemId == R.id.bluetooth_enabled) {// ver se o dispositivo pode ser descoberto
            toggleWifi();
            return true;
        }
        return false;
    }
}