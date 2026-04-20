package com.example.biblioteca;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {
    //para trabajar con las notificacioones de la barra de arriba

    private static final String CHANNEL_ID = "biblioteca_channel";
    private static int notifId = 0;

    //crea canal de notis
    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Biblioteca", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notificaciones de Mi Biblioteca");
            ctx.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    //Construye y lanza la notificación que aparece en la barra del móvil cuando se añade un item
    public static void mostrarNotificacion(Context ctx, String titulo, String tipoES) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        // Traducir tipo
        String tipoTraducido;
        if ("Película".equals(tipoES)) {
            tipoTraducido = ctx.getString(R.string.tipo_pelicula);
        } else {
            tipoTraducido = ctx.getString(R.string.tipo_serie);
        }

        Notification notif = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(tipoTraducido + " " + ctx.getString(R.string.notif_añadida))
            .setContentText("\"" + titulo + "\" " + ctx.getString(R.string.notif_mensaje))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build();

        NotificationManagerCompat.from(ctx).notify(notifId++, notif);
    }
}