package com.example.weatherapplication.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.weatherapplication.network.model.DataModel;
import com.example.weatherapplication.viewmodel.repos.Repository;

import javax.inject.Inject;

public class WeatherViewModel extends ViewModel {
    private static final String TAG = "WeatherViewModel";

    private Repository mRepository;
    private LiveData<DataModel> data;

    @Inject
    public WeatherViewModel(Repository repository){
        this.mRepository = repository;
        Log.d(TAG, "WeatherViewModel: successfully created");
    }

    public LiveData<DataModel> getData(double lat, double lon, String unit){
        loadData(lat, lon, unit);
        return data;
    }

    public void loadData(double lat, double lon, String unit){
        data = mRepository.fetchData(lat, lon, unit);
    }
}
