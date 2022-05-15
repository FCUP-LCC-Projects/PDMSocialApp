package com.example.socialapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


import java.util.LinkedList;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {
    private Context context;
    private ChatUtils chatUtils;
    private BluetoothAdapter bluetoothAdapter;

    private ListView listBluetoothChat;
    private EditText editMessageBlock;
    private Button sendMessageButton;
    private LinkedList<String> messageList;
    private ArrayAdapter<String> messageAdapter;

    private String connectedDevice = "";
    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch(message.what){
                case CommCodes.MESSAGE_STATE_CHANGED:
                    switch(message.arg1){
                        case ChatUtils.STATE_NONE:
                        case ChatUtils.STATE_LISTEN: setState("Not Connected"); break;
                        case ChatUtils.STATE_CONNECTED: setState("Connecting..."); break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connected "+connectedDevice);
                            messageAdapter.clear();
                            break;
                    }
                    break;
                case CommCodes.MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(CommCodes.DEVICE_NAME);
                    Toast.makeText(context,"Connected to " + connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case CommCodes.MESSAGE_READ:
                    byte[] buffer = (byte[]) message.obj;
                    String inBuffer = new String(buffer, 0, message.arg1);
                    if(!inBuffer.isEmpty()){
                        Log.d("Message Received", "message");
                        messageAdapter.add(connectedDevice + ": "+inBuffer);
                    }
                    break;
                case CommCodes.MESSAGE_TOAST:
                    Toast.makeText(context, message.getData().getString(CommCodes.TOAST_MESSAGE), Toast.LENGTH_SHORT).show();
                    break;
                case CommCodes.MESSAGE_WRITE:
                    byte[] buffer2 = (byte[]) message.obj;
                    String outBuffer = new String(buffer2);
                    if(!outBuffer.isEmpty())
                        messageAdapter.add("Me: "+outBuffer);
                    Log.d("Message Sent", "message");
                    break;
            }
            return false;
        }
    });


    private void setState(CharSequence subTitle){
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(subTitle);
    }

    @Override
    protected void onDestroy() {
        if(chatUtils!=null)
            chatUtils.stop();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        chatUtils = new ChatUtils(context, handler);
        initBluetoothAdapter();
        initChatComponents();
    }

    @Override
    protected void onResume() {
        super.onResume();

        getSupportActionBar().setSubtitle(connectedDevice);
        if(chatUtils != null){
            if(chatUtils.getState() == ChatUtils.STATE_NONE)
                chatUtils.start();
        }
    }

    private void initBluetoothAdapter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null)
            Toast.makeText(context, "No Bluetooth Adapter", Toast.LENGTH_SHORT).show();
    }

    private void initChatComponents(){
        listBluetoothChat = findViewById(R.id.bluetooth_listview);
        editMessageBlock = findViewById(R.id.send_message_text);
        sendMessageButton = findViewById(R.id.send_button);

        messageList = new LinkedList<String>();
        messageAdapter = new ArrayAdapter<String>(context, R.layout.message_item, messageList);
        listBluetoothChat.setAdapter(messageAdapter);



        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = editMessageBlock.getText().toString();
                if (!message.isEmpty()) {
                    editMessageBlock.setText("");
                    sendMessage(message);
                }
            }
        });

        Intent intent = getIntent();
        String address = intent.getStringExtra("address");

        Log.d("Device Address", address);

        chatUtils.connect(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address));
    }

    private void sendMessage(String message) {
        if(chatUtils.getState() != ChatUtils.STATE_CONNECTED){
            Toast.makeText(context, "Not Connected", Toast.LENGTH_SHORT).show();
            return;
        }

        if(message.length() >0){
            byte[] text = message.getBytes();
            chatUtils.write(text);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.chat_menu_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.search_devices:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}