package com.example.weatherapplication.di.viewmodel_module;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.weatherapplication.viewmodel.WeeklyWeatherViewModel;
import com.example.weatherapplication.viewmodel.repos.Repository;
import com.example.weatherapplication.viewmodel.WeatherViewModel;
import com.example.weatherapplication.viewmodel.repos.WeeklyRepository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dagger.MapKey;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;

@Module
public class ViewModelModule {

    /**
     * This is custom annotation @ViewModelKey that will be used on a class to Map it as a value in the Map object of ViewModel-custom
     * factory
     */
    @Documented
    @MapKey
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface ViewModelKey{
        Class<? extends ViewModel> value();
    }

    @Provides
    ViewModelProvider.Factory provideViewModelFactory(ViewModelFactory viewModelFactory){
        return viewModelFactory;
    }

    /**
     * Declare a "Provider" method for each ViewModel that will be used in the application and annotate it with the
     * custom annotation @ViewModelKey
     */
    @Provides
    @IntoMap
    @ViewModelKey(WeatherViewModel.class)
    ViewModel provideWeatherViewModel(Repository repository){
        return new WeatherViewModel(repository);
    }

    @Provides
    @IntoMap
    @ViewModelKey(WeeklyWeatherViewModel.class)
    ViewModel provideWeeklyWeatherViewModel(WeeklyRepository weeklyRepository){
        return new WeeklyWeatherViewModel(weeklyRepository);
    }
}
