package com.example.biblioteca;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import android.content.SharedPreferences;

public class MediaWidget extends AppWidgetProvider {

    private static final String TAG = "MediaWidget";
    public static final String ACCION_ACTUALIZAR = "com.example.biblioteca.ACTUALIZAR_WIDGET";
    private static final String WORKER_NAME = "media_widget_worker";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate() llamado. Instancias: " + appWidgetIds.length);
        SharedPreferences prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE);
        String ultimoTexto = prefs.getString("ultimo_texto", context.getString(R.string.widget_cargando));
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id, ultimoTexto);
        }
    }

public static void updateWidget(Context context, AppWidgetManager manager,
                                int appWidgetId, String texto) {
    Log.d(TAG, "updateWidget() id=" + appWidgetId + " texto='" + texto + "'");

    context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            .edit().putString("ultimo_texto", texto).apply();

    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.media_widget);
    views.setTextViewText(R.id.tvWidgetPendientes, texto);
    views.setTextViewText(R.id.tvWidgetTitulo, context.getString(R.string.widget_titulo));

    Intent intent = new Intent(context, MediaWidget.class);
    intent.setAction(ACCION_ACTUALIZAR);
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
    PendingIntent pi = PendingIntent.getBroadcast(context, appWidgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    views.setOnClickPendingIntent(R.id.tvWidgetPendientes, pi);

    manager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled() - configurando alarma cada 30 min.");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WidgetAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 7475, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setRepeating(AlarmManager.RTC,
                System.currentTimeMillis() + 5000,
                1800000, pi);
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled() - cancelando alarma.");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WidgetAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 7475, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() acción recibida: " + intent.getAction());
        super.onReceive(context, intent);

        if (ACCION_ACTUALIZAR.equals(intent.getAction())) {
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.d(TAG, "ACCION_ACTUALIZAR para widgetId=" + widgetId);

            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.w(TAG, "widgetId inválido, ignorando.");
                return;
            }

            // Muestra cargando y lanza el worker al tocar el widget
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            updateWidget(context, manager, widgetId,
                    context.getString(R.string.widget_cargando));
            lanzarWorker(context);
        }
    }

    public static void lanzarWorker(Context context) {
        Log.d(TAG, "lanzarWorker()");
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(MediaWidgetWorker.class).build();
        WorkManager.getInstance(context)
                .enqueueUniqueWork(WORKER_NAME, ExistingWorkPolicy.REPLACE, request);
    }

}