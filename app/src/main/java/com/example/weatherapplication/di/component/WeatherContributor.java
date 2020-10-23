package com.example.weatherapplication.di.component;


import com.example.weatherapplication.fragments.WeeklyOverviewDialog;
import com.example.weatherapplication.ui.HomeActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class WeatherContributor {

    @ContributesAndroidInjector
    abstract HomeActivity contributeHomeActivity();

}
