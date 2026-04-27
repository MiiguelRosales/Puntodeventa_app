package com.example.puntodeventa;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class HistorialComprasManager {

    private static final String ARCHIVO = "historial_compras.json";

    private HistorialComprasManager() {
    }

    public static boolean guardarCompra(Context context, List<ProductoCompra> productos, double total) {
        if (productos == null || productos.isEmpty()) {
            return false;
        }

        try {
            Historial historial = leerHistorial(context);
            if (historial.compras == null) {
                historial.compras = new ArrayList<>();
            }

            Compra compra = new Compra();
            compra.folio = generarSiguienteFolio(historial.compras);
            compra.fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            compra.moneda = CambioMonedaManager.obtenerMonedaActual(context);
            compra.total = redondear(total);
            compra.productos = new ArrayList<>(productos);

            historial.compras.add(compra);
            guardarHistorial(context, historial);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Historial obtenerHistorial(Context context) {
        try {
            return leerHistorial(context);
        } catch (Exception e) {
            return new Historial();
        }
    }

    private static Historial leerHistorial(Context context) {
        File file = new File(context.getFilesDir(), ARCHIVO);
        if (!file.exists()) {
            return new Historial();
        }

        try (FileInputStream inputStream = context.openFileInput(ARCHIVO)) {
            byte[] buffer = new byte[(int) file.length()];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead <= 0) {
                return new Historial();
            }

            String json = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            Historial historial = new Gson().fromJson(json, Historial.class);
            if (historial == null) {
                return new Historial();
            }
            if (historial.compras == null) {
                historial.compras = new ArrayList<>();
            }
            for (Compra compra : historial.compras) {
                if (compra != null && (compra.moneda == null || compra.moneda.trim().isEmpty())) {
                    compra.moneda = "MXN";
                }
            }
            return historial;
        } catch (Exception e) {
            return new Historial();
        }
    }

    private static void guardarHistorial(Context context, Historial historial) throws Exception {
        String json = new Gson().toJson(historial);
        try (FileOutputStream outputStream = context.openFileOutput(ARCHIVO, Context.MODE_PRIVATE)) {
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String generarSiguienteFolio(List<Compra> compras) {
        int maximo = 0;
        for (Compra compra : compras) {
            if (compra == null || compra.folio == null) {
                continue;
            }
            if (!compra.folio.startsWith("V-")) {
                continue;
            }
            try {
                int valor = Integer.parseInt(compra.folio.substring(2));
                if (valor > maximo) {
                    maximo = valor;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return "V-" + (maximo + 1);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public static final class Historial {
        @SerializedName("compras")
        public List<Compra> compras;

        public Historial() {
            this.compras = new ArrayList<>();
        }
    }

    public static final class Compra {
        @SerializedName("folio")
        public String folio;

        @SerializedName("fecha")
        public String fecha;

        @SerializedName("moneda")
        public String moneda;

        @SerializedName("total")
        public double total;

        @SerializedName("productos")
        public List<ProductoCompra> productos;
    }

    public static final class ProductoCompra {
        @SerializedName("codigo")
        public String codigo;

        @SerializedName("nombre")
        public String nombre;

        @SerializedName("cantidad")
        public int cantidad;

        @SerializedName("precio_unitario")
        public double precioUnitario;

        @SerializedName("subtotal")
        public double subtotal;

        public ProductoCompra(String codigo, String nombre, int cantidad, double precioUnitario, double subtotal) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precioUnitario = redondear(precioUnitario);
            this.subtotal = redondear(subtotal);
        }
    }
}
