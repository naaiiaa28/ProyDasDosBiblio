package com.example.biblioteca;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImagenWorker extends Worker { // Worker para subir imagenes al servidor (media o fotoperfil)

    public ImagenWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String urlStr      = getInputData().getString("url");
        String accion      = getInputData().getString("accion");   // "subir_imagen" o "subir_foto"
        String id          = getInputData().getString("id");
        String usuarioId   = getInputData().getString("usuario_id");
        String rutaLocal   = getInputData().getString("ruta_local");
        String campo       = getInputData().getString("campo");    // "imagen" o "foto"

        String boundary = "----Boundary" + System.currentTimeMillis();
        String lineEnd  = "\r\n";
        String twoHyphens = "--";

        try {
            File archivo = new File(rutaLocal);
            if (!archivo.exists()) return Result.failure();

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            // Campo: accion
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"accion\"" + lineEnd + lineEnd);
            dos.writeBytes(accion + lineEnd);

            // Campo: id (solo si no es null)
            if (id != null) {
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"id\"" + lineEnd + lineEnd);
                dos.writeBytes(id + lineEnd);
            }

            // Campo: usuario_id
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"usuario_id\"" + lineEnd + lineEnd);
            dos.writeBytes(usuarioId + lineEnd);

            // El fichero
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"" + campo + "\"; filename=\"" + archivo.getName() + "\"" + lineEnd);
            dos.writeBytes("Content-Type: image/jpeg" + lineEnd + lineEnd);

            FileInputStream fis = new FileInputStream(archivo);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) dos.write(buffer, 0, len);
            fis.close();

            dos.writeBytes(lineEnd + twoHyphens + boundary + twoHyphens + lineEnd);
            dos.flush();
            dos.close();

            int code = conn.getResponseCode();
            if (code == 200) {
                java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                Data output = new Data.Builder()
                    .putString("resultado", sb.toString())
                    .build();
                return Result.success(output);
            }
            return Result.failure();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}