package com.example.socialapp;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences pref;

    public static SettingsFragment newInstance() { return new SettingsFragment(); }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        final SwitchPreferenceCompat modeToggle = (SwitchPreferenceCompat) findPreference(CommCodes.KEY_PREF_MODE);
        final CheckBoxPreference timelineToggle = (CheckBoxPreference) findPreference(CommCodes.KEY_PREF_TIMELINE);

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