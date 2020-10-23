package com.example.weatherapplication.di.viewmodel_module;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * This is a custom factory which is used to create and initialize "ViewModels" in the entire application
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    /**
     * A "Map" object which is used to keep track of each ViewModel and initialize it with the required provider in the key pair
     */
    private Map<Class<? extends ViewModel>, Provider<ViewModel>> mCreator;

    @Inject
    public ViewModelFactory(Map<Class<? extends ViewModel>, Provider<ViewModel>> creator){
        this.mCreator = creator;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        Provider<ViewModel> creator = mCreator.get(modelClass);

        if(creator == null){
            throw new RuntimeException("unknown model class" + modelClass);
        }
        else {
            return (T) creator.get();
        }
    }
}
