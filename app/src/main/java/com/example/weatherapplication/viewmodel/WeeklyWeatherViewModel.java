package com.example.weatherapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.weatherapplication.network.model.weekly.DataModelWeekly;
import com.example.weatherapplication.viewmodel.repos.WeeklyRepository;

import javax.inject.Inject;

public class WeeklyWeatherViewModel extends ViewModel {

    private WeeklyRepository mWeeklyRepository;
    private LiveData<DataModelWeekly> mWeeklyData;

    @Inject
    public WeeklyWeatherViewModel(WeeklyRepository weeklyRepository){
        this.mWeeklyRepository = weeklyRepository;
    }

    public LiveData<DataModelWeekly> getWeeklyData(Double lat, Double lon, String unit){
        loadWeeklyData(lat, lon, unit);
        return mWeeklyData;
    }

    public void loadWeeklyData(Double lat, Double lon, String unit){
        mWeeklyData = mWeeklyRepository.fetchWeeklyData(lat,lon, unit);
    }
}
