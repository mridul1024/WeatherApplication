package com.example.weatherapplication.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.weatherapplication.R;
import com.example.weatherapplication.broadcastReceiver.WeatherNotificationReceiver;
import com.example.weatherapplication.di.viewmodel_module.ViewModelFactory;
import com.example.weatherapplication.fragments.WeeklyOverviewDialog;
import com.example.weatherapplication.listeners.PositionInterface;
import com.example.weatherapplication.listeners.NetworkChangeListener;
import com.example.weatherapplication.network.model.DataModel;
import com.example.weatherapplication.network.model.weekly.DataModelWeekly;
import com.example.weatherapplication.utils.ConnectivityStatus;
import com.example.weatherapplication.utils.TaskHandler;
import com.example.weatherapplication.viewmodel.WeatherViewModel;
import com.example.weatherapplication.viewmodel.WeeklyWeatherViewModel;

import java.util.Calendar;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;


public class HomeActivity extends DaggerAppCompatActivity
        implements
        NetworkChangeListener, PositionInterface,
        WeeklyOverviewDialog.PassWeeklyData,
        SharedPreferences.OnSharedPreferenceChangeListener,
        LocationListener {

    private static final String TAG = "HomeActivity";

    private Toast toastMessage;

    private static final String JOB_FLAG = "job_flag_key";
    private static final String UNITS_FLAG = "units_flag_key";

    private static final int PENDING_INTENT_REQUEST_CODE = 5;

    private String mUnits;
    private boolean mNotificationSwitch;

    private WeatherViewModel mWeatherViewModel;
    private WeeklyWeatherViewModel mWeeklyWeatherViewModel;

    private WeeklyOverviewDialog mWeeklyOverviewDialog;

    private AlarmManager alarmManager;
    private Intent alarmIntent;
    private PendingIntent pendingIntent;

    @Inject
    ViewModelFactory mViewModelFactory;

    private TextView mTemperature, mFeelsLike, mTemperatureMax, mTemperatureMin, mHumidity, mCityHeader, mWeatherCondition;
    String tempString,feelsLikeString, tempMaxString, tempMinString, humidityString, weatherConditionString, cityheaderString;
    private ImageView mTemperatureImageView;

    private ProgressBar mProgressBar;

    private Double mMonday, mTuesday, mWednesday, mThursday, mFriday, mSaturday, mSunday;

    private Button mSettingsBtn, mWeeklyOverviewBtn;

    private ConnectivityManager mConnectivityManager;
    private NetworkRequest mNetworkRequest;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private ConnectivityStatus mConnectionStatus = ConnectivityStatus.NO_INTERNET;

    private LocationManager locationManager;

    private double latitude, longitude;

    private TaskHandler mTaskHandler;
    private Handler mHandler;

    private ActivityResultLauncher<String> requestPermissionLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Log.d(TAG, "onCreate: oncreate");
        init();
        setupSharedPreference();
        getLocation();
        checkInternetConnectivity();

        if(mNotificationSwitch){
            setNotificationAlarm();
        }

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initObserver();
                initWeeklyObserver();
            }
        }, 2000);

    }

    /**
     * Function which is used to initialize all the widgets in the layout file
     */
    public void init() {
        mProgressBar = findViewById(R.id.progress_bar);
        mTemperature = findViewById(R.id.temperature_text_view);
        mFeelsLike = findViewById(R.id.feels_like_holder);
        mTemperatureMax = findViewById(R.id.temp_max_holder);
        mTemperatureMin = findViewById(R.id.temp_min_holder);
        mHumidity = findViewById(R.id.humidity_holder);
        mCityHeader = findViewById(R.id.city_header_text_view);
        mWeeklyOverviewBtn = findViewById(R.id.weekly_overview_button);
        mWeatherCondition = findViewById(R.id.weather_condition_text_view);
        mTemperatureImageView = findViewById(R.id.temperature_image_view);
        mSettingsBtn = findViewById(R.id.settings_button);

        mWeeklyOverviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWeeklyOverview();
            }
        });

        mSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });

        mWeeklyOverviewDialog = new WeeklyOverviewDialog();

        mWeatherViewModel = new ViewModelProvider(this, mViewModelFactory).get(WeatherViewModel.class);
        mWeeklyWeatherViewModel = new ViewModelProvider(this, mViewModelFactory).get(WeeklyWeatherViewModel.class);

        mTaskHandler = new TaskHandler();
    }

    public void openSettings(){
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    /**
     * Function to register a shared preference listener for the whole runtime of the application
     */
    public void setupSharedPreference(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUnits = sharedPreferences.getString(getString(R.string.preference_list_key),getString(R.string.preference_default_value));
        mNotificationSwitch = sharedPreferences.getBoolean(getString(R.string.notification_preference_key),false);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void showWeeklyOverview(){
        mWeeklyOverviewDialog.show(getSupportFragmentManager(), "weekly_overview_dialog_fragment");
    }

    @Override
    public void registerNetworkChangeListener() {
        mConnectivityManager.registerNetworkCallback(mNetworkRequest, mNetworkCallback);
        mConnectivityManager.requestNetwork(mNetworkRequest, mNetworkCallback, 2500);
    }

    @Override
    public void unregisterNetworkChangeListener() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    public void showProgressBar(){
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setElevation(10);
    }

    public void hideProgressBar(){
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    /**
     * Two Observer function to observe ViewModel which is used for fetching current weather (For both weekly and daily data)
     */
    public void initObserver() {
        Log.d(TAG, "initObserver: called observer");
        if (mConnectionStatus.equals(ConnectivityStatus.CONNECTED)) {
            if(mUnits == null){
                mUnits = "metric";
            }

            mWeatherViewModel.getData(latitude, longitude, mUnits).observe(this, new Observer<DataModel>() {
                @Override
                public void onChanged(DataModel dataModel) {
                    Log.d(TAG, "onChanged: entered onChange block");
                    showProgressBar();
                    displayWeatherData(
                            dataModel.getMain().getTemp().toString(),
                            dataModel.getMain().getFeelsLike().toString(),
                            dataModel.getMain().getTempMax().toString(),
                            dataModel.getMain().getTempMin().toString(),
                            dataModel.getMain().getHumidity().toString(),
                            dataModel.getName(),
                            dataModel.getWeather().get(0).getMain()
                    );

                    displayWeatherImage(dataModel.getWeather().get(0).getMain());
                    hideProgressBar();

                }
            });

        }
    }

    public void initWeeklyObserver(){
        Log.d(TAG, "initWeeklyObserver: entered weekly observer");

        if (mConnectionStatus.equals(ConnectivityStatus.CONNECTED)) {
            if(mUnits == null){
                mUnits = "metric";
            }

            mWeeklyWeatherViewModel.getWeeklyData(latitude, longitude, mUnits).observe(this, new Observer<DataModelWeekly>() {
                @Override
                public void onChanged(DataModelWeekly dataModelWeekly) {

                    mMonday = dataModelWeekly.getDaily().get(1).getTemp().getDay();
                    mTuesday = dataModelWeekly.getDaily().get(2).getTemp().getDay();
                    mWednesday = dataModelWeekly.getDaily().get(3).getTemp().getDay();
                    mThursday = dataModelWeekly.getDaily().get(4).getTemp().getDay();
                    mFriday = dataModelWeekly.getDaily().get(5).getTemp().getDay();
                    mSaturday = dataModelWeekly.getDaily().get(6).getTemp().getDay();
                    mSunday = dataModelWeekly.getDaily().get(7).getTemp().getDay();
                }
            });


        }
    }


    /**
     * function used to display weather data in the layout
     * @param temp
     * @param feelsLike
     * @param tempMax
     * @param tempMin
     * @param humidity
     * @param cityHeader
     */
    public void displayWeatherData(String temp, String feelsLike, String tempMax, String tempMin, String humidity, String cityHeader, String weatherCondition) {

        cityheaderString = cityHeader;

        if(mUnits.equals("metric")) {
            tempString = temp.trim() + " °C";
            feelsLikeString = feelsLike.trim() + " °C";
            tempMaxString = tempMax.trim() + " °C";
            tempMinString = tempMin.trim() + " °C";
        }

        else {
            tempString = temp.trim() + " °F";
            feelsLikeString = feelsLike.trim() + " °F";
            tempMaxString = tempMax.trim() + " °F";
            tempMinString = tempMin.trim() + " °F";
        }

        humidityString = humidity.trim() + " %";
        weatherConditionString = weatherCondition.trim();

        Log.d(TAG, "displayWeatherData: tempString " + tempString);

        mTemperature.setText(tempString);
        mFeelsLike.setText(feelsLikeString);
        mTemperatureMax.setText(tempMaxString);
        mTemperatureMin.setText(tempMinString);
        mHumidity.setText(humidityString);
        mCityHeader.setText(cityheaderString);
        mWeatherCondition.setText(weatherConditionString);

    }

    public void displayWeatherImage(String weatherCondition){
            //Rain, Drizzle, Thunderstorm
            if(weatherCondition.equals("Rain")||weatherCondition.equals("Drizzle")||weatherCondition.equals("Thunderstorm")){
                mTemperatureImageView.setImageResource(R.drawable.rain_image);
            }
            else if(weatherCondition.equals("Snow")){
                mTemperatureImageView.setImageResource(R.drawable.snow_image);
            }
            else{
                mTemperatureImageView.setImageResource(R.drawable.sunny_image);
            }
    }

    /**
     * Checks weather internet connection is available or not
     *
     * Three states are used to depict its behaviour i.e. 'CONNECTED' , 'DISCONNECTED' and 'NO_INTERNET'
     */
    @Override
    public void checkInternetConnectivity() {
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetworkRequest = new NetworkRequest.Builder().build();

        Runnable task = new Runnable() {
            @Override
            public void run() {
                mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        displayToast("Connected to internet");
                        mConnectionStatus = ConnectivityStatus.CONNECTED;
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        displayToast("Disconnected");
                        mConnectionStatus = ConnectivityStatus.DISCONNECTED;
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        displayToast("No internet connection available");
                        mConnectionStatus = ConnectivityStatus.NO_INTERNET;
                        showNetworkAlertDialog();
                    }
                };
            }
        };
        mTaskHandler.submit(task);
    }

    public void showNetworkAlertDialog(){
        AlertDialog.Builder networkAlertDialog = new AlertDialog.Builder(this);
        networkAlertDialog.setTitle("No internet connection!");
        networkAlertDialog.setMessage("Connect to the internet and retry or exit app");
        networkAlertDialog.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
                startActivity(getIntent());
            }
        });
        networkAlertDialog.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
                System.exit(0);
            }
        });

        networkAlertDialog.show();
    }

    public void displayToast(String message) {
        if (toastMessage != null) {
            toastMessage.cancel();
        }
        toastMessage = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toastMessage.show();
    }


    /**
     * Saves the current user location during application startup
     */
    @Override
    public void getLocation() {
        Log.d(TAG, "Entered getLocation()");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "getLocation: fine and corse location permission are not assigned in the manifest");
            } else {
                Log.d(TAG, "getLocation: first if block");
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(location != null) {

                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    Log.d(TAG, "getLocation: " + "latitude: " + location.getLatitude() + " longitude: " + location.getLongitude());
                }
                else{
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000, 0, this);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerNetworkChangeListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
        unregisterNetworkChangeListener();
    }


    /**
     * Functions for sending weekly overview data to the bottom sheet dialog
     */
    @Override
    public Double getMonday() {
        return mMonday;
    }

    @Override
    public Double getTuesday() {
        return mTuesday;
    }

    @Override
    public Double getWednesday() {
        return mWednesday;
    }

    @Override
    public Double getThursday() {
        return mThursday;
    }

    @Override
    public Double getFriday() {
        return mFriday;
    }

    @Override
    public Double getSaturday() {
        return mSaturday;
    }

    @Override
    public Double getSunday() {
        return mSunday;
    }

    @Override
    public String getUnits(){
        return mUnits;
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(getString(R.string.preference_list_key))){
            mUnits = sharedPreferences.getString(key, getString(R.string.preference_default_value));
        }
        else{
            mNotificationSwitch = sharedPreferences.getBoolean(key, Boolean.parseBoolean(getString(R.string.notification_preference_default_value)));
        }

        mWeatherViewModel.changeParams(latitude, longitude, mUnits);
        mWeeklyWeatherViewModel.changeWeeklyParams(latitude, longitude, mUnits);

        if(mNotificationSwitch){
            setNotificationAlarm();
        }
        else{
            cancelNotificationAlarm();
        }

        Log.d(TAG, "onSharedPreferenceChanged: called changes "+ mUnits + " , "+ mNotificationSwitch);


    }

    /**
     * Function to create a Job service for sending timely notifications locally (with the help of a broadcast receiver and alarm manager)
     * The alarm is displayed at approx 9 am daily (time is not 100% exact)
     */
    public void setNotificationAlarm(){

        if(alarmManager == null){
            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        }


            alarmIntent = new Intent(this, WeatherNotificationReceiver.class);
            alarmIntent.putExtra(UNITS_FLAG, mUnits);
            alarmIntent.putExtra(JOB_FLAG, mNotificationSwitch);
            pendingIntent = PendingIntent.getBroadcast(this, PENDING_INTENT_REQUEST_CODE, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Log.d(TAG, "setNotificationAlarm: creatorUid - " + pendingIntent.getCreatorUid());

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 10);
            calendar.set(Calendar.MINUTE, 30);
            calendar.set(Calendar.SECOND, 0);

            assert alarmManager != null;
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);


    }

    public void cancelNotificationAlarm(){
        AlarmManager cancelAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (pendingIntent != null) {
            PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, PENDING_INTENT_REQUEST_CODE, alarmIntent, PendingIntent.FLAG_NO_CREATE);
            if(cancelAlarmManager != null && pendingIntent != null) {
                Log.d(TAG, "cancelNotificationAlarm: cancelling for sure");
                alarmManager.cancel(cancelPendingIntent);
            }
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        locationManager.removeUpdates(this);
        Log.d(TAG, "onLocationChanged: onLocationChanged");

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        mWeatherViewModel.changeParams(latitude, longitude, mUnits);
        mWeeklyWeatherViewModel.changeWeeklyParams(latitude, longitude, mUnits);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}