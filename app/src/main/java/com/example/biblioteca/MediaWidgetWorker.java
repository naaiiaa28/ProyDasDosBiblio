package com.example.biblioteca;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MediaWidgetWorker extends Worker {//worker que se encarga de obtener el número de pelis/series pendientes de devolver y actualizar el widget, se lanza desde el MediaWidget cada vez que se actualiza o se hace click en el widget
//los logs eran para pruebas
    private static final String TAG = "MediaWidgetWorker";

    public MediaWidgetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork() iniciado");
        // Contexto con el idioma que el usuario tiene seleccionado en la app
        Context context = LanguageHelper.aplicarIdioma(getApplicationContext());

        //  Verificar sesión activa y obtener usuarioId
        SharedPreferences prefs = context.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        int usuarioId = prefs.getInt("id", -1);
        Log.d(TAG, "usuarioId leído de SharedPreferences: " + usuarioId);

        if (usuarioId == -1) {
            Log.w(TAG, "No hay sesión activa (id == -1). Mostrando '—' en el widget.");
            actualizarWidget(context, "—");
            return Result.success();
        }

        //preparar conexión HTTP
        HttpURLConnection conn = null;
        try {
            String urlStr = ServidorConfig.URL_MEDIA;
            Log.d(TAG, "URL destino: " + urlStr);

            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String params = "accion=filtrar_estado"
                    + "&usuario_id=" + usuarioId
                    + "&estado=Pendiente";
            Log.d(TAG, "Parámetros POST: " + params);

            OutputStream os = conn.getOutputStream();
            os.write(params.getBytes("UTF-8"));
            os.flush();
            os.close();

            // Leer respuesta
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Código de respuesta HTTP: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Servidor respondió con error: " + responseCode);
                actualizarWidget(context, "Error servidor (" + responseCode + ")");
                // Reintenta automáticamente con backoff exponencial
                return Result.retry();
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
            reader.close();

            String jsonStr = json.toString().trim();
            Log.d(TAG, "JSON recibido: " + jsonStr);

            if (jsonStr.isEmpty()) {
                Log.w(TAG, "El servidor devolvió una respuesta vacía.");
                actualizarWidget(context, context.getString(R.string.widget_sin_pendientes));
                return Result.success();
            }

            // Parsear JSON y contar pendientes
            int pendientes = 0;
            StringBuilder sb = new StringBuilder();

            if (!jsonStr.equals("[]")) {
                JSONArray array = new JSONArray(jsonStr);
                pendientes = array.length();
                Log.d(TAG, "Número de elementos pendientes: " + pendientes);

                for (int i = 0; i < array.length(); i++) {
                    String titulo = array.getJSONObject(i).optString("titulo", "(sin título)");
                    Log.d(TAG, "  [" + i + "] " + titulo);
                    sb.append("• ").append(titulo);
                    if (i < array.length() - 1) sb.append("\n");
                }
            } else {
                Log.d(TAG, "JSON vacío: no hay pendientes.");
            }

            // Construir texto final para el widget
            String textoFinal;
            if (pendientes == 0) {
                textoFinal = context.getString(R.string.widget_sin_pendientes);
            } else {
                textoFinal = context.getString(R.string.widget_pendientes, pendientes)
                        + "\n" + sb.toString();
            }
            Log.d(TAG, "Texto final para el widget: " + textoFinal);

            actualizarWidget(context, textoFinal);
            return Result.success();

        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "Timeout - reintentando", e);
            // Muestra cargando y deja que WorkManager reintente con backoff exponencial
            actualizarWidget(context, context.getString(R.string.widget_cargando));
            return Result.retry();
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "Host desconocido: " + ServidorConfig.URL_MEDIA, e);
            actualizarWidget(context, context.getString(R.string.widget_cargando));
            return Result.retry();
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error parseando JSON", e);
            actualizarWidget(context, "Error de formato");
            return Result.failure();
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado en doWork()", e);
            actualizarWidget(context, context.getString(R.string.widget_cargando));
            return Result.retry();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void actualizarWidget(Context context, String texto) {//método para actualizar el contenido del widget, se llama desde el worker cuando obtiene el número de pelis/series pendientes
        Log.d(TAG, "actualizarWidget() → " + texto);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, MediaWidget.class);
        int[] ids = manager.getAppWidgetIds(thisWidget);
        Log.d(TAG, "Instancias del widget: " + ids.length);
        for (int id : ids) {
            MediaWidget.updateWidget(context, manager, id, texto);
        }
    }
}