package com.example.puntodeventa;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CambioMonedaManager {

    private static final String ARCHIVO_JSON = "cambio_moneda.json";
    private static final String DB_NOMBRE = "administracion";
    private static final int DB_VERSION = 1;
    private static final String TABLA_PRODUCTOS = "productos";
    private static final String PREFS = "cambio_moneda_prefs";
    private static final String KEY_MONEDA_ACTUAL = "moneda_actual";
    private static final String MONEDA_POR_DEFECTO = "MXN";

    private CambioMonedaManager() {
    }

    public static String obtenerMonedaActual(Context context) {
        try {
            ConfigCambioMoneda config = leerConfiguracion(context);
            return obtenerMonedaActual(context, config);
        } catch (Exception e) {
            return MONEDA_POR_DEFECTO;
        }
    }

    public static ResultadoConversion convertirPrecios(Context context) {
        try {
            ConfigCambioMoneda config = leerConfiguracion(context);
            if (config == null || config.tiposDeCambio == null || config.tiposDeCambio.isEmpty()) {
                return ResultadoConversion.error("No se pudo leer cambio_moneda.json");
            }

            String monedaActual = obtenerMonedaActual(context, config);
            String monedaDestino = obtenerMonedaDestino(config, monedaActual);
            TipoCambio tipoCambio = buscarTipoCambio(config.tiposDeCambio, monedaActual, monedaDestino);

            if (tipoCambio == null || tipoCambio.tasa <= 0) {
                return ResultadoConversion.error("No se encontro tasa para convertir de " + monedaActual + " a " + monedaDestino);
            }

            int filasActualizadas = actualizarPreciosEnDb(context, tipoCambio.tasa);
            guardarMonedaActual(context, monedaDestino);

            return ResultadoConversion.ok(monedaActual, monedaDestino, tipoCambio.tasa, filasActualizadas);
        } catch (Exception e) {
            return ResultadoConversion.error("Ocurrio un error al convertir precios");
        }
    }

    public static double obtenerTasa(Context context, String origen, String destino) {
        if (origen == null || destino == null) {
            return 0;
        }
        try {
            ConfigCambioMoneda config = leerConfiguracion(context);
            if (config == null || config.tiposDeCambio == null || config.tiposDeCambio.isEmpty()) {
                return 0;
            }
            TipoCambio tipo = buscarTipoCambio(config.tiposDeCambio, origen, destino);
            return (tipo != null) ? tipo.tasa : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int actualizarPreciosEnDb(Context context, double tasa) {
        AdminSqLite admin = new AdminSqLite(context, DB_NOMBRE, null, DB_VERSION);
        SQLiteDatabase bd = null;
        Cursor cursor = null;
        int actualizados = 0;

        try {
            bd = admin.getWritableDatabase();
            bd.beginTransaction();
            cursor = bd.rawQuery("select codigo, precio from " + TABLA_PRODUCTOS, null);

            while (cursor.moveToNext()) {
                int codigo = cursor.getInt(0);
                double precioActual = cursor.getDouble(1);
                double nuevoPrecio = redondear(precioActual * tasa);

                ContentValues values = new ContentValues();
                values.put("precio", nuevoPrecio);
                int filas = bd.update(TABLA_PRODUCTOS, values, "codigo=?", new String[]{String.valueOf(codigo)});
                if (filas > 0) {
                    actualizados += filas;
                }
            }

            bd.setTransactionSuccessful();
            return actualizados;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (bd != null) {
                if (bd.inTransaction()) {
                    bd.endTransaction();
                }
                bd.close();
            }
        }
    }

    private static ConfigCambioMoneda leerConfiguracion(Context context) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(ARCHIVO_JSON)));
        StringBuilder sb = new StringBuilder();
        String linea;
        while ((linea = reader.readLine()) != null) {
            sb.append(linea);
        }
        reader.close();
        return new Gson().fromJson(sb.toString(), ConfigCambioMoneda.class);
    }

    private static String obtenerMonedaActual(Context context, ConfigCambioMoneda config) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String guardada = prefs.getString(KEY_MONEDA_ACTUAL, MONEDA_POR_DEFECTO);
        if (guardada == null || guardada.trim().isEmpty()) {
            return MONEDA_POR_DEFECTO;
        }
        if (config.monedasSoportadas == null || config.monedasSoportadas.isEmpty()) {
            return guardada.toUpperCase(Locale.ROOT);
        }

        String actual = guardada.toUpperCase(Locale.ROOT);
        for (String soportada : config.monedasSoportadas) {
            if (actual.equalsIgnoreCase(soportada)) {
                return actual;
            }
        }
        return config.monedasSoportadas.get(0).toUpperCase(Locale.ROOT);
    }

    private static void guardarMonedaActual(Context context, String moneda) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MONEDA_ACTUAL, moneda)
                .apply();
    }

    private static String obtenerMonedaDestino(ConfigCambioMoneda config, String monedaActual) {
        if (config.monedasSoportadas == null || config.monedasSoportadas.isEmpty()) {
            return "USD";
        }

        for (String moneda : config.monedasSoportadas) {
            if (!monedaActual.equalsIgnoreCase(moneda)) {
                return moneda.toUpperCase(Locale.ROOT);
            }
        }
        return monedaActual;
    }

    private static TipoCambio buscarTipoCambio(List<TipoCambio> tipos, String origen, String destino) {
        for (TipoCambio tipo : tipos) {
            if (tipo != null
                    && origen.equalsIgnoreCase(tipo.origen)
                    && destino.equalsIgnoreCase(tipo.destino)) {
                return tipo;
            }
        }
        return null;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static final class ConfigCambioMoneda {
        @SerializedName("monedas_soportadas")
        List<String> monedasSoportadas = new ArrayList<>();

        @SerializedName("tipos_de_cambio")
        List<TipoCambio> tiposDeCambio = new ArrayList<>();
    }

    private static final class TipoCambio {
        @SerializedName("origen")
        String origen;

        @SerializedName("destino")
        String destino;

        @SerializedName("tasa")
        double tasa;
    }

    public static final class ResultadoConversion {
        public final boolean exito;
        public final String origen;
        public final String destino;
        public final double tasa;
        public final int filasActualizadas;
        public final String mensajeError;

        private ResultadoConversion(boolean exito, String origen, String destino, double tasa,
                                    int filasActualizadas, String mensajeError) {
            this.exito = exito;
            this.origen = origen;
            this.destino = destino;
            this.tasa = tasa;
            this.filasActualizadas = filasActualizadas;
            this.mensajeError = mensajeError;
        }

        static ResultadoConversion ok(String origen, String destino, double tasa, int filasActualizadas) {
            return new ResultadoConversion(true, origen, destino, tasa, filasActualizadas, null);
        }

        static ResultadoConversion error(String mensajeError) {
            return new ResultadoConversion(false, null, null, 0, 0, mensajeError);
        }
    }
}
