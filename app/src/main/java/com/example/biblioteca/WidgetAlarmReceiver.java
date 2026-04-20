package com.example.biblioteca;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetAlarmReceiver extends BroadcastReceiver {// Receiver que se dispara cada 15 minutos para actualizar el widget

    private static final String TAG = "WidgetAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() - alarma disparada.");

        Context ctx = LanguageHelper.aplicarIdioma(context);
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.media_widget);
        views.setTextViewText(R.id.tvWidgetPendientes,
                ctx.getString(R.string.widget_cargando));

        ComponentName widget = new ComponentName(ctx, MediaWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
        int[] ids = manager.getAppWidgetIds(widget);
        Log.d(TAG, "Instancias encontradas: " + ids.length);
        manager.updateAppWidget(widget, views);

        MediaWidget.lanzarWorker(ctx);
    }
}