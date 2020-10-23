package com.example.weatherapplication.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class for creating a new thread for executing intensive tasks
 */
public class TaskHandler {

    public void submit(Runnable task){
        ExecutorService mExecutorService = Executors.newFixedThreadPool(3);
        mExecutorService.submit(task);
    }
}
