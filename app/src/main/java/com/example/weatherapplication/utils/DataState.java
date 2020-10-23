package com.example.weatherapplication.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Generic class for handling upstream errors in RxJava and stops it for propagating to main thread
 * @param <T>
 */
public final class DataState<T> {

    private T mData;
    private Throwable mError;

    private DataState(@Nullable T data, @Nullable Throwable error){
        this.mData = data;
        this.mError = error;
    }

    public static <T> DataState<T> Success(@NonNull T data){
        return new DataState<>(data, null);
    }

    public static <T> DataState<T> Error(@NonNull Throwable error){
        return new DataState<>(null, error);
    }

    public T getData(){
        return mData;
    }

    public Throwable getError(){
        return mError;
    }
}
