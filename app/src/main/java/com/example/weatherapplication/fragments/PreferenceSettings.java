package com.example.weatherapplication.fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.example.weatherapplication.R;

public class PreferenceSettings extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preference_settings);
    }
}
