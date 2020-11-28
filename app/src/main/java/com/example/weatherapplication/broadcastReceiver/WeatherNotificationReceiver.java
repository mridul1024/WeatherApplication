package com.example.weatherapplication.broadcastReceiver;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.util.Log;

import com.example.weatherapplication.services.NotificationService;

public class WeatherNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "WeatherNotificationRece";
    private static final String JOB_FLAG = "job_flag_key";
    private static final String UNITS_FLAG = "units_flag_key";

    @Override
    public void onReceive(Context context, Intent intent) {

        String units = intent.getStringExtra(UNITS_FLAG);
        boolean notificationSwitch = intent.getBooleanExtra(JOB_FLAG, false);
        Log.d(TAG, "onReceive: broadcast units - "+ units);

        if(notificationSwitch){
            PersistableBundle bundle = new PersistableBundle();
            bundle.putBoolean(JOB_FLAG, true);
            bundle.putString(UNITS_FLAG, units);

            ComponentName serviceComponent = new ComponentName(context, NotificationService.class);
            JobInfo jobInfo = new JobInfo.Builder(1, serviceComponent)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(bundle)
                    .build();

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            assert jobScheduler != null;
            int schedulerCode = jobScheduler.schedule(jobInfo);
            if(schedulerCode == JobScheduler.RESULT_SUCCESS){
                Log.d(TAG, "onReceive: service success");
            }
            else if(schedulerCode == JobScheduler.RESULT_FAILURE){
                Log.d(TAG, "onReceive: service failed");
            }
        }
    }
}
