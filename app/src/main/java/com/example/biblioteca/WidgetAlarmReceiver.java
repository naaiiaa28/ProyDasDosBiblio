package com.example.biblioteca;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WidgetAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "WidgetAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarma disparada → lanzando worker");
        MediaWidget.lanzarWorker(LanguageHelper.aplicarIdioma(context));
    }
}