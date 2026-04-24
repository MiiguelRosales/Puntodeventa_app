package com.example.puntodeventa;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class GestorTraducciones {
    private static final String ARCHIVO = "traductor.json";
    private static final String IDIOMA_ES = "es";
    private static final String IDIOMA_EN = "en";

    private static final Map<String, Map<String, String>> diccionarios = new HashMap<>();
    private static String idiomaActual = IDIOMA_ES;
    private static boolean inicializado = false;

    private GestorTraducciones() {
    }

    public static void inicializar(Context context) {
        if (inicializado) {
            return;
        }
        try {
            String contenido = leerAsset(context, ARCHIVO);
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(contenido, JsonObject.class);

            Map<String, String> es = gson.fromJson(json.getAsJsonObject(IDIOMA_ES), Map.class);
            Map<String, String> en = gson.fromJson(json.getAsJsonObject(IDIOMA_EN), Map.class);

            diccionarios.put(IDIOMA_ES, es);
            diccionarios.put(IDIOMA_EN, en);
            inicializado = true;
        } catch (Exception e) {
            inicializado = false;
        }
    }

    public static void alternarIdioma(Context context) {
        inicializar(context);
        idiomaActual = IDIOMA_ES.equals(idiomaActual) ? IDIOMA_EN : IDIOMA_ES;
    }

    public static String obtenerIdiomaActual() {
        return idiomaActual;
    }

    public static String obtenerTexto(Context context, String clave, String porDefecto) {
        inicializar(context);
        Map<String, String> idioma = diccionarios.get(idiomaActual);
        if (idioma == null) {
            return porDefecto;
        }
        String valor = idioma.get(clave);
        return valor != null ? valor : porDefecto;
    }

    private static String leerAsset(Context context, String archivo) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(archivo))
        );
        StringBuilder sb = new StringBuilder();
        String linea;
        while ((linea = reader.readLine()) != null) {
            sb.append(linea);
        }
        reader.close();
        return sb.toString();
    }
}
