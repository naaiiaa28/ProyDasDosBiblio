package com.example.biblioteca;

import android.content.Context;
import android.content.SharedPreferences;

public class SesionManager {//mantiene la sesion iniciad en el movil
    private static final String PREF_NAME = "sesion";
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SesionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void guardarSesion(int id, String nombre, String email, String rutaFoto) {
        editor.putInt("id", id);
        editor.putString("nombre", nombre);
        editor.putString("email", email);
        editor.putString("ruta_foto", rutaFoto != null ? rutaFoto : "");
        editor.putBoolean("logueado", true);
        editor.apply();
    }

    public boolean estaLogueado() {
        return prefs.getBoolean("logueado", false);
    }

    public int getId() { return prefs.getInt("id", -1); }
    public String getNombre() { return prefs.getString("nombre", ""); }
    public String getEmail() { return prefs.getString("email", ""); }
    public String getRutaFoto() { return prefs.getString("ruta_foto", ""); }

    public void cerrarSesion() {
        editor.clear();
        editor.apply();
    }
}