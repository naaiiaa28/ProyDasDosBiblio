package com.example.biblioteca;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import android.content.Context;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class PerfilActivity extends AppCompatActivity { //perfil del usuario

    private SesionManager sesion;

    private static final String KEY_PHOTO_PATH = "photo_path";

    private Uri photoURI;
    private File photoFile;

    //launcher para seleccionar imagen de galería. Devuelve un URI que copiamos a nuestro storage privado.
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            String ruta = copiarImagenAlStorage(uri);
                            if (ruta != null) mostrarYSubir(ruta);
                        }
                    });

    //launcher para pedir permiso de cámara. Si se concede, llama a lanzarCamara() que abre la cámara.
    private final ActivityResultLauncher<String> permisoCamaraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            lanzarCamara();
                        } else {
                            Toast.makeText(this,
                                    "Permiso de cámara denegado",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    //launcher para abrir la cámara. Devuelve un booleano de éxito, pero la foto se guarda directamente en photoURI.
    private final ActivityResultLauncher<Uri> cameraLauncher =
        registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    android.util.Log.d("CAMARA", "success=" + success);
                    android.util.Log.d("CAMARA", "photoFile=" + photoFile);
                    if (photoFile != null) {
                        android.util.Log.d("CAMARA", "exists=" + photoFile.exists() 
                            + " size=" + photoFile.length());
                    }
                    if (success && photoFile != null
                            && photoFile.exists()
                            && photoFile.length() > 0) {
                        mostrarYSubir(photoFile.getAbsolutePath());
                    } else {
                        Toast.makeText(this,
                                "No se ha guardado la foto",
                                Toast.LENGTH_SHORT).show();
                    }
                });



    @Override
    protected void attachBaseContext(Context newBase) {//idioma
        super.attachBaseContext(LanguageHelper.aplicarIdioma(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        if (savedInstanceState != null) {
            String path = savedInstanceState.getString(KEY_PHOTO_PATH);
            if (path != null) {
                photoFile = new File(path);
                photoURI = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        photoFile);
            }
        }

        sesion = new SesionManager(this);

        cargarDatosUsuario();
        cargarFotoPerfil();
        cargarTextoUI();

        findViewById(R.id.ivPerfilFoto).setOnClickListener(v -> elegirOrigenFoto());
        findViewById(R.id.btnCambiarFoto).setOnClickListener(v -> elegirOrigenFoto());
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> mostrarDialogoCerrarSesion());
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {//guardamos la ruta de la foto para restaurarla si el sistema destruye la actividad mientras se toma la foto
        super.onSaveInstanceState(outState);
        if (photoFile != null) {
            outState.putString(KEY_PHOTO_PATH, photoFile.getAbsolutePath());
        }
    }


    private void cargarTextoUI() {//cargamos el texto de los botones para que se actualice al cambiar el idioma
        ((TextView) findViewById(R.id.btnCambiarFoto))
                .setText(getString(R.string.anadir_imagen));
        ((TextView) findViewById(R.id.btnCerrarSesion))
                .setText(getString(R.string.cerrar_sesion));
        ((TextView) findViewById(R.id.btnVolver))
                .setText(getString(R.string.volver));
    }

    private void cargarDatosUsuario() {//cargamos los datos del usuario en los campos correspondientes
        ((TextView) findViewById(R.id.tvPerfilNombre)).setText(sesion.getNombre());
        ((TextView) findViewById(R.id.tvPerfilEmail)).setText(sesion.getEmail());
    }

    private void cargarFotoPerfil() {//cargamos la foto de perfil 
        String ruta = sesion.getRutaFoto();
        if (ruta != null && !ruta.isEmpty()) {
            Glide.with(this)
                    .load(ServidorConfig.BASE_URL + ruta + "?t=" + System.currentTimeMillis())
                    .circleCrop()
                    .into((ImageView) findViewById(R.id.ivPerfilFoto));
        }
    }


    private void elegirOrigenFoto() {//mostramos un diálogo para elegir entre cámara o galería
        new AlertDialog.Builder(this)
                .setTitle("Selecciona origen")
                .setItems(new String[]{"Cámara", "Galería"}, (dialog, which) -> {
                    if (which == 0) abrirCamara();
                    else abrirGaleria();
                })
                .show();
    }

    private void abrirGaleria() {
        // Lanza el selector de imágenes del sistema
        galleryLauncher.launch("image/*");
    }

    private void abrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Pedimos permiso; si se concede, el launcher llama a lanzarCamara()
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
        } else {
            lanzarCamara();
        }
    }

    private void lanzarCamara() {//creamos un archivo temporal para que la cámara guarde la foto, y obtenemos su URI con FileProvider
        try {
            photoFile = File.createTempFile(
                    "foto_" + System.currentTimeMillis(),
                    ".jpg",
                    getFilesDir()
            );

            photoURI = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );


            cameraLauncher.launch(photoURI);

        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarYSubir(String ruta) {
        // Mostrar la nueva foto de forma inmediata en el ImageView
        Glide.with(this)
            .load(new File(ruta))
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .circleCrop()
            .into((ImageView) findViewById(R.id.ivPerfilFoto));

        // Subir al servidor en segundo plano mediante WorkManager
        subirFoto(ruta);
    }


    private void subirFoto(String rutaLocal) {//preparamos los datos para el worker que subirá la foto al servidor
        int userId = sesion.getId();
        if (userId <= 0) {
            Toast.makeText(this, "Usuario inválido", Toast.LENGTH_LONG).show();
            return;
        }

        Data input = new Data.Builder()
                .putString("url",        ServidorConfig.URL_USUARIOS)
                .putString("accion",     "subir_foto")
                .putString("usuario_id", String.valueOf(userId))
                .putString("ruta_local", rutaLocal)
                .putString("campo",      "foto")
                .build();

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(ImagenWorker.class)
                        .setInputData(input)
                        .build();

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        String resultado = workInfo.getOutputData().getString("resultado");
                        if (resultado != null) {
                            procesarRespuesta(resultado);
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(request);
    }

    private void procesarRespuesta(String resultado) {//procesamos la respuesta del servidor tras subir la foto. Si es correcta, actualizamos la sesión con la nueva ruta de foto y recargamos la foto de perfil para mostrarla.
        try {
            JSONObject json = new JSONObject(resultado);
            if ("ok".equals(json.getString("resultado"))) {
                sesion.guardarSesion(
                        sesion.getId(),
                        sesion.getNombre(),
                        sesion.getEmail(),
                        json.getString("ruta_foto")
                );
                cargarFotoPerfil();
                Toast.makeText(this, "Foto actualizada", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String copiarImagenAlStorage(Uri uri) {//copiamos la imagen seleccionada de la galería a nuestro almacenamiento privado para poder subirla al servidor. Devolvemos la ruta del archivo copiado.
        try {
            InputStream input = getContentResolver().openInputStream(uri);

            File dir = new File(getFilesDir(), "fotos");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, UUID.randomUUID() + ".jpg");
            OutputStream output = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }

            output.close();
            input.close();

            return file.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void mostrarDialogoCerrarSesion() {//mostramos un diálogo de confirmación antes de cerrar sesión
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro?")
                .setPositiveButton("Sí", (d, w) -> {
                    sesion.cerrarSesion();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {//hacemos que el botón de volver en la barra de acción funcione igual que el botón de volver de la interfaz
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}