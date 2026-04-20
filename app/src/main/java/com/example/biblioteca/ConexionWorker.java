package com.example.biblioteca;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConexionWorker extends Worker { // Worker para manejar las conexiones HTTP en segundo plano, evitando bloquear la UI

    public ConexionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String urlStr = getInputData().getString("url");
        String accion = getInputData().getString("accion");

        // Si la acción es subir imagen, usamos el método especial Multipart
        if ("subir_imagen".equals(accion)) {
            return subirImagenMultipart(urlStr);
        }

        // Para el resto de acciones (obtener, insertar, etc) seguimos con el método normal
        return peticionEstandar(urlStr);
    }

    private Result peticionEstandar(String urlStr) { // Para acciones como obtener, insertar, actualizar, eliminar
        String parametros = getInputData().getString("parametros");
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            PrintWriter out = new PrintWriter(conn.getOutputStream());
            out.print(parametros);
            out.close();

            if (conn.getResponseCode() == 200) {
                String res = leerRespuesta(conn.getInputStream());
                return Result.success(new Data.Builder().putString("resultado", res).build());
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Result.failure();
    }

    private Result subirImagenMultipart(String urlStr) { // Para la acción de subir imagen, necesitamos enviar un multipart/form-data
        String boundary = "*****";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        try {
            String id = getInputData().getString("id");
            String usuarioId = getInputData().getString("usuario_id");
            String rutaLocal = getInputData().getString("ruta_local");
            File archivo = new File(rutaLocal);

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            // Campo accion
            escribirCampo(dos, boundary, "accion", "subir_imagen");
            // Campo id
            escribirCampo(dos, boundary, "id", id);
            // Campo usuario_id
            escribirCampo(dos, boundary, "usuario_id", usuarioId);

            // Archivo imagen
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"imagen\"; filename=\"" + archivo.getName() + "\"" + lineEnd);
            dos.writeBytes(lineEnd);

            FileInputStream fis = new FileInputStream(archivo);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) dos.write(buffer, 0, length);
            
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            
            fis.close();
            dos.flush();
            dos.close();

            if (conn.getResponseCode() == 200) {
                String res = leerRespuesta(conn.getInputStream());
                return Result.success(new Data.Builder().putString("resultado", res).build());
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Result.failure();
    }

    //para escribir un campo de texto en el multipart/form-data
    private void escribirCampo(DataOutputStream dos, String boundary, String nombre, String valor) throws IOException {
        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n");
        dos.writeBytes(valor + "\r\n");
    }

    // para leer la respuesta del servidor
    private String leerRespuesta(InputStream is) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder total = new StringBuilder();
        for (String line; (line = r.readLine()) != null; ) total.append(line);
        return total.toString();
    }
}