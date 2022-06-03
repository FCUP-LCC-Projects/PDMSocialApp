package com.example.socialapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.internal.VisibilityAwareImageButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity implements ListDevicesFragment.OnDeviceClickListener {
    private RecyclerView listView;
    private MessageViewAdapter messageViewAdapter;
    private TextView readMsgTextView;
    TextView connectionStatusTextView;

    private EditText editMessageBlock;
    private Button sendMessageButton;

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
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        // We received something
                        handler.obtainMessage(CommCodes.MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        StrictMode.ThreadPolicy policy
                = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        initializeComponents();

        if (savedInstanceState != null)
            editMessageBlock.setText(savedInstanceState.getString("message"));

        runListener();
        setState("Not Connected");
    }

    Handler handler = new Handler(msg -> {
        if(msg.what == CommCodes.MESSAGE_READ) {
            Log.d("MESSAGE_READ", "received");
            byte[] readBuffer = (byte[]) msg.obj;
            String readMessage = new String(readBuffer, 0, msg.arg1);
            MessageItem messageItem = new MessageItem(connectedDevice, readMessage);
            messageViewAdapter.getMessageList().push(messageItem);
        }
         else if(msg.what == CommCodes.MESSAGE_WRITE){
             Log.d("MESSAGE_WRITE", "sent");
            byte[] writeBuffer = (byte[]) msg.obj;
            String writeMessage = new String(writeBuffer);
            if(writeMessage.length() != 0){
                MessageItem messageItem = new MessageItem("Me", writeMessage);
                messageViewAdapter.getMessageList().push(messageItem);
            }
        }
        return true;
    });

    public void setState(CharSequence subTitle){
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(subTitle);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String msg = editMessageBlock.getText().toString().trim();
        if (!msg.isEmpty())
            outState.putString("message", msg);
    }


    private void runListener() {

        sendMessageButton.setOnClickListener(v -> {
            String msg = editMessageBlock.getText().toString();
            Log.d("message", msg);
            if(!msg.isEmpty() && sendReceiveThread!=null)
                sendReceiveThread.write(msg.getBytes());
                editMessageBlock.setText("");
        });

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
                setState("Host");
                serverThread = new ServerThread();
                serverThread.start();
                readMsgTextView.setVisibility(View.INVISIBLE);
            } else if (info.groupFormed) {
                setState("Client");
                clientThread = new ClientThread(groupOwnerAddress);
                clientThread.start();
                readMsgTextView.setVisibility(View.INVISIBLE);
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
                readMsgTextView.setText("Discovery Started");
            }

            @Override
            public void onFailure(int reason) {
                // Failed to start discovering
                readMsgTextView.setText("Discovery failed to Start");
            }
        });
        getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                .add(R.id.list_devices_container,
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

    private void initializeComponents() {

        sendMessageButton = findViewById(R.id.send_button);
        listView = findViewById(R.id.bluetooth_listview);
        readMsgTextView = findViewById(R.id.readMsg);
        editMessageBlock = findViewById(R.id.send_message_text);

        messageViewAdapter = new MessageViewAdapter(this, new LinkedList<>());
        listView.setAdapter(messageViewAdapter);
        listView.setLayoutManager(new LinearLayoutManager(this));

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

    public class MessageViewHolder extends RecyclerView.ViewHolder{
        LinearLayout container;
        TextView messageUser, messageText;
        ImageView messageImage;
        final MessageViewAdapter messageViewAdapter;


        public MessageViewHolder(View itemView, MessageViewAdapter messageViewAdapter) {
            super(itemView);
            container = itemView.findViewById(R.id.message_item_container);
            messageImage = itemView.findViewById(R.id.message_item_image);
            messageUser = itemView.findViewById(R.id.message_item_user);
            messageText = itemView.findViewById(R.id.message_item_text);
            this.messageViewAdapter = messageViewAdapter;
        }
    }

    public class MessageViewAdapter extends RecyclerView.Adapter<MessageViewHolder>{
        private final LinkedList<MessageItem> messageList;
        private final LayoutInflater layoutInflater;

        public MessageViewAdapter(Context context, LinkedList<MessageItem> messageList){
            this.messageList = messageList;
            layoutInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = layoutInflater.inflate(R.layout.message_item, parent, false);
            return new MessageViewHolder(itemView, this);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            MessageItem current = messageList.get(position);
            holder.messageUser.setText(current.getUsername());
            holder.messageText.setText(current.getMessage());
            //holder.messageImage.setImageDrawable(current.getImage()); NEED TO FIGURE OUT HOW TO SET IMAGE
        }

        @Override
        public int getItemCount() { return messageList.size(); }

        public LinkedList<MessageItem> getMessageList() { return messageList; }
    }
}
