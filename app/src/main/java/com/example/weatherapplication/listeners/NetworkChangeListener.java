package com.example.weatherapplication.listeners;

import android.net.ConnectivityManager;
import android.net.NetworkRequest;

import com.example.weatherapplication.utils.ConnectivityStatus;

public interface NetworkChangeListener {

    void checkInternetConnectivity();
    void registerNetworkChangeListener();
    void unregisterNetworkChangeListener();
}
