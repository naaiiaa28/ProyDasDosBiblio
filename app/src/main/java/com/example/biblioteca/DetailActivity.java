package com.example.biblioteca;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import android.net.Uri;
import android.widget.ImageView;
import java.io.File;
import android.content.Context;
import org.json.JSONObject;
import com.bumptech.glide.Glide;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class DetailActivity extends AppCompatActivity { //muestra detalles de la película/serie seleccionada, con opción a editarla

    private SesionManager sesion;
    private int itemId;
    

    private  ActivityResultLauncher<Intent> editLauncher;
            

    @Override
    protected void attachBaseContext(Context newBase) { //para cambiar idioma 
        super.attachBaseContext(LanguageHelper.aplicarIdioma(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        editLauncher=registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            cargarDatos();
                        }
                    }
            );
        sesion = new SesionManager(this);
        itemId = getIntent().getIntExtra("id", -1);

        if (itemId == -1) { finish(); return; }

        cargarDatos();

        findViewById(R.id.btnDetalleEditar).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditActivity.class);
            intent.putExtra("item_id", itemId);
            editLauncher.launch(intent);
        });
    }

    // Pide al servidor los datos del item por su id
    private void cargarDatos() {
        String parametros = "accion=obtener_por_id&id=" + itemId + "&usuario_id=" + sesion.getId();

        Data inputData = new Data.Builder()
                .putString("url", ServidorConfig.URL_MEDIA)
                .putString("parametros", parametros)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ConexionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        String resultado = workInfo.getOutputData().getString("resultado");
                        mostrarDatos(resultado);
                    }
                });

        WorkManager.getInstance(this).enqueue(request);
    }

    // Parsea el JSON y rellena todos los campos de la pantalla
    private void mostrarDatos(String resultado) {
        try {
            JSONObject obj = new JSONObject(resultado);

            // Si el servidor devuelve error cerramos la actividad
            if (obj.has("resultado") && !"ok".equals(obj.optString("resultado"))) {
                finish();
                return;
            }

            String titulo = obj.optString("titulo");
            setTitle(titulo);
            ((TextView) findViewById(R.id.tvDetalleTitulo)).setText(titulo);

            // Tipo traducido al idioma activo para mostrar; el valor en BD siempre es español
            String tipoES = obj.optString("tipo");
            String tipoTraducido;
            switch (tipoES) {
                case "Película": tipoTraducido = getString(R.string.tipo_pelicula); break;
                case "Serie":    tipoTraducido = getString(R.string.tipo_serie); break;
                default:         tipoTraducido = tipoES; break;
            }
            ((TextView) findViewById(R.id.tvDetalleTipo))
                .setText(tipoTraducido + " • " + obj.optString("genero"));

            float puntuacion = (float) obj.optDouble("puntuacion", 0);
            ((TextView) findViewById(R.id.tvDetallePuntuacion))
                .setText("⭐ " + (int) puntuacion + "/5");

            // Color y texto del estado, igual que antes
            TextView tvEstado = findViewById(R.id.tvDetalleEstado);
            String estadoES = obj.optString("estado");
            switch (estadoES) {
                case "Visto":       tvEstado.setTextColor(0xFF4CAF50); break;
                case "En progreso": tvEstado.setTextColor(0xFFFF9800); break;
                case "Pendiente":   tvEstado.setTextColor(0xFF9E9E9E); break;
            }
            switch (estadoES) {
                case "Visto":       tvEstado.setText(getString(R.string.estado_visto)); break;
                case "En progreso": tvEstado.setText(getString(R.string.estado_en_progreso)); break;
                case "Pendiente":   tvEstado.setText(getString(R.string.estado_pendiente)); break;
                default:            tvEstado.setText(estadoES);
            }

            String resumen = obj.optString("resumen");
            ((TextView) findViewById(R.id.tvDetalleResumen)).setText(
                resumen.isEmpty() ? getString(R.string.sin_resumen) : resumen);

            String comentario = obj.optString("comentario");
            ((TextView) findViewById(R.id.tvDetalleComentario)).setText(
                comentario.isEmpty() ? getString(R.string.sin_comentario) : comentario);

            ((TextView) findViewById(R.id.tvDetalleProgreso)).setText(
                getString(R.string.progreso_texto,
                    obj.optInt("temporada_actual", 0),
                    obj.optInt("capitulo_actual", 0),
                    obj.optInt("temporadas_totales", 0)));

            // Dentro de mostrarDatos(String resultado)
            String rutaImagen = obj.optString("ruta_imagen"); // Trae "imagenes/archivo.jpg"
            ImageView ivImagen = findViewById(R.id.ivDetalleImagen);

            if (rutaImagen != null && !rutaImagen.isEmpty()) {
                ivImagen.setVisibility(View.VISIBLE);
                String urlCompleta = ServidorConfig.BASE_URL + rutaImagen + "?t=" + System.currentTimeMillis();

                Glide.with(this)
                    .load(urlCompleta)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .skipMemoryCache(true)
                    .into(ivImagen);
            } else {
                ivImagen.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        // Al volver de editar recargamos desde el servidor para ver los cambios
        if (res == RESULT_OK) cargarDatos();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {// Para que el botón de volver en la barra funcione
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}