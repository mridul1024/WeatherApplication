package com.example.weatherapplication.fragments;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.weatherapplication.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


public class WeeklyOverviewDialog extends BottomSheetDialogFragment{
    private static final String TAG = "WeeklyOverviewDialog";

    private String mon, tue, wed, thur, fri, sat, sun, unit;
    private TextView mMonday, mTuesday, mWednesday, mThursday, mFriday, mSaturday, mSunday;
    private View mView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.weekly_overview_layout, container, false);
        init();
        return mView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        PassWeeklyData locationData = (PassWeeklyData) context;
        Log.d(TAG, "onAttach: Monday "+ locationData.getMonday().toString() + " monday " + locationData.getMonday());
        storeWeeklyData(
                locationData.getMonday().toString(),
                locationData.getTuesday().toString(),
                locationData.getWednesday().toString(),
                locationData.getThursday().toString(),
                locationData.getFriday().toString(),
                locationData.getSaturday().toString(),
                locationData.getSunday().toString(),
                locationData.getUnits()
        );
    }

    public void init(){
        mMonday = mView.findViewById(R.id.monday_text_view);
        mTuesday = mView.findViewById(R.id.tuesday_text_view);
        mWednesday = mView.findViewById(R.id.wednesday_text_view);
        mThursday = mView.findViewById(R.id.thursday_text_view);
        mFriday = mView.findViewById(R.id.friday_text_view);
        mSaturday = mView.findViewById(R.id.saturday_text_view);
        mSunday = mView.findViewById(R.id.sunday_text_view);

        displayData();
    }


    public void displayData(){
        mMonday.setText(mon.trim());
        mTuesday.setText(tue.trim());
        mWednesday.setText(wed.trim());
        mThursday.setText(thur.trim());
        mFriday.setText(fri.trim());
        mSaturday.setText(sat.trim());
        mSunday.setText(sun.trim());
    }

    public void storeWeeklyData(String monday, String tuesday, String wednesday, String thursday, String friday, String saturday, String sunday, String unit){
        this.unit = unit;
        if(this.unit.equals("metric")){
            mon = monday + " °C";
            tue = tuesday + " °C";
            wed = wednesday + " °C";
            thur = thursday + " °C";
            fri = friday + " °C";
            sat = saturday + " °C";
            sun = sunday + " °C";
        }
        else{
            mon = monday + " °F";
            tue = tuesday + " °F";
            wed = wednesday + " °F";
            thur = thursday + " °F";
            fri = friday + " °F";
            sat = saturday + " °F";
            sun = sunday + " °F";
        }
    }

    public interface PassWeeklyData{
        Double getMonday();
        Double getTuesday();
        Double getWednesday();
        Double getThursday();
        Double getFriday();
        Double getSaturday();
        Double getSunday();
        String getUnits();
    }
}
