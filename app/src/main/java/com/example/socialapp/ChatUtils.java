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
    private static final String NAME_SECURE = "BluetoothChatSecure";

    private static final UUID APP_UUID = UUID.fromString("b0a1cff4-9b6b-464c-864f-7f7529e05c04"); //randomly generated

    private final BluetoothAdapter adapter;
    private Handler handler;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private CommunicateThread commThread;
    private int mState;
    private int mNewState;

    // Constantes para cada estado da conexao
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    /**
     * Construtor para a sessao de chat bluetooth
     *
     * @param context A view atual
     * @param handler handler para mandar de volta ao context
     */
    public ChatUtils(Context context, Handler handler) {
        adapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        handler = handler;
    }


    /**
     * Retorna o estado atual (daqueles 4) da conexao
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Comeca o servico de chat, comecando uma AcceptThread e ouvindo em modo de server.
     * E chamado pelo onResume()
     */
    public synchronized void start() {
        Log.d("ChatUtils", "start");

        // Cancelar threads tentando fazer a conexao
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancelar threads que estao conectadas
        if (commThread != null) {
            commThread.cancel();
            commThread = null;
        }

        // Comecar a thread para ouvir
        if (acceptThread == null) {
            acceptThread = new AcceptThread(true);
            acceptThread.start();
        }



    }

    /**
     * Come a ConnectThread para conectar a um dispositivo
     *
     * @param device O dispositivo
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d("ChatUtils", "connect to: " + device);

        // Cancelar threads tentando fazer a conexao
        if (mState == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        // Cancelar threads que estao conectadas
        if (commThread != null) {
            commThread.cancel();
            commThread = null;
        }

        // Comecar a thread para ouvir
        connectThread = new ConnectThread(device);
        connectThread.start();

    }

    /**
     * Comeca a ConnectedThread para lidar com a conexao bluetooth atual
     *
     * @param socket A BluetoothSocket da conexao
     * @param device O dispositivo a conectar
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d("ChatUtils", "connected, Socket Type:" + socketType);

        // Cancelar a ConnectThread ja que ja estamos connectados
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancelar qualquer outra conexao actual
        if (commThread != null) {
            commThread.cancel();
            commThread = null;
        }

        // Cancelar a acceptthread ja que so queremos nos ligar a 1 dispositivo
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        // Comecar a thread e comecar a transmitir
        commThread = new CommunicateThread(socket, socketType);
        commThread.start();

        // Mandar o nome do dispositivo de volta a atividade da UI
        Message msg = handler.obtainMessage(CommCodes.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(CommCodes.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);



    }

    /**
     * Parar TODAS as threads
     */
    public synchronized void stop() {
        Log.d("ChatUtils", "stop");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (commThread != null) {
            commThread.cancel();
            commThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        mState = STATE_NONE;

    }

    /**
     * Escrever para a ConnectThread
     * De forma asincrona
     *
     * @param out O que escrever (array de bytes)
     */
    public void write(byte[] out) {
        CommunicateThread r;
        // sincronizar uma copia da connectedthread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = commThread;
        }
        // escrever de forma asincrona
        r.write(out);
    }

    /**
     * Indicar que a conexao falhou e informar a UI
     */
    private void connectionFailed() {
        // Mandar a mensagem de falha de volta
        Message msg = handler.obtainMessage(CommCodes.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(CommCodes.TOAST_MESSAGE, "Unable to connect device");
        msg.setData(bundle);
        handler.sendMessage(msg);

        mState = STATE_NONE;


        // Comecar o servico mais uma vez
        ChatUtils.this.start();
    }

    /**
     * Indicar que a conexao foi perdida e informar o utilizador
     */
    private void connectionLost() {
        // Mandar a mensagem de falha de volta a UI
        Message msg = handler.obtainMessage(CommCodes.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(CommCodes.TOAST_MESSAGE, "Device connection was lost");
        msg.setData(bundle);
        handler.sendMessage(msg);

        mState = STATE_NONE;


        // Comecar o servico mais uma vez
        ChatUtils.this.start();
    }

    /**
     * Esse thread executa enquanto escuta a conexoes.
     * Ela funciona como um client server-side, executa ate a conexao for aceite ou cancelada
     */
    private class AcceptThread extends Thread {
        // A server socket local
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Criar novo server socket para ouvir
            try {
                tmp = adapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, APP_UUID);
            } catch (IOException e) {
                Log.e("ChatUtils", "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            Log.d("ChatUtils", "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket;

            //  Ourvir a server socket para ver se estamos conectados
            while (mState != STATE_CONNECTED) {
                try {
                    // Esse call so retorna se a conexao for aceite ou haver exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e("ChatUtils", "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // Se for aceite...
                if (socket != null) {
                    synchronized (ChatUtils.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Tudo bom, comecar a connectrhread
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Nao esta pronto ou nao conectou, fechar a socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e("ChatUtils", "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i("ChatUtils", "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d("ChatUtils", "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e("ChatUtils", "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * Essa thread executa enquanto tenta fazer uma conexao a um dispositivo.
     * Executa ate sucedir ou falhar
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = "Secure";

            // pegar a BluetoothSocket usando o BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("ChatUtils", "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i("ChatUtils", "BEGIN connectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Lembrar de cancelar a descoberta depois disso, para nao deixar tudo lerdo
            adapter.cancelDiscovery();

            // conectar a BluetoothSocket
            try {
                //So retorna quando for sucedido ou em excessao
                mmSocket.connect();
            } catch (IOException e) {
                // fechar a socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e("ChatUtils", "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Acabar com a ConnectThread quando acabamos
            synchronized (ChatUtils.this) {
                connectThread = null;
            }

            // Comecar a ConnectedThread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("ChatUtils", "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * Essa thread roda enquanto ha a conexao com um dispositivo remoto.
     * Lida com todas as conexoes entrando e saindo
     */
    private class CommunicateThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public CommunicateThread(BluetoothSocket socket, String socketType) {
            Log.d("ChatUtils", "create CommunicateThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Pegar streams de input e output de BluetoothSocket
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("ChatUtils", "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i("ChatUtils", "BEGIN commThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Continuar ouvindo a InputStream enquanto conectada
            while (mState == STATE_CONNECTED) {
                try {
                    // Ler da InputStream
                    bytes = mmInStream.read(buffer);

                    // Mandar os bytes obtidos a atividade UI
                    handler.obtainMessage(CommCodes.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e("ChatUtils", "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Escrever para a OutStream conectada
         *
         * @param buffer O que escrever (array de bytes)
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Mandar a mensagem de volta a atividade UI
                handler.obtainMessage(CommCodes.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e("ChatUtils", "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("ChatUtils", "close() of connect socket failed", e);
            }
        }
    }
}
