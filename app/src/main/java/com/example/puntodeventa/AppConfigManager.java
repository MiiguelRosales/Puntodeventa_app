package com.example.puntodeventa;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class AppConfigManager {

    private static final String ARCHIVO_ASSET = "appconfig.json";
    private static final String ARCHIVO_INTERNO = "appconfig_runtime.json";

    private static Configuracion cache;

    private AppConfigManager() {
    }

    public static Configuracion obtenerConfiguracion(Context context) {
        if (cache != null) {
            return cache;
        }

        try {
            String contenido = leerInterno(context);
            if (contenido == null || contenido.trim().isEmpty()) {
                contenido = leerAsset(context, ARCHIVO_ASSET);
            }
            cache = normalizar(new Gson().fromJson(contenido, Configuracion.class));
        } catch (Exception e) {
            cache = Configuracion.porDefecto();
        }

        if (cache == null) {
            cache = Configuracion.porDefecto();
        }

        return cache;
    }

    public static boolean guardarConfiguracion(Context context, String nombreTienda, String colorEnfasis,
                                               String tamanoTexto) {
        try {
            Configuracion actual = obtenerConfiguracion(context);
            Configuracion nueva = new Configuracion();
            nueva.apariencia = new Apariencia();
            nueva.negocio = new Negocio();

            nueva.apariencia.temaOscuro = actual.apariencia.temaOscuro;
            nueva.apariencia.colorEnfasis = (colorEnfasis == null || colorEnfasis.trim().isEmpty())
                    ? actual.apariencia.colorEnfasis
                    : colorEnfasis.trim();
            nueva.apariencia.tamanoTexto = normalizarTamanoTexto(tamanoTexto);
            nueva.negocio.nombreTienda = (nombreTienda == null || nombreTienda.trim().isEmpty())
                    ? actual.negocio.nombreTienda
                    : nombreTienda.trim();

            String json = new Gson().toJson(nueva);
            try (FileOutputStream outputStream = context.openFileOutput(ARCHIVO_INTERNO, Context.MODE_PRIVATE)) {
                outputStream.write(json.getBytes(StandardCharsets.UTF_8));
            }

            cache = normalizar(nueva);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void aplicarTemaDesdeConfig(Context context) {
        Configuracion config = obtenerConfiguracion(context);
        if (config.apariencia.temaOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public static void aplicarEscalaTexto(View raiz, String tamanoTexto) {
        if (raiz == null) {
            return;
        }

        float escala = resolverEscalaTexto(tamanoTexto);
        aplicarEscalaTextoRecursivo(raiz, escala);
    }

    public static void aplicarColorEnfasis(Context context, String colorHex, Button... botones) {
        if (botones == null || botones.length == 0) {
            return;
        }

        int color = resolverColorEnfasis(context, colorHex);
        ColorStateList tint = ColorStateList.valueOf(color);

        for (Button boton : botones) {
            if (boton != null) {
                boton.setBackgroundTintList(tint);
            }
        }
    }

    public static String obtenerNombreTienda(Context context) {
        Configuracion config = obtenerConfiguracion(context);
        String nombre = config.negocio.nombreTienda;
        if (nombre == null || nombre.trim().isEmpty()) {
            return "PUNTO DE VENTA";
        }
        return nombre.trim();
    }

    private static int resolverColorEnfasis(Context context, String colorHex) {
        if (colorHex == null || colorHex.trim().isEmpty()) {
            return ContextCompat.getColor(context, R.color.button_start);
        }

        try {
            return Color.parseColor(colorHex.trim());
        } catch (IllegalArgumentException ex) {
            return ContextCompat.getColor(context, R.color.button_start);
        }
    }

    private static float resolverEscalaTexto(String tamanoTexto) {
        if (tamanoTexto == null) {
            return 1f;
        }

        String valor = tamanoTexto.trim().toLowerCase();
        if ("pequeno".equals(valor)) {
            return 0.9f;
        }
        if ("grande".equals(valor)) {
            return 1.15f;
        }
        return 1f;
    }

    private static void aplicarEscalaTextoRecursivo(View vista, float escala) {
        if (vista instanceof TextView) {
            TextView tv = (TextView) vista;
            Object valorBase = tv.getTag(R.id.tag_base_text_size);
            float tamanoBase;

            if (valorBase instanceof Float) {
                tamanoBase = (Float) valorBase;
            } else {
                tamanoBase = tv.getTextSize();
                tv.setTag(R.id.tag_base_text_size, tamanoBase);
            }

            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanoBase * escala);
        }

        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) {
                aplicarEscalaTextoRecursivo(grupo.getChildAt(i), escala);
            }
        }
    }

    private static String leerAsset(Context context, String archivo) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(archivo)));
        StringBuilder sb = new StringBuilder();
        String linea;
        while ((linea = reader.readLine()) != null) {
            sb.append(linea);
        }
        reader.close();
        return sb.toString();
    }

    private static String leerInterno(Context context) {
        File file = new File(context.getFilesDir(), ARCHIVO_INTERNO);
        if (!file.exists()) {
            return null;
        }

        try (FileInputStream inputStream = context.openFileInput(ARCHIVO_INTERNO)) {
            byte[] buffer = new byte[(int) file.length()];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead <= 0) {
                return null;
            }
            return new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static Configuracion normalizar(Configuracion config) {
        if (config == null) {
            return Configuracion.porDefecto();
        }

        if (config.apariencia == null) {
            config.apariencia = Apariencia.porDefecto();
        }
        if (config.negocio == null) {
            config.negocio = Negocio.porDefecto();
        }

        if (config.apariencia.colorEnfasis == null || config.apariencia.colorEnfasis.trim().isEmpty()) {
            config.apariencia.colorEnfasis = Apariencia.porDefecto().colorEnfasis;
        }
        config.apariencia.tamanoTexto = normalizarTamanoTexto(config.apariencia.tamanoTexto);

        if (config.negocio.nombreTienda == null || config.negocio.nombreTienda.trim().isEmpty()) {
            config.negocio.nombreTienda = Negocio.porDefecto().nombreTienda;
        }

        return config;
    }

    private static String normalizarTamanoTexto(String tamanoTexto) {
        if (tamanoTexto == null) {
            return "mediano";
        }

        String valor = tamanoTexto.trim().toLowerCase();
        if ("pequeno".equals(valor) || "mediano".equals(valor) || "grande".equals(valor)) {
            return valor;
        }
        return "mediano";
    }

    public static final class Configuracion {
        @SerializedName("apariencia")
        public Apariencia apariencia;

        @SerializedName("negocio")
        public Negocio negocio;

        static Configuracion porDefecto() {
            Configuracion c = new Configuracion();
            c.apariencia = Apariencia.porDefecto();
            c.negocio = Negocio.porDefecto();
            return c;
        }
    }

    public static final class Apariencia {
        @SerializedName("tema_oscuro")
        public boolean temaOscuro;

        @SerializedName("color_enfasis")
        public String colorEnfasis;

        @SerializedName("tamano_texto")
        public String tamanoTexto;

        static Apariencia porDefecto() {
            Apariencia a = new Apariencia();
            a.temaOscuro = false;
            a.colorEnfasis = "#4A7EE8";
            a.tamanoTexto = "mediano";
            return a;
        }
    }

    public static final class Negocio {
        @SerializedName("nombre_tienda")
        public String nombreTienda;

        static Negocio porDefecto() {
            Negocio n = new Negocio();
            n.nombreTienda = "PUNTO DE VENTA";
            return n;
        }
    }
}
