package com.example.biblioteca;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.net.Uri;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.content.Context;
import org.json.JSONObject;
import com.bumptech.glide.Glide;
import android.widget.Toast;


public class AddEditActivity extends AppCompatActivity {

    private SesionManager sesion;
    private MediaItem itemEditar = null;      // guardamos el item completo al editar
    private int itemId = -1;                  // id del item si venimos a editar

    private EditText etTitulo, etGenero, etResumen, etComentario;
    private EditText etTemporadas, etTemporadaActual, etCapituloActual;
    private RatingBar ratingBar;
    private Spinner spinnerTipo, spinnerEstado;
    private LinearLayout layoutSerie;
    private String rutaImagenSeleccionada = null;
    private static final int REQUEST_IMAGEN = 200;
    private ImageView ivPreview;  

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.aplicarIdioma(newBase));
    } //para poenr el idioma correspondiente

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        sesion = new SesionManager(this);

        etTitulo          = findViewById(R.id.etTitulo);
        etGenero          = findViewById(R.id.etGenero);
        etResumen         = findViewById(R.id.etResumen);
        etComentario      = findViewById(R.id.etComentario);
        etTemporadas      = findViewById(R.id.etTemporadas);
        etTemporadaActual = findViewById(R.id.etTemporadaActual);
        etCapituloActual  = findViewById(R.id.etCapituloActual);
        ratingBar         = findViewById(R.id.ratingBar);
        spinnerTipo       = findViewById(R.id.spinnerTipo);
        spinnerEstado     = findViewById(R.id.spinnerEstado);
        layoutSerie       = findViewById(R.id.layoutSerie);
        ivPreview         = findViewById(R.id.ivPreviewImagen);  

        spinnerTipo.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item,
            new String[]{getString(R.string.tipo_pelicula), getString(R.string.tipo_serie)}));

        spinnerEstado.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item,
            new String[]{getString(R.string.estado_visto), getString(R.string.estado_en_progreso), getString(R.string.estado_pendiente)}));

        // Si viene un id por Intent estamos editando, pedimos los datos al servidor
        itemId = getIntent().getIntExtra("item_id", -1);
        if (itemId != -1) {
            setTitle("Editar");
            cargarItemRemoto(itemId);
        } else {
            setTitle("Añadir");
        }

        spinnerTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long i) {
                layoutSerie.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());

        findViewById(R.id.btnSeleccionarImagen).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGEN);
        });
    }

    // Pide al servidor los datos del item para rellenar el formulario al editar
    private void cargarItemRemoto(int id) {
        String parametros = "accion=obtener_por_id&id=" + id + "&usuario_id=" + sesion.getId();

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
                        procesarItemRecibido(resultado);
                    }
                });

        WorkManager.getInstance(this).enqueue(request);
    }

    // Convierte el JSON del servidor en un MediaItem y rellena el formulario
    private void procesarItemRecibido(String resultado) {
        try {
            JSONObject obj = new JSONObject(resultado);
            MediaItem item = new MediaItem();
            item.setId(obj.getInt("id"));
            item.setTitulo(obj.optString("titulo"));
            item.setTipo(obj.optString("tipo"));
            item.setGenero(obj.optString("genero"));
            item.setEstado(obj.optString("estado"));
            item.setPuntuacion((float) obj.optDouble("puntuacion", 0));
            item.setResumen(obj.optString("resumen"));
            item.setComentario(obj.optString("comentario"));
            item.setTemporadasTotales(obj.optInt("temporadas_totales", 0));
            item.setTemporadaActual(obj.optInt("temporada_actual", 0));
            item.setCapituloActual(obj.optInt("capitulo_actual", 0));
            item.setFechaAdicion(obj.optString("fecha_adicion"));
            item.setRutaImagen(obj.optString("ruta_imagen"));
            itemEditar = item;
            rellenarFormulario(item);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rellenarFormulario(MediaItem item) { //formulario
        etTitulo.setText(item.getTitulo());
        etGenero.setText(item.getGenero());
        etResumen.setText(item.getResumen());
        etComentario.setText(item.getComentario());
        ratingBar.setRating(item.getPuntuacion());

        // Verificar que ivPreview no sea null antes de usar Glide. Glide para guardar la imagen en caché y mostrarla, evitando problemas de carga lenta o falta de memoria.
        if (ivPreview != null && item.getRutaImagen() != null && !item.getRutaImagen().isEmpty()) {
            Glide.with(this)
                .load(ServidorConfig.BASE_URL + item.getRutaImagen())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(ivPreview);
            ivPreview.setVisibility(View.VISIBLE);
        }
        else {
            ivPreview.setVisibility(View.INVISIBLE);
        }

        // Siempre comparamos contra los valores en español que guarda el servidor
        String[] tiposES = {"Película", "Serie"};
        for (int i = 0; i < tiposES.length; i++) {
            if (tiposES[i].equals(item.getTipo())) spinnerTipo.setSelection(i);
        }

        String[] estadosES = {"Visto", "En progreso", "Pendiente"};
        for (int i = 0; i < estadosES.length; i++) {
            if (estadosES[i].equals(item.getEstado())) spinnerEstado.setSelection(i);
        }

        if ("Serie".equals(item.getTipo())) { //si es serie tiene mas campos
            layoutSerie.setVisibility(View.VISIBLE);
            etTemporadas.setText(String.valueOf(item.getTemporadasTotales()));
            etTemporadaActual.setText(String.valueOf(item.getTemporadaActual()));
            etCapituloActual.setText(String.valueOf(item.getCapituloActual()));
        }
    }

    private void guardar() { //guarda la info del formulario
        String titulo = etTitulo.getText().toString().trim();
        if (titulo.isEmpty()) {
            etTitulo.setError(getString(R.string.titulo_obligatorio));
            return;
        }

        // Tipo y estado siempre en español hacia el servidor
        int posTipo = spinnerTipo.getSelectedItemPosition();
        String tipo = posTipo == 0 ? "Película" : "Serie";

        int posEstado = spinnerEstado.getSelectedItemPosition();
        String[] estadosES = {"Visto", "En progreso", "Pendiente"};
        String estado = estadosES[posEstado];

        String fechaAdicion = new SimpleDateFormat("yyyy-MM-dd HH:mm",
                Locale.getDefault()).format(new Date());

        // Construye los parámetros POST según si es inserción o actualización
        StringBuilder parametros = new StringBuilder();
        if (itemEditar == null) {
            parametros.append("accion=insertar");
        } else {
            parametros.append("accion=actualizar&id=").append(itemEditar.getId());
        }

        parametros.append("&usuario_id=").append(sesion.getId())
                  .append("&titulo=").append(Uri.encode(titulo))
                  .append("&tipo=").append(Uri.encode(tipo))
                  .append("&genero=").append(Uri.encode(etGenero.getText().toString()))
                  .append("&resumen=").append(Uri.encode(etResumen.getText().toString()))
                  .append("&comentario=").append(Uri.encode(etComentario.getText().toString()))
                  .append("&puntuacion=").append(ratingBar.getRating())
                  .append("&estado=").append(Uri.encode(estado))
                  .append("&fecha_adicion=").append(Uri.encode(fechaAdicion));

        if ("Serie".equals(tipo)) {
            parametros.append("&temporadas_totales=").append(parseIntSafe(etTemporadas.getText().toString()))
                      .append("&temporada_actual=").append(parseIntSafe(etTemporadaActual.getText().toString()))
                      .append("&capitulo_actual=").append(parseIntSafe(etCapituloActual.getText().toString()));
        } else {
            // Si es película mandamos 0 para que el servidor no deje nulos
            parametros.append("&temporadas_totales=0&temporada_actual=0&capitulo_actual=0");
        }


        Data inputData = new Data.Builder()
                .putString("url", ServidorConfig.URL_MEDIA)
                .putString("parametros", parametros.toString())
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ConexionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        String resultado = workInfo.getOutputData().getString("resultado");
                        procesarGuardado(resultado, titulo, tipo);
                    }
                });

        WorkManager.getInstance(this).enqueue(request);

        setResult(RESULT_OK);
        finish();
        MediaWidget.lanzarWorker(this);
    }

    // Procesa la respuesta del servidor tras guardar el item, y si se ha subido una imagen, lanza su worker
    private void procesarGuardado(String resultado, String titulo, String tipo) {
    try {
        JSONObject json = new JSONObject(resultado);

        if ("ok".equals(json.getString("resultado"))) {

            int idFinal = (itemEditar == null) ? json.getInt("id") : itemEditar.getId();

            // 🔥 SUBIR IMAGEN SI EXISTE
            if (rutaImagenSeleccionada != null && !rutaImagenSeleccionada.isEmpty()) {

                Toast.makeText(this, "Subiendo imagen...", Toast.LENGTH_SHORT).show();

                Data dataImagen = new Data.Builder()
                        .putString("url", ServidorConfig.URL_MEDIA)
                        .putString("accion", "subir_imagen")
                        .putString("id", String.valueOf(idFinal))
                        .putString("usuario_id", String.valueOf(sesion.getId()))
                        .putString("ruta_local", rutaImagenSeleccionada)
                        .putString("campo", "imagen")
                        .build();

                OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ImagenWorker.class)
                        .setInputData(dataImagen)
                        .build();

                WorkManager.getInstance(this).getWorkInfoByIdLiveData(req.getId())
                        .observe(this, workInfo -> {
                            if (workInfo != null && workInfo.getState().isFinished()) {

                                String resultadoImg = workInfo.getOutputData().getString("resultado");

                                procesarSubidaImagen(resultadoImg);
                            }
                        });

                WorkManager.getInstance(this).enqueue(req);
            }

            // Notificación solo si es nuevo
            if (itemEditar == null) {
                NotificationHelper.mostrarNotificacion(this, titulo, tipo);
            }

            MediaWidget.lanzarWorker(this);
        }

    } catch (Exception e) {
        e.printStackTrace();
    }   
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {// para el botón de volver atrás en la ActionBar
        if (menuItem.getItemId() == android.R.id.home) {
            mostrarDialogoDescartar();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void mostrarDialogoDescartar() { // muestra un diálogo para confirmar si se quieren descartar los cambios
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.descartar_titulo))
            .setMessage(getString(R.string.descartar_mensaje))
            .setPositiveButton(getString(R.string.salir), (d, w) -> finish())
            .setNegativeButton(getString(R.string.seguir_editando), null)
            .show();
    }

    private int parseIntSafe(String s) { // intenta convertir a entero, si falla devuelve 0
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    @Override
    public void onBackPressed() { // al pulsar el botón de atrás del sistema, también mostramos el diálogo de descartar
        mostrarDialogoDescartar();
    }

   @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { // resultado de seleccionar imagen
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGEN && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String rutaCopia = copiarImagenAlStorage(uri);

            if (rutaCopia != null) {
                rutaImagenSeleccionada = rutaCopia;

                
                Glide.with(this)
                        .load(new File(rutaCopia))
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivPreview);

                ivPreview.setVisibility(View.VISIBLE);
            }
        }
    }


    private String copiarImagenAlStorage(Uri uri) { // copia la imagen seleccionada al almacenamiento interno de la app y devuelve la ruta absoluta
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            File dir = new File(getFilesDir(), "imagenes");
            if (!dir.exists()) dir.mkdirs();

            File archivo = new File(dir, UUID.randomUUID().toString() + ".jpg");

            OutputStream output = new FileOutputStream(archivo);

            byte[] buffer = new byte[4096]; 
            int len;

            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }

            output.close();
            input.close();

            return archivo.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void procesarSubidaImagen(String resultado) { // procesa la respuesta del servidor tras subir la imagen, y si ha ido bien, muestra la imagen en el formulario
    try {
        JSONObject json = new JSONObject(resultado);

        if ("ok".equals(json.getString("resultado"))) {

            String ruta = json.getString("ruta_imagen");

            

            Glide.with(this)
                .load(ServidorConfig.BASE_URL + ruta)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .into(ivPreview);

            Toast.makeText(this, "Imagen subida correctamente", Toast.LENGTH_SHORT).show();

            setResult(RESULT_OK);
            finish();

        } else {
            Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_LONG).show();
        }

    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "Respuesta inválida del servidor", Toast.LENGTH_LONG).show();
    }
    }
}