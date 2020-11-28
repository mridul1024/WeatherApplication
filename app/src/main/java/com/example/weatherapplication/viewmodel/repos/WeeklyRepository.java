package com.example.weatherapplication.viewmodel.repos;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.weatherapplication.network.WeatherService;
import com.example.weatherapplication.network.model.weekly.DataModelWeekly;
import com.example.weatherapplication.utils.DataState;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class WeeklyRepository {

    private static final String TAG = "WeeklyRepository";

    private WeatherService mWeatherService;
    private MediatorLiveData<DataModelWeekly> mDataModelWeekly;

    private static final String API_KEY = "e4c20ba5b57d2510f44502ce1da3686d";
    private static final String EXCLUDE = "current,minutely,hourly";

    @Inject
    public WeeklyRepository(WeatherService weatherService){
        this.mWeatherService = weatherService;
        this.mDataModelWeekly = new MediatorLiveData<>();
    }

    public MutableLiveData<DataModelWeekly> fetchWeeklyData(Double lat, Double lon, String unit){
        LiveData<DataState<DataModelWeekly>> weeklySource = LiveDataReactiveStreams.fromPublisher(
                mWeatherService.getWeatherWeeklyData(Double.toString(lat), Double.toString(lon), EXCLUDE, API_KEY, unit)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(DataState::Success)
                .onErrorReturn(DataState::Error)
        );

        mDataModelWeekly.addSource(weeklySource, new Observer<DataState<DataModelWeekly>>() {
            @Override
            public void onChanged(DataState<DataModelWeekly> dataModelWeeklyDataState) {
                if(dataModelWeeklyDataState.getError() != null){
                    Log.d(TAG, "onChanged: Error- "+ dataModelWeeklyDataState.getError().getLocalizedMessage());
                }
                else{
                    Log.d(TAG, "onChanged: Weekly data- "+ dataModelWeeklyDataState.getData());
                    mDataModelWeekly.setValue(dataModelWeeklyDataState.getData());
                    mDataModelWeekly.removeSource(weeklySource);
                }
            }
        });

        return mDataModelWeekly;
    }
}
