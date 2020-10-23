package com.example.weatherapplication.ui;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.weatherapplication.R;
import com.example.weatherapplication.di.viewmodel_module.ViewModelFactory;
import com.example.weatherapplication.fragments.WeeklyOverviewDialog;
import com.example.weatherapplication.listeners.LocationChangeListener;
import com.example.weatherapplication.listeners.NetworkChangeListener;
import com.example.weatherapplication.network.model.DataModel;
import com.example.weatherapplication.network.model.weekly.DataModelWeekly;
import com.example.weatherapplication.services.NotificationService;
import com.example.weatherapplication.utils.ConnectivityStatus;
import com.example.weatherapplication.utils.TaskHandler;
import com.example.weatherapplication.viewmodel.WeatherViewModel;
import com.example.weatherapplication.viewmodel.WeeklyWeatherViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;


public class HomeActivity extends DaggerAppCompatActivity
        implements
        NetworkChangeListener, LocationChangeListener,
        WeeklyOverviewDialog.PassWeeklyData,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "HomeActivity";

    private static final String NOTIFICATION_MESSAGE = "notification_message_key";
    private static final String JOB_FLAG = "job_flag_key";
    private static final String UNITS_FLAG = "units_flag_key";

    private String mUnits;
    private boolean mNotificationSwitch;

    private WeatherViewModel mWeatherViewModel;
    private WeeklyWeatherViewModel mWeeklyWeatherViewModel;

    private WeeklyOverviewDialog mWeeklyOverviewDialog;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        init();
        setupSharedPreference();
        getLocation();
        checkInternetConnectivity();

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initObserver();
                initWeeklyObserver();
                serviceInitializer();
            }
        }, 2000);
    }

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
    }

    public void hideProgressBar(){
        mProgressBar.setVisibility(View.GONE);
    }

    /**
     * Observer function to observe ViewModel which is used for fetching current weather
     */
    public void initObserver() {
        Log.d(TAG, "initObserver: called observer");
        showProgressBar();
        if (mConnectionStatus.equals(ConnectivityStatus.CONNECTED)) {
            if(mUnits == null){
                mUnits = "metric";
            }
            mWeatherViewModel.getData(latitude, longitude, mUnits).observe(this, new Observer<DataModel>() {
                @Override
                public void onChanged(DataModel dataModel) {
                    Log.d(TAG, "onChanged: entered onChange block");
                    hideProgressBar();
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
                    }
                };
            }
        };
        mTaskHandler.submit(task);
    }

    public void displayToast(String message) {
        Log.d(TAG, "displayToast: toast called - " + message.trim());
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
                if (location == null) {
                    Log.d(TAG, "getLocation: location is null");
                }
                displayToast("latitude: " + location.getLatitude() + " longitude: " + location.getLongitude() + " ");
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Log.d(TAG, "getLocation: " + "latitude: " + location.getLatitude() + " longitude: " + location.getLongitude());
                registerLocationChangeListener();
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
        unregisterNetworkChangeListener();
    }

    /**
     * Function to keep track of user location changes
     */
    @Override
    public void registerLocationChangeListener() {
        LocationListener locationChangeListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "onLocationChanged: location changed ");
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                initObserver();
                initWeeklyObserver();
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
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //      ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 80, locationChangeListener);
    }

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
        displayToast(mUnits);
        displayToast(Boolean.toString(mNotificationSwitch));
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                initObserver();
                initWeeklyObserver();
                serviceInitializer();
            }
        }, 2500);
    }

    public void serviceInitializer(){
        PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(JOB_FLAG, mNotificationSwitch);
        bundle.putString(UNITS_FLAG, mUnits);

        ComponentName serviceComponent = new ComponentName(this, NotificationService.class);
        JobInfo jobInfo = new JobInfo.Builder(1, serviceComponent)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(bundle)
                .setPeriodic(15*60*1000)
                .build();

        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        assert jobScheduler != null;
        int schedulerCode = jobScheduler.schedule(jobInfo);
        if(schedulerCode == JobScheduler.RESULT_SUCCESS){
            Log.d(TAG, "serviceInitializer: service success");
        }
        else if(schedulerCode == JobScheduler.RESULT_FAILURE){
            Log.d(TAG, "serviceInitializer: service failed");
        }
    }

}