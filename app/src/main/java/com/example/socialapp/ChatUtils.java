package com.example.socialapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.app.ActivityCompat;

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


    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;

    public ChatUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        this.state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(CommCodes.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }

    public void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            if (commThread != null) {
                commThread.cancel();
                commThread = null;
            }
        }

        if (exchangeThread != null) {
            exchangeThread.cancel();
            exchangeThread = null;
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
            ackThread = new AckThread();
            ackThread.start();
        }

        if (exchangeThread != null) {
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

        if (exchangeThread != null) {
            exchangeThread.cancel();
            exchangeThread = null;
        }

        setState(STATE_NONE);
    }

    public void write(byte[] buffer) {
        ExchangeThread exThread;
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            exThread = exchangeThread;
        }

        exThread.write(buffer);
    }


    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (commThread != null) {
            commThread.cancel();
            commThread = null;
        }

        if (exchangeThread != null) {
            exchangeThread.cancel();
            exchangeThread = null;
        }

        if (ackThread != null) {
            ackThread.cancel();
            ackThread = null;
        }

        exchangeThread = new ExchangeThread(socket);
        exchangeThread.start();

        Message message = handler.obtainMessage(CommCodes.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(CommCodes.DEVICE_NAME, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }

    private synchronized void connectionFailed() {
        Message message = handler.obtainMessage(CommCodes.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(CommCodes.TOAST_MESSAGE, "Can't Connect to the Device");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    private class CommunicateThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public CommunicateThread(BluetoothDevice device) {
            this.device = device;

            BluetoothSocket tempSocket = null;
            try {
                tempSocket = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("CommThread.Constructor", e.toString());
            } catch(SecurityException se) {
                Log.e("CommThread.Sec", se.toString());
            }

            socket = tempSocket;
            setState(STATE_CONNECTING);
        }


        public void run() {
            Log.d("CommThread", "running");
            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
            } catch (IOException e) {
                Log.e("CommThread.run ", e.toString());
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e("CommThread.close", e2.toString());
                }
                connectionFailed();
                return;
            }catch(SecurityException e3){}

            synchronized (ChatUtils.this) {
                commThread = null;
            }

            connected(socket, device);
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


        public AckThread(){
            BluetoothServerSocket tempServerSocket = null;
            try{
                tempServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(CommCodes.APP_NAME,APP_UUID);
            }catch(IOException e) {
                Log.e("AckThread.constructor", e.toString());
            }catch (SecurityException e2){}
            serverSocket = tempServerSocket;
            setState(STATE_LISTEN);
        }

        public void run() {
            Log.d("AckThread", "running");
            BluetoothSocket socket = null;

            while (state != STATE_CONNECTED) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e("AckThread.run", e.toString());
                    break;
                }
            }

            if (socket != null) {
                synchronized (ChatUtils.this) {
                    switch (state) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_CONNECTED:
                        case STATE_NONE:
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e("AckThread.run", e.toString());
                            }
                            break;
                    }
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
            setState(STATE_CONNECTED);
        }

        public void run() {
            Log.d("ExchangeThread", "running");
            byte[] buffer = new byte[BUFFER_LENGTH];
            int bytes;

            while (state == STATE_CONNECTED) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(CommCodes.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    lostConnection();
                    break;
                }
            }
        }

        public void write(byte[] buffer){
            try{
                outputStream.write(buffer);
                handler.obtainMessage(CommCodes.MESSAGE_WRITE, -1, -1, buffer);
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
        Message message = handler.obtainMessage(CommCodes.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(CommCodes.TOAST_MESSAGE, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_NONE);
        ChatUtils.this.start();
    }
}
