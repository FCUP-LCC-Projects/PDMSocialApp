package com.example.socialapp;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    static class CountThread extends Thread {
        Toast toast;
        int minutes;
        public CountThread(Toast t, int i) {
            toast = t;
            minutes = i;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep(1000*60*minutes);
                    toast.show();
                }

            }
            catch (InterruptedException e) {}
        }
    }

    CountThread t = null;
    private SharedPreferences pref;

    public static SettingsFragment newInstance() { return new SettingsFragment(); }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        final SwitchPreferenceCompat modeToggle = (SwitchPreferenceCompat) findPreference(CommCodes.KEY_PREF_MODE);
        final CheckBoxPreference timelineToggle = (CheckBoxPreference) findPreference(CommCodes.KEY_PREF_TIMELINE);
        final ListPreference breakList = (ListPreference) findPreference(CommCodes.KEY_PREF_BREAK);
        final Preference reportCall = (Preference) findPreference(CommCodes.KEY_PREF_REPORT);

        pref = getActivity().getApplicationContext().getSharedPreferences("myPref",MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        modeToggle.setChecked(pref.getBoolean(CommCodes.KEY_PREF_MODE, false));

        modeToggle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if(modeToggle.isChecked()){
                    modeToggle.setChecked(false);
                    editor.putBoolean(CommCodes.KEY_PREF_MODE, false);
                    editor.apply();
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                else{
                    modeToggle.setChecked(true);
                    editor.putBoolean(CommCodes.KEY_PREF_MODE, true);
                    editor.apply();
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                return true;
            }
        });

        timelineToggle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if(timelineToggle.isChecked()){
                    timelineToggle.setChecked(false);
                    editor.putBoolean(CommCodes.KEY_PREF_TIMELINE, false);
                    editor.apply();
                }else{
                    timelineToggle.setChecked(true);
                    editor.putBoolean(CommCodes.KEY_PREF_TIMELINE, true);
                    editor.apply();
                }
                return true;
            }
        });

        breakList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String val = (String) newValue;
                editor.putInt(CommCodes.KEY_PREF_BREAK, Integer.parseInt(val));
                editor.apply();
                if (t != null) {
                    t.interrupt();
                    try {
                        t.join();
                        Toast.makeText(getActivity().getApplicationContext(),
                                "Changed!", Toast.LENGTH_SHORT).show();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    t = null;
                }
                switch(val) {
                    case "0":
                        Toast.makeText(getActivity().getApplicationContext(), "Disabled",
                                Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast to = Toast.makeText(getActivity().getApplicationContext(), "Remember to take a break",
                                Toast.LENGTH_SHORT);
                        t = new CountThread(to, Integer.parseInt(val));
                        t.start();
                        break;
                }
                return true;
            }
        });
        reportCall.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference arg0) {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse("mailto:"));
                        String[] addr = new String[1];
                        addr[0] = "up201805265@up.pt";
                        intent.putExtra(Intent.EXTRA_EMAIL, addr);
                        startActivity(intent);
                        return true;
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if(pref.getBoolean(CommCodes.KEY_PREF_MODE, false)) {
            view.setBackgroundColor(getResources().getColor(R.color.black));

        }
        else
            view.setBackgroundColor(getResources().getColor(R.color.white));

        return view;
    }
}