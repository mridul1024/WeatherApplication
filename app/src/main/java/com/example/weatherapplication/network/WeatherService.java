package com.example.weatherapplication.network;

import com.example.weatherapplication.network.model.DataModel;
import com.example.weatherapplication.network.model.weekly.DataModelWeekly;

import io.reactivex.Flowable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherService {

    //https://api.openweathermap.org/data/2.5/onecall?lat=26.144518&lon=91.736237&exclude=current,minutely,hourly&appid={your api key}&units={conversion units}

    @GET("weather")
    Flowable<DataModel> getWeatherByCoordinates(@Query("lat") String latitude,
                                                @Query("lon") String longitude,
                                                @Query("appid") String apiKey,
                                                @Query("units") String units);

    @GET("onecall")
    Flowable<DataModelWeekly> getWeatherWeeklyData(@Query("lat") String latitude,
                                            @Query("lon") String longitude,
                                            @Query("exclude") String exclude,
                                            @Query("appid") String apiKey,
                                            @Query("units") String units);
}
