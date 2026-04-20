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

public class MediaWidget extends AppWidgetProvider { //widget para mostrar el número de pelis/series pendientes de devolver

    private static final String TAG = "MediaWidget";
    public static final String ACCION_ACTUALIZAR = "com.example.biblioteca.ACTUALIZAR_WIDGET";

    // Nombre único para el worker: evita que se encolen múltiples copias a la vez
    private static final String WORKER_NAME = "media_widget_worker";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) { //se llama cada vez que se actualiza el widget (cada 30 min por la alarma, o al agregarlo a la pantalla)
        Log.d(TAG, "onUpdate() llamado. Instancias: " + appWidgetIds.length);
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id,
                    context.getString(R.string.widget_cargando));
        }
        lanzarWorker(context);
    }

    @Override
    public void onEnabled(Context context) { //se llama la primera vez que se agrega el widget a la pantalla, o si se agrega otro después de haber eliminado el último
        Log.d(TAG, "onEnabled() - configurando alarma cada 30 min.");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WidgetAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 7475, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setRepeating(AlarmManager.RTC,
                System.currentTimeMillis() + 5000,
                1800000, pi); ///se actualiza cada 30 minutos (1800000 ms) y la primera vez a los 5 segundos (5000 ms) para pruebas rápidas
    }

    @Override
    public void onDisabled(Context context) {//se llama cuando se elimina el último widget de la pantalla, o si se elimina un widget y no quedan más
        Log.d(TAG, "onDisabled() - cancelando alarma.");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WidgetAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 7475, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }

    @Override
    public void onReceive(Context context, Intent intent) {//se llama cada vez que se recibe una acción, ya sea la de actualización periódica o la de click en el widget
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

            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            updateWidget(context, manager, widgetId,
                    context.getString(R.string.widget_cargando));
            lanzarWorker(context);
        }
    }

    public static void lanzarWorker(Context context) {//se llama desde el onUpdate y desde el onReceive al hacer click en el widget, para actualizar su contenido
        Log.d(TAG, "lanzarWorker()");
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(MediaWidgetWorker.class).build();
        WorkManager.getInstance(context)
                .enqueueUniqueWork(WORKER_NAME, ExistingWorkPolicy.REPLACE, request);
    }

    //método para actualizar el contenido del widget, se llama desde el worker cuando obtiene el número de pelis/series pendientes
    public static void updateWidget(Context context, AppWidgetManager manager,
                                    int appWidgetId, String texto) {
        Log.d(TAG, "updateWidget() id=" + appWidgetId + " texto='" + texto + "'");

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.media_widget);
        views.setTextViewText(R.id.tvWidgetPendientes, texto);

        Intent intent = new Intent(context, MediaWidget.class);
        intent.setAction(ACCION_ACTUALIZAR);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pi = PendingIntent.getBroadcast(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.tvWidgetPendientes, pi);
        views.setTextViewText(R.id.tvWidgetTitulo,context.getString(R.string.widget_titulo));

        manager.updateAppWidget(appWidgetId, views);
    }
}