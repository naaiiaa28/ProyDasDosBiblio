package com.example.biblioteca;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {//registro

    private EditText etNombre, etEmail, etPassword;

    @Override
    protected void attachBaseContext(Context newBase) {//idioam
        super.attachBaseContext(LanguageHelper.aplicarIdioma(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnRegistrar = findViewById(R.id.btnRegistrar);

        btnRegistrar.setOnClickListener(v -> registrar());
    }

    private void registrar() {//registro de usuario
        
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String parametros = "accion=registro&nombre=" + nombre +
                "&email=" + email + "&password=" + password;

        Data inputData = new Data.Builder()
        .putString("url", ServidorConfig.URL_USUARIOS)
        .putString("parametros", "accion=registro&nombre=" + nombre + "&email=" + email + "&password=" + password)
        .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ConexionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        String resultado = workInfo.getOutputData().getString("resultado");
                        procesarRegistro(resultado);
                    }
                });

        WorkManager.getInstance(this).enqueue(request);
        Toast.makeText(this, "Registrando...", Toast.LENGTH_SHORT).show();
    }

    private void procesarRegistro(String resultado) {//procesar resultado del registro
        try {
            JSONObject json = new JSONObject(resultado);
            String res = json.getString("resultado");

            if (res.equals("ok")) {
                Toast.makeText(this, "Registro exitoso. Inicia sesión.",
                        Toast.LENGTH_SHORT).show();
                finish();
            } else if (res.equals("email_existente")) {
                Toast.makeText(this, "Ese email ya está registrado",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error al registrar", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
        }
    }
}