package com.example.socialapp;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class ListDevicesFragment extends DialogFragment {
    private ListView listView;
    private String[] deviceNameArray;
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private WifiP2pDevice[] deviceArray;
    private WifiP2pManager wifiP2pManager;

    public static ListDevicesFragment newInstance() {

        Bundle args = new Bundle();

        ListDevicesFragment fragment = new ListDevicesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public interface OnDeviceClickListener{
        public void onDeviceClick(WifiP2pDevice wifiP2pDevice);
    }

    OnDeviceClickListener onDeviceClickListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Activity activity = getActivity();
        if(context instanceof OnDeviceClickListener)
            onDeviceClickListener = (OnDeviceClickListener) activity;

    }

    WifiP2pManager.PeerListListener peerListListener = peerList -> {
        if (!peerList.getDeviceList().equals(peers)) {
            peers.clear();
            peers.addAll(peerList.getDeviceList());

            deviceNameArray = new String[peers.size()];
            deviceArray = new WifiP2pDevice[peers.size()];

            int index = 0;
            for (WifiP2pDevice device : peers) {
                deviceNameArray[index] = device.deviceName;
                deviceArray[index] = device;
                index++;
            }

            ArrayAdapter<String> arrayAdapter
                    = new ArrayAdapter<>(getActivity().getApplicationContext(),
                    android.R.layout.simple_list_item_1, deviceNameArray);

            listView.setAdapter(arrayAdapter);

            Log.d("peersize", String.valueOf(peers.size()));

            if (peers.size() == 0) {
                Toast.makeText(getActivity().getApplicationContext(),
                        "No Device Found!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.activity_list_devices, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = getView().findViewById(R.id.list_aval_devices);
        wifiP2pManager = (WifiP2pManager) getActivity().getSystemService(Context.WIFI_P2P_SERVICE);

        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            Log.d("index", String.valueOf(i));
            Log.d("array", String.valueOf(deviceArray[i]));
            WifiP2pDevice device = deviceArray[i];
            onDeviceClickListener.onDeviceClick(device);
            getParentFragmentManager().beginTransaction().remove(ListDevicesFragment.this).commit();
        });

    }
}
