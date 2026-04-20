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

public class MediaWidgetWorker extends Worker {

    private static final String TAG = "MediaWidgetWorker";

    public MediaWidgetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork() iniciado");
        Context context = LanguageHelper.aplicarIdioma(getApplicationContext());

        SharedPreferences prefs = context.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        int usuarioId = prefs.getInt("id", -1);
        Log.d(TAG, "usuarioId: " + usuarioId);

        if (usuarioId == -1) {
            Log.w(TAG, "Sin sesión activa, mostrando '—'");
            actualizarWidget(context, "—");
            return Result.success();
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(ServidorConfig.URL_MEDIA);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String params = "accion=filtrar_estado"
                    + "&usuario_id=" + usuarioId
                    + "&estado=Pendiente";

            OutputStream os = conn.getOutputStream();
            os.write(params.getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "HTTP: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                actualizarWidget(context, "Error servidor (" + responseCode + ")");
                return Result.failure();
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
                actualizarWidget(context, context.getString(R.string.widget_sin_pendientes));
                return Result.success();
            }

            int pendientes = 0;
            StringBuilder sb = new StringBuilder();

            if (!jsonStr.equals("[]")) {
                JSONArray array = new JSONArray(jsonStr);
                pendientes = array.length();
                for (int i = 0; i < array.length(); i++) {
                    String titulo = array.getJSONObject(i).optString("titulo", "(sin título)");
                    sb.append("• ").append(titulo);
                    if (i < array.length() - 1) sb.append("\n");
                }
            }

            String textoFinal;
            if (pendientes == 0) {
                textoFinal = context.getString(R.string.widget_sin_pendientes);
            } else {
                textoFinal = context.getString(R.string.widget_pendientes, pendientes)
                        + "\n" + sb.toString();
            }

            actualizarWidget(context, textoFinal);
            return Result.success();

        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "Timeout", e);
            actualizarWidget(context, "Sin conexión");
            return Result.failure();
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "Host desconocido", e);
            actualizarWidget(context, "Sin conexión");
            return Result.failure();
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error JSON", e);
            actualizarWidget(context, "Error de formato");
            return Result.failure();
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado", e);
            actualizarWidget(context, "Error");
            return Result.failure();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void actualizarWidget(Context context, String texto) {
        Log.d(TAG, "actualizarWidget() → " + texto);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, MediaWidget.class);
        int[] ids = manager.getAppWidgetIds(thisWidget);
        for (int id : ids) {
            MediaWidget.updateWidget(context, manager, id, texto);
        }
    }
}