package com.example.biblioteca;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

import androidx.work.PeriodicWorkRequest;
import com.example.biblioteca.RecordatorioWorker;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MediaAdapter adapter;
    private List<MediaItem> lista = new ArrayList<>();
    private TextView tvVacio;
    private SesionManager sesion;
    private String filtroEstado = null; // null = todos

    @Override
    protected void attachBaseContext(Context newBase) {//cambiar idioma
        super.attachBaseContext(LanguageHelper.aplicarIdioma(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sesion = new SesionManager(this);

        // Si no hay sesión activa manda al login antes de hacer nada más
        if (!sesion.estaLogueado()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.toolbar_principal));

        //boton para abrir el mapa
        findViewById(R.id.btnMapa).setOnClickListener(v -> {startActivity(new Intent(this, MapaActivity.class));});

        // Botón idioma
        findViewById(R.id.btnIdioma).setOnClickListener(v -> mostrarDialogoIdioma());
        actualizarBotonIdioma();

        //boton para ver el perfil
        findViewById(R.id.btnPerfil).setOnClickListener(v -> startActivity(new Intent(this, PerfilActivity.class)));

        NotificationHelper.createChannel(this);

        tvVacio = findViewById(R.id.tvVacio);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MediaAdapter(lista, new MediaAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaItem item) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra("id", item.getId());
                startActivity(intent);
            }
            @Override
            public void onEdit(MediaItem item) {
                Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                intent.putExtra("item_id", item.getId());
                startActivityForResult(intent, 100);
            }
            @Override
            public void onDelete(MediaItem item) {
                mostrarDialogoEliminar(item);
            }
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fabAdd).setOnClickListener(v ->
            startActivityForResult(new Intent(this, AddEditActivity.class), 100));

        configurarFiltros();
        cargarListaRemota(); // carga inicial desde servidor

        PeriodicWorkRequest workRequest =
            new PeriodicWorkRequest.Builder(
                    RecordatorioWorker.class,
                    15, TimeUnit.MINUTES
            ).build();

        WorkManager.getInstance(this).enqueue(workRequest);
    }

    // Pide al servidor la lista de items del usuario logueado,
    // usando la acción correcta según haya filtro de estado o no
    private void cargarListaRemota() {
        String parametros;
        if (filtroEstado == null) {
            // Sin filtro: pide todos los elementos del usuario
            parametros = "accion=obtener_todos&usuario_id=" + sesion.getId();
        } else {
            // Con filtro: usa la acción filtrar_estado del servidor
            parametros = "accion=filtrar_estado"
                    + "&usuario_id=" + sesion.getId()
                    + "&estado=" + filtroEstado;
        }

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
                        procesarLista(resultado);
                    }
                });

        WorkManager.getInstance(this).enqueue(request);
    }

    // Convierte el JSON del servidor en objetos MediaItem y actualiza el adapter
    private void procesarLista(String resultado) {
        lista = new ArrayList<>();
        if (resultado == null || resultado.equals("null") || resultado.isEmpty() || resultado.equals("[]")) {
            adapter.actualizarLista(lista);
            actualizarVista();
            return;
        }
        try {
            JSONArray array = new JSONArray(resultado);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                MediaItem item = new MediaItem();
                item.setId(Integer.parseInt(obj.optString("id", "0")));
                item.setTitulo(obj.optString("titulo"));
                item.setTipo(obj.optString("tipo"));
                item.setGenero(obj.optString("genero"));
                item.setEstado(obj.optString("estado"));
                item.setPuntuacion(Float.parseFloat(obj.optString("puntuacion", "0")));
                item.setResumen(obj.optString("resumen"));
                item.setComentario(obj.optString("comentario"));
                item.setTemporadasTotales(Integer.parseInt(obj.optString("temporadas_totales", "0")));
                item.setTemporadaActual(Integer.parseInt(obj.optString("temporada_actual", "0")));
                item.setCapituloActual(Integer.parseInt(obj.optString("capitulo_actual", "0")));
                item.setFechaAdicion(obj.optString("fecha_adicion"));
                item.setRutaImagen(obj.optString("ruta_imagen"));
                lista.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        adapter.actualizarLista(lista);
        actualizarVista();
    }

    private void mostrarDialogoEliminar(MediaItem item) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.eliminar_titulo))
            .setMessage(String.format(getString(R.string.eliminar_mensaje), item.getTitulo()))
            .setPositiveButton(getString(R.string.eliminar), (d, w) -> eliminarRemoto(item))
            .setNegativeButton(getString(R.string.cancelar), null)
            .show();
    }

    // Manda la petición de borrado al servidor y recarga la lista si va bien
    private void eliminarRemoto(MediaItem item) {
        String parametros = "accion=eliminar&id=" + item.getId()
                + "&usuario_id=" + sesion.getId();

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
                        cargarListaRemota(); // refresca siempre tras eliminar
                    }
                });

        WorkManager.getInstance(this).enqueue(request);
    }

    private boolean spinnerListo = false; // para evitar que se ejecute el filtro al iniciar, ya que onCreate ya carga la lista sin filtro

    private void configurarFiltros() { // configura el spinner de filtros y su listener
        Spinner spinner = findViewById(R.id.spinnerFiltro);

        String[] opciones = {
            getString(R.string.filtro_todos),
            getString(R.string.estado_visto),
            getString(R.string.estado_en_progreso),
            getString(R.string.estado_pendiente)
        };

        spinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                opciones
        ));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                // Evitar que se ejecute al iniciar (la carga inicial ya la hace onCreate)
                if (!spinnerListo) {
                    spinnerListo = true;
                    return;
                }

                // Los valores deben coincidir exactamente con los de la BD
                String[] estadosDB = {"Visto", "En progreso", "Pendiente"};
                filtroEstado = (position == 0) ? null : estadosDB[position - 1];

                cargarListaRemota();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void actualizarVista() { // muestra el mensaje de "lista vacía" si no hay items, o el RecyclerView si hay
        tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res == RESULT_OK) cargarListaRemota();
    }

    private boolean primeraVez = true; // para evitar que se recargue la lista al volver de una actividad, ya que onCreate ya la carga inicialmente

    @Override
    protected void onResume() {
        super.onResume();
        if (!primeraVez) cargarListaRemota();
        primeraVez = false;
    }

    private void mostrarDialogoIdioma() {
        String idiomaActual = LanguageHelper.getIdioma(this);
        String[] opciones = {"Español", "English", "Euskera"};
        int seleccionado;
        switch (idiomaActual) {
            case "en": seleccionado = 1; break;
            case "eu": seleccionado = 2; break;
            default:   seleccionado = 0; break;
        }
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.cambiar_idioma))
            .setSingleChoiceItems(opciones, seleccionado, (dialog, which) -> {
                String lang;
                switch (which) {
                    case 1:  lang = "en"; break;
                    case 2:  lang = "eu"; break;
                    default: lang = "es"; break;
                }
                LanguageHelper.cambiarIdioma(this, lang);
                dialog.dismiss();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            })
            .show();
    }

    private void actualizarBotonIdioma() {
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnIdioma =
            findViewById(R.id.btnIdioma);
        String lang = LanguageHelper.getIdioma(this);
        switch (lang) {
            case "en": btnIdioma.setText("EN"); break;
            case "eu": btnIdioma.setText("EU"); break;
            default:   btnIdioma.setText("ES"); break;
        }
    }
}