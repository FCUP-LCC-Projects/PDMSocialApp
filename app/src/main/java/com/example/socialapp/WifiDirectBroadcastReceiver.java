package com.example.socialapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;


public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private ChatActivity chatActivity;
    private PostCommActivity postCommActivity;
    private ListDevicesFragment fragment;

    public WifiDirectBroadcastReceiver(WifiP2pManager wifiP2pManager,
                                       WifiP2pManager.Channel channel,
                                       Activity activity, ListDevicesFragment fragment) {
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
        this.fragment = fragment;
        this.postCommActivity = null;
        this.chatActivity = null;
        if(activity instanceof ChatActivity)
            chatActivity = (ChatActivity) activity;
        else if(activity instanceof PostCommActivity)
            postCommActivity = (PostCommActivity) activity;

    }

    @Override
    @SuppressLint("MissingPermission")
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(context, "Wifi is on!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Wifi is off!", Toast.LENGTH_SHORT).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers.
            if (wifiP2pManager != null) {
                wifiP2pManager.requestPeers(channel, fragment.peerListListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connections or disconnections.
            if (wifiP2pManager == null) {
                return;
            }

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                if(chatActivity!=null)
                    wifiP2pManager.requestConnectionInfo(channel, chatActivity.connectionInfoListener);
                else if(postCommActivity !=null)
                    wifiP2pManager.requestConnectionInfo(channel, postCommActivity.connectionInfoListener);
            } else {
                if(chatActivity!=null)
                    chatActivity.setState("Device disconnected");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's Wi-Fi state changing.
        }

    }
}
