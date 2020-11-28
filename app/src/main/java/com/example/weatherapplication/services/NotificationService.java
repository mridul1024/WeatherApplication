package com.example.weatherapplication.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.example.weatherapplication.R;
import com.example.weatherapplication.network.WeatherService;
import com.example.weatherapplication.network.model.DataModel;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class NotificationService extends JobService
implements LocationListener {

    private static final String TAG = "NotificationService";

    private NotificationManager mNotificationManager;
    private String mNotificationMessage;

    private double latitude, longitude;


    private WeatherService mWeatherService;


    private static final String API_KEY = "e4c20ba5b57d2510f44502ce1da3686d";
    private static final String NOTIFICATION_CHANNEL = "notification_channel";
    private static final String NOTIFICATION_CHANNEL_ID = "100";

    private static final String JOB_FLAG = "job_flag_key";
    private static final String UNITS_FLAG = "units_flag_key";
    private static final String NOTIFICATION_MESSAGE = "notification_message_key";

    private boolean jobSwitch = false;
    private LiveData<DataModel> data;
    private MediatorLiveData<DataModel> mediatorData;

    private LocationManager locationManager;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "onStartJob: starting job ");
        mediatorData = new MediatorLiveData<>();
        getLocationCoordinates();
        createNotificationChannel();
        performNotificationTask(jobParameters);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        jobSwitch = false;
        Log.d(TAG, "onStopJob: stopping job");
        return true;
    }

    public void createNotificationChannel() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL, NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setDescription("Hey there!");
        mNotificationManager.createNotificationChannel(notificationChannel);
    }

    public void sendNotification(String contentString) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notification.setContentTitle("Weather app notification")
                .setContentText(contentString)
                .setSmallIcon(R.drawable.ic_launcher_background);
        mNotificationManager.notify(1, notification.build());
    }

    public void performNotificationTask(JobParameters params) {

        jobSwitch = params.getExtras().getBoolean(JOB_FLAG);
        String units = params.getExtras().getString(UNITS_FLAG);
        Log.d(TAG, "performNotificationTask: unit - "+ units);
        Log.d(TAG, "performNotificationTask: switch -"+ jobSwitch);
        if (jobSwitch) {
            Retrofit mRetrofit = new Retrofit.Builder()
                    .baseUrl("http://api.openweathermap.org/data/2.5/")
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            mWeatherService = mRetrofit.create(WeatherService.class);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: inside run block");
                     data = LiveDataReactiveStreams.fromPublisher(
                            mWeatherService.getWeatherByCoordinates(Double.toString(latitude), Double.toString(longitude), API_KEY, units)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                    );

                     data.observeForever(new Observer<DataModel>() {
                         @Override
                         public void onChanged(DataModel dataModel) {
                             Log.d(TAG, "onChanged: inside observe forever block");
                             String appendUnit;
                             assert units != null;
                             if(units.equals("metric")){
                                 appendUnit = " °C";
                             }
                             else{
                                 appendUnit = " °F";
                             }

                            sendNotification("The temperature is "+ dataModel.getMain().getTempMax() + appendUnit);
                            Log.d(TAG, "onChanged: data ="+ dataModel.getMain().getTempMax());
                            onStopJob(params);
                         }
                     });
                }
            }, 2500);

        } else {
            onStopJob(params);
        }
    }

    public void getLocationCoordinates() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        assert locationManager != null;
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if(location == null){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, this);

        }

        else{
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            Log.d(TAG, "getLocationCoordinates: lat long: "+ latitude + " " + longitude);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        locationManager.removeUpdates(this);
        latitude = location.getLatitude();
        longitude = location.getLongitude();
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
