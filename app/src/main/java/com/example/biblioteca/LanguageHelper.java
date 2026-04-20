package com.example.biblioteca;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import java.util.Locale;

public class LanguageHelper {
//para organizar todoo el tema de los idiomas
    private static final String PREF_NAME = "settings";
    private static final String KEY_LANG = "language";

    public static Context aplicarIdioma(Context context) { //accede a settings y mira el idioma del movil, si este existe lo cambia sino por defecto español
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lang = prefs.getString(KEY_LANG, "es");
        return setLocale(context, lang);
    }

    public static void cambiarIdioma(Context context, String lang) {//asigna nuevo idioma pero no cambia lo que hace es volver a llamar a la pagina en la que este para aplifca el attach y asi cambia el idioma de preferencia
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();
    }

    public static String getIdioma(Context context) { //devuelve el idioma activo en ese moment
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, "es");
    }

    public static Context setLocale(Context context, String lang) { //cambia una confi
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }
}