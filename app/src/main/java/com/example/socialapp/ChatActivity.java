package com.example.socialapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ChatActivity extends AppCompatActivity {
    private Context context;
    private ChatUtils chatUtils;
    private BluetoothAdapter bluetoothAdapter;
    private final int REQUEST_LOCATION_PERMISSION = 101;


    private ListView listBluetoothChat;
    private EditText editMessageBlock;
    private Button sendMessageButton;
    private ArrayAdapter<String> messageAdapter;

    private String connectedDevice;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch(message.what){
                case CommCodes.MESSAGE_STATE_CHANGED:
                    switch(message.arg1){
                        case ChatUtils.STATE_NONE: setState("Not Connected"); break;
                        case ChatUtils.STATE_LISTEN: setState("Not Connected"); break;
                        case ChatUtils.STATE_CONNECTED: setState("Connecting..."); break;
                        case ChatUtils.STATE_CONNECTING: setState("Connected "+connectedDevice); break;
                    }
                    break;
                case CommCodes.MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(CommCodes.DEVICE_NAME);
                    break;
                case CommCodes.MESSAGE_READ:
                    byte[] buffer = (byte[]) message.obj;
                    String inBuffer = new String(buffer, 0, message.arg1);
                    messageAdapter.add(connectedDevice + ": "+inBuffer);
                    break;
                case CommCodes.MESSAGE_TOAST:
                    Toast.makeText(context, message.getData().getString(CommCodes.TOAST_MESSAGE), Toast.LENGTH_SHORT).show();
                    break;
                case CommCodes.MESSAGE_WRITE:
                    byte[] buffer2 = (byte[]) message.obj;
                    String outBuffer = new String(buffer2);
                    messageAdapter.add("Me: "+outBuffer);

                    break;
            }
            return false;
        }
    });



    ActivityResultLauncher<Intent> selectDeviceLauncher =  registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = getIntent();
                        String address = intent.getStringExtra("address");
                        chatUtils.connect(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address));
                    }
                }
            }
    );

    private void setState(CharSequence subTitle){
        getSupportActionBar().setSubtitle(subTitle);
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

    private void initBluetoothAdapter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null)
            Toast.makeText(context, "No Bluetooth Adapter", Toast.LENGTH_SHORT).show();
    }

    private void initChatComponents(){
        listBluetoothChat = findViewById(R.id.bluetooth_listview);
        editMessageBlock = findViewById(R.id.send_message_text);
        sendMessageButton = findViewById(R.id.send_button);

        messageAdapter = new ArrayAdapter<String>(context, R.layout.device_list_item);
        listBluetoothChat.setAdapter(messageAdapter);

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = editMessageBlock.getText().toString();
                if (!message.isEmpty()) {
                    editMessageBlock.setText("");
                    chatUtils.write(message.getBytes());
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.search_devices:
                checkPermissions();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        else{
            Intent intent = new Intent(this, ListDevicesActivity.class);
            selectDeviceLauncher.launch(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(this, ListDevicesActivity.class);
                startActivity(intent);
            } else {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Access to Location is necessary to use the app")
                        .setPositiveButton("Access",  (dialogInterface, i) -> checkPermissions())
                        .setNegativeButton("Deny", (dialogInterface, i) -> ChatActivity.this.finish()).show();
            }
        }
        else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}