package com.example.socialapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class ChatUtils {
    private final Context context;
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private CommunicateThread commThread;
    private ExchangeThread exchangeThread;
    private AckThread ackThread;
    private final UUID APP_UUID = UUID.fromString("b0a1cff4-9b6b-464c-864f-7f7529e05c04"); //randomly generated
    private final String APP_NAME = "PDM_APP";

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;

    public ChatUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
    }

    public int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(ChatActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }

    public void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            commThread.cancel();
            commThread = null;
        }

        commThread = new CommunicateThread(device);
        commThread.start();

        setState(STATE_CONNECTING);
    }

    public synchronized void start() {
        if (commThread != null) {
            commThread.cancel();
            commThread = null;
        }

        if (ackThread != null) {
            ackThread.cancel();
            ackThread = null;
        }

        if(exchangeThread != null){
            exchangeThread.cancel();
            exchangeThread = null;
        }

        setState(STATE_LISTEN);
    }

    public synchronized void stop() {
        if (commThread != null) {
            commThread.cancel();
            commThread = null;
        }

        if (ackThread != null) {
            ackThread.cancel();
            ackThread = null;
        }

        if(exchangeThread != null){
            exchangeThread.cancel();
            exchangeThread = null;
        }

        setState(STATE_NONE);
    }

    public void write(byte[] buffer) {
        ExchangeThread exThread;
        synchronized (this) {
            if (state != STATE_CONNECTED) {
                return;
            }

            exThread = exchangeThread ;
        }

        exThread.write(buffer);
    }

    @SuppressLint("MissingPermission")
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (commThread != null) {
            commThread.cancel();
            commThread = null;
        }

        if(exchangeThread != null){
            exchangeThread.cancel();
            exchangeThread = null;
        }

        exchangeThread = new ExchangeThread(socket);
        exchangeThread.start();

        Message message = handler.obtainMessage(ChatActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(ChatActivity.DEVICE_NAME, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }

    private synchronized void connectionFailed() {
        Message message = handler.obtainMessage(ChatActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ChatActivity.TOAST_MESSAGE, "Can't Connect to the Device");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    private class CommunicateThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        @SuppressLint("MissingPermission")
        public CommunicateThread(BluetoothDevice device) {
            this.device = device;

            BluetoothSocket tempSocket = null;
            try {
                tempSocket = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("CommThread.Constructor", e.toString());
            }

            socket = tempSocket;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            try {
                socket.connect();
            } catch (IOException e) {
                Log.e("CommThread.run ", e.toString());
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e("CommThread.close", e2.toString());
                }
            }

            synchronized (ChatUtils.this) {
                commThread = this;
            }

            connect(device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e2) {
                Log.e("CommThread.cancel", e2.toString());
            }
        }
    }

    private class AckThread extends Thread{
        private BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        public AckThread(){
            BluetoothServerSocket tempServerSocket = null;
            try{
                tempServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,APP_UUID);
            }catch(IOException e) {
                Log.e("AckThread.constructor", e.toString());
            }
            serverSocket = tempServerSocket;
        }

        public void run(){
            BluetoothSocket socket = null;
            try{
                socket = serverSocket.accept();
            }catch(IOException e) {
                Log.e("AckThread.run", e.toString());
            }

            if(socket!=null){
                switch(state){
                    case STATE_CONNECTING: connect(socket.getRemoteDevice()); break;
                    case STATE_CONNECTED:
                    case STATE_NONE:
                        try {
                            socket.close();
                        }catch(IOException e){ Log.e("AckThread.run", e.toString());} break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e2) {
                Log.e("AckThread.cancel", e2.toString());
            }
        }
    }

    private class ExchangeThread extends Thread {
        private static final int BUFFER_LENGTH = 1024;
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ExchangeThread(BluetoothSocket socket) {
            this.socket = socket;

            InputStream tmpInStream = null;
            OutputStream tmpOutStream = null;
            try {
                tmpInStream = socket.getInputStream();
                tmpOutStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("ExchangeThread.Constr", e.toString());
            }
            this.inputStream = tmpInStream;
            this.outputStream = tmpOutStream;
        }

        public void run() {
            byte[] buffer = new byte[BUFFER_LENGTH];
            int bytes;

            try {
                bytes = inputStream.read(buffer);
                handler.obtainMessage(ChatActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {
                lostConnection();
            }
        }

        public void write(byte[] buffer){
            try{
                outputStream.write(buffer);
                handler.obtainMessage(ChatActivity.MESSAGE_WRITE, -1, -1, buffer);
            }catch(IOException e){

            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("ExchangeThread.cancel", e.toString());
            }
        }
    }

    private void lostConnection() {
        Message message = handler.obtainMessage(ChatActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ChatActivity.TOAST_MESSAGE, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);
    }
}
