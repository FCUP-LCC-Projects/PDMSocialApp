package com.example.socialapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.Objects;


public class BluetoothChatActivity extends AppCompatActivity {
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    private RecyclerView listBluetoothChat;
    private EditText editMessageBlock;
    private Button sendMessageButton;

    private String connectedDevice = null;
    private MessageViewAdapter messageAdapter;
    private StringBuffer outStringBuffer;
    private BluetoothAdapter bluetoothAdapter = null;
    private ChatUtils chatUtils = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não esta disponível", Toast.LENGTH_LONG).show();

        }
        initChatComponents();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (bluetoothAdapter == null) {
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (chatUtils == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatUtils != null) {
            chatUtils.stop();
        }
    }


    private void setState(CharSequence subTitle){
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(subTitle);
    }


    private void initChatComponents(){
        listBluetoothChat = findViewById(R.id.bluetooth_listview);
        editMessageBlock = findViewById(R.id.send_message_text);
        sendMessageButton = findViewById(R.id.send_button);
    }

    private void setupChat() {
        messageAdapter = new MessageViewAdapter(this, new LinkedList<MessageItem>());

        listBluetoothChat.setAdapter(messageAdapter);
        listBluetoothChat.setLayoutManager(new LinearLayoutManager(this));

        editMessageBlock.setOnEditorActionListener(writeListener);


        sendMessageButton.setOnClickListener(view -> {
                if (view != null) {
                    try {
                        String message = editMessageBlock.getText().toString();
                        sendMessage(message);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
        });

        // inicializar o servico bluetooth
        chatUtils = new ChatUtils(this, handler);

        // buffer para mensagens saindo
        outStringBuffer = new StringBuffer();
    }

    /**
     * Ligar o bluetooth por 5 minutos
     */
    private void ensureDiscoverable() {
        if (bluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    private void sendMessage(String message) {

        if (chatUtils.getState() != ChatUtils.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            chatUtils.write(send);

            outStringBuffer.setLength(0);
            editMessageBlock.setText(outStringBuffer);
        }
    }

    private TextView.OnEditorActionListener writeListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {

            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };



    private final Handler handler = new Handler(new Handler.Callback() {
        @SuppressLint("HandlerLeak")
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            if (msg.what == CommCodes.MESSAGE_STATE_CHANGED) {
                switch (msg.arg1) {
                    case ChatUtils.STATE_CONNECTED:
                        // setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                        setState("Connected "+connectedDevice); //change to upper...
                        break;
                    case ChatUtils.STATE_CONNECTING:
                        setState("Connecting...");
                        break;
                    case ChatUtils.STATE_LISTEN:
                    case ChatUtils.STATE_NONE:
                        setState("Not Connected");
                        break;
                }
            } else if (msg.what == CommCodes.MESSAGE_WRITE) {
                byte[] writeBuf = (byte[]) msg.obj;
                // construir string do buffer para adicionar ao arrayadapter
                String writeMessage = new String(writeBuf);
                if(writeMessage.length() != 0){
                    if(writeMessage.charAt(0) == '@'){
                        //nao mandar um posts para eu proprio
                    }else{
                        messageAdapter.getMessageList().add(new MessageItem("Me", writeMessage));
                    }
                }

            } else if (msg.what == CommCodes.MESSAGE_READ) {
                byte[] readBuf = (byte[]) msg.obj;
                // construir string do buffer recebido
                String readMessage = new String(readBuf, 0, msg.arg1);
                if(readMessage.length() != 0){
                    if(readMessage.charAt(0) == '@'){

                    }else if(readMessage.charAt(0) == '#'){
                        String[] parts = readMessage.split(" ", 2);
                        connectedDevice = parts[1];
                    }else{
                        messageAdapter.getMessageList().add(new MessageItem(connectedDevice, readMessage));
                    }
                }
            } else if (msg.what == CommCodes.MESSAGE_DEVICE_NAME) {
                connectedDevice = msg.getData().getString(CommCodes.DEVICE_NAME);
                }
            else if (msg.what == CommCodes.MESSAGE_TOAST) {

            }
            return true;
        }
    });

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Toast.makeText(this, R.string.bluetooth_unenabled_text,
                            Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void connectDevice(Intent data) {

        Bundle extras = data.getExtras();
        if (extras == null) {
            return;
        }
        String address = extras.getString(CommCodes.DEVICE_NAME);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        chatUtils.connect(device);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.search_devices) {// iniciar devicelist e scanear por disps pairados
            startActivityForResult(new Intent(this, ListDevicesActivity.class), REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        } else if (itemId == R.id.bluetooth_enabled) {// ver se o dispositivo pode ser descoberto
            ensureDiscoverable();
            return true;
        }
        return false;
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
