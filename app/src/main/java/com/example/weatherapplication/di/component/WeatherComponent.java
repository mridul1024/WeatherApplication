package com.example.weatherapplication.di.component;

import android.app.Application;

import com.example.weatherapplication.di.WeatherApplication;
import com.example.weatherapplication.di.network_module.NetworkModule;
import com.example.weatherapplication.di.viewmodel_module.ViewModelModule;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

/**
 * Declare the modules which will be used in Dagger component
 * [NOTE: ADD THE "AndroidSupportInjectionModule.class" IN ORDER TO CREATE THE COMPONENT]
 */
@Component(
        modules = {
                AndroidSupportInjectionModule.class,
                NetworkModule.class,
                ViewModelModule.class,
                WeatherContributor.class
        }
)
@Singleton
public interface WeatherComponent extends AndroidInjector<WeatherApplication> {

    @Component.Builder
    interface Builder{

        @BindsInstance
        Builder application(Application application);

        WeatherComponent build();
    }
}
