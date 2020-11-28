package com.example.weatherapplication.viewmodel.repos;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.weatherapplication.network.WeatherService;
import com.example.weatherapplication.network.model.DataModel;
import com.example.weatherapplication.utils.DataState;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class Repository {
    private static final String TAG = "Repository";

    private WeatherService mWeatherService;
    private MediatorLiveData<DataModel> mDataModel;

    private static final String API_KEY = "e4c20ba5b57d2510f44502ce1da3686d";

    @Inject
    public Repository(WeatherService weatherService){
        this.mWeatherService = weatherService;
        mDataModel = new MediatorLiveData<>();
    }

    /**
     * Communicates with the REST API to fetch data and converts it into live data
     * @param lat
     * @param lon
     * @return
     */
    public MutableLiveData<DataModel> fetchData(Double lat, Double lon, String unit){
        Log.d(TAG, "fetchData: lat "+lat+" lon "+lon+" apiKey "+ API_KEY + " units "+ unit);
        LiveData<DataState<DataModel>> source = LiveDataReactiveStreams.fromPublisher(
                mWeatherService.getWeatherByCoordinates(Double.toString(lat), Double.toString(lon), API_KEY, unit)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(DataState::Success)
                .onErrorReturn(DataState::Error)
        );

        mDataModel.addSource(source, new Observer<DataState<DataModel>>() {
            @Override
            public void onChanged(DataState<DataModel> dataModelDataState) {
                if(dataModelDataState.getError() != null){
                    Log.d(TAG, "Error: "+dataModelDataState.getError().getLocalizedMessage());
                }
                else{
                    Log.d(TAG, "onChanged: data -" + dataModelDataState.getData().getMain().getTemp());
                    mDataModel.setValue(dataModelDataState.getData());
                    mDataModel.removeSource(source);
                }
            }
        });

        return mDataModel;
    }


}