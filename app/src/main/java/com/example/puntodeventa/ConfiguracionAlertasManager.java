package com.example.puntodeventa;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ConfiguracionAlertasManager {

    private static final String ARCHIVO_ASSET = "configuracion_alertas.json";
    private static final String ARCHIVO_INTERNO = "configuracion_alertas_runtime.json";

    private static ConfiguracionAlertas cache;

    private ConfiguracionAlertasManager() {
    }

    public static ConfiguracionAlertas obtenerConfiguracion(Context context) {
        if (cache != null) {
            return cache;
        }

        try {
            String contenido = leerInterno(context);
            if (contenido == null || contenido.trim().isEmpty()) {
                contenido = leerAsset(context, ARCHIVO_ASSET);
            }
            cache = normalizar(new Gson().fromJson(contenido, ConfiguracionAlertas.class));
        } catch (Exception e) {
            cache = ConfiguracionAlertas.porDefecto();
        }

        if (cache == null) {
            cache = ConfiguracionAlertas.porDefecto();
        }

        return cache;
    }

    public static boolean guardarConfiguracion(Context context, ConfiguracionAlertas config) {
        try {
            ConfiguracionAlertas normalizada = normalizar(config);
            String json = new Gson().toJson(normalizada);
            try (FileOutputStream outputStream = context.openFileOutput(ARCHIVO_INTERNO, Context.MODE_PRIVATE)) {
                outputStream.write(json.getBytes(StandardCharsets.UTF_8));
            }
            cache = normalizada;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static EvaluacionStock evaluar(Context context, int stockDisponible) {
        ConfiguracionAlertas config = obtenerConfiguracion(context);
        if (stockDisponible <= 0) {
            return new EvaluacionStock(NivelStock.SIN_STOCK, config.alertas.mensajeSinStock, true);
        }
        if (stockDisponible <= config.stock.critico) {
            return new EvaluacionStock(NivelStock.CRITICO, config.alertas.mensajeCritico, false);
        }
        if (stockDisponible <= config.stock.minimoAlerta) {
            return new EvaluacionStock(NivelStock.BAJO, config.alertas.mensajeBajo, false);
        }
        return new EvaluacionStock(NivelStock.OK, null, false);
    }

    public static boolean debeBloquearSinStock(Context context) {
        return obtenerConfiguracion(context).stock.bloquearSinStock;
    }

    public static boolean debeMostrarPopup(Context context) {
        return obtenerConfiguracion(context).alertas.mostrarPopup;
    }

    public static boolean debeUsarColor(Context context) {
        return obtenerConfiguracion(context).alertas.usarColor;
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

    private static ConfiguracionAlertas normalizar(ConfiguracionAlertas config) {
        if (config == null) {
            return ConfiguracionAlertas.porDefecto();
        }
        if (config.stock == null) {
            config.stock = Stock.porDefecto();
        }
        if (config.alertas == null) {
            config.alertas = Alertas.porDefecto();
        }
        if (config.stock.minimoAlerta < 0) {
            config.stock.minimoAlerta = 0;
        }
        if (config.stock.critico < 0) {
            config.stock.critico = 0;
        }
        if (config.stock.critico > config.stock.minimoAlerta) {
            config.stock.minimoAlerta = config.stock.critico;
        }
        if (config.alertas.mensajeBajo == null) {
            config.alertas.mensajeBajo = Alertas.porDefecto().mensajeBajo;
        }
        if (config.alertas.mensajeCritico == null) {
            config.alertas.mensajeCritico = Alertas.porDefecto().mensajeCritico;
        }
        if (config.alertas.mensajeSinStock == null) {
            config.alertas.mensajeSinStock = Alertas.porDefecto().mensajeSinStock;
        }
        return config;
    }

    public enum NivelStock {
        OK,
        BAJO,
        CRITICO,
        SIN_STOCK
    }

    public static final class EvaluacionStock {
        public final NivelStock nivel;
        public final String mensaje;
        public final boolean sinStock;

        public EvaluacionStock(NivelStock nivel, String mensaje, boolean sinStock) {
            this.nivel = nivel;
            this.mensaje = mensaje;
            this.sinStock = sinStock;
        }
    }

    public static final class ConfiguracionAlertas {
        @SerializedName("stock")
        public Stock stock;

        @SerializedName("alertas")
        public Alertas alertas;

        static ConfiguracionAlertas porDefecto() {
            ConfiguracionAlertas c = new ConfiguracionAlertas();
            c.stock = Stock.porDefecto();
            c.alertas = Alertas.porDefecto();
            return c;
        }
    }

    public static final class Stock {
        @SerializedName("minimo_alerta")
        public int minimoAlerta;

        @SerializedName("critico")
        public int critico;

        @SerializedName("bloquear_sin_stock")
        public boolean bloquearSinStock;

        static Stock porDefecto() {
            Stock s = new Stock();
            s.minimoAlerta = 3;
            s.critico = 1;
            s.bloquearSinStock = true;
            return s;
        }
    }

    public static final class Alertas {
        @SerializedName("mostrar_popup")
        public boolean mostrarPopup;

        @SerializedName("usar_color")
        public boolean usarColor;

        @SerializedName("mensaje_bajo")
        public String mensajeBajo;

        @SerializedName("mensaje_critico")
        public String mensajeCritico;

        @SerializedName("mensaje_sin_stock")
        public String mensajeSinStock;

        static Alertas porDefecto() {
            Alertas a = new Alertas();
            a.mostrarPopup = true;
            a.usarColor = true;
            a.mensajeBajo = "Stock bajo";
            a.mensajeCritico = "Stock critico";
            a.mensajeSinStock = "Sin stock";
            return a;
        }
    }
}

