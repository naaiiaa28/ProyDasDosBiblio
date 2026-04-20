package com.example.biblioteca;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity { //login que es la primera pantalla que aparece

    private EditText etEmail, etPassword;
    private SesionManager sesion;

    @Override
    protected void attachBaseContext(Context newBase) { //aplicar idioma
        super.attachBaseContext(LanguageHelper.aplicarIdioma(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sesion = new SesionManager(this);

        // Si ya está logueado va directo a MainActivity
        if (sesion.estaLogueado()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegistro = findViewById(R.id.tvRegistro);

        btnLogin.setOnClickListener(v -> login());
        tvRegistro.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() { //para iniciar sesión
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        String parametros = "accion=login&email=" + email + "&password=" + password;

        Data inputData = new Data.Builder()
                .putString("url", ServidorConfig.URL_USUARIOS)
                .putString("parametros", parametros)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ConexionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        String resultado = workInfo.getOutputData().getString("resultado");
                        procesarLogin(resultado);
                    }
                });

        WorkManager.getInstance(this).enqueue(request);
        Toast.makeText(this, "Iniciando sesión...", Toast.LENGTH_SHORT).show();
    }

    private void procesarLogin(String resultado) { //procesar la respuesta del servidor
        try {
            JSONObject json = new JSONObject(resultado);
            String res = json.getString("resultado");

            if (res.equals("ok")) {
                int id = Integer.parseInt(json.getString("id"));
                String nombre = json.getString("nombre");
                String email = json.getString("email");
                String rutaFoto = json.optString("ruta_foto", "");
                sesion.guardarSesion(id, nombre, email, rutaFoto);
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (res.equals("usuario_no_encontrado")) {
                Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
            } else if (res.equals("password_incorrecto")) {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
        }
    }
}