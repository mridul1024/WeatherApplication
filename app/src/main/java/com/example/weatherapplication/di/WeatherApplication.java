package com.example.weatherapplication.di;

import com.example.weatherapplication.di.component.DaggerWeatherComponent;

import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;

public class WeatherApplication extends DaggerApplication {
    /**
     * Creates an injectionModule for creating the dagger component class [NOTE: DECLARE IT IN THE MANIFEST FILE]
     */

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return DaggerWeatherComponent.builder().application(this).build();
    }
}
