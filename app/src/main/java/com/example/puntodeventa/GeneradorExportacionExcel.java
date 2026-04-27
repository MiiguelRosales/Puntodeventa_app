package com.example.puntodeventa;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GeneradorExportacionExcel {

    private static final String ARCHIVO_JSON_INTERNO = "exportar_excel.json";

    private GeneradorExportacionExcel() {
    }

    public static String exportarCompraDesdeJsonACsv(Context context, HistorialComprasManager.Compra compra) {
        if (compra == null || compra.productos == null || compra.productos.isEmpty()) {
            return null;
        }

        ExportacionExcel exportacion = construirExportacion(context, compra);

        try {
            guardarJsonTemporalInterno(context, exportacion);

            ExportacionExcel leido = leerJsonTemporalInterno(context);
            if (leido == null || leido.productos == null || leido.productos.isEmpty()) {
                return null;
            }

            String csv = convertirACsv(leido);
            String folioSeguro = compra.folio == null ? "sin_folio" : compra.folio.replace(" ", "_");
            String nombreArchivo = "exportar_excel_" + folioSeguro + "_" + System.currentTimeMillis() + ".csv";
            return guardarEnDescargas(context, nombreArchivo, "text/csv", csv);
        } catch (Exception e) {
            return null;
        }
    }

    private static ExportacionExcel construirExportacion(Context context, HistorialComprasManager.Compra compra) {
        ExportacionExcel exportacion = new ExportacionExcel();
        exportacion.fecha = compra.fecha;
        if (compra != null && compra.moneda != null && !compra.moneda.trim().isEmpty()) {
            exportacion.moneda = compra.moneda.trim();
        } else {
            exportacion.moneda = CambioMonedaManager.obtenerMonedaActual(context);
        }
        exportacion.totalVenta = redondear(compra.total);

        for (HistorialComprasManager.ProductoCompra producto : compra.productos) {
            ExportacionExcel.ProductoExcel p = new ExportacionExcel.ProductoExcel();
            p.codigo = producto.codigo;
            p.nombre = producto.nombre;
            p.precio = redondear(producto.precioUnitario);
            p.cantidad = producto.cantidad;
            p.subtotal = redondear(producto.subtotal);
            exportacion.productos.add(p);
        }

        return exportacion;
    }

    private static void guardarJsonTemporalInterno(Context context, ExportacionExcel exportacion) throws Exception {
        String contenidoJson = new Gson().toJson(exportacion);
        try (FileOutputStream outputStream = context.openFileOutput(ARCHIVO_JSON_INTERNO, Context.MODE_PRIVATE)) {
            outputStream.write(contenidoJson.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static ExportacionExcel leerJsonTemporalInterno(Context context) {
        File file = new File(context.getFilesDir(), ARCHIVO_JSON_INTERNO);
        if (!file.exists()) {
            return null;
        }

        try (FileInputStream inputStream = context.openFileInput(ARCHIVO_JSON_INTERNO)) {
            byte[] buffer = new byte[(int) file.length()];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead <= 0) {
                return null;
            }

            String json = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            return new Gson().fromJson(json, ExportacionExcel.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static String convertirACsv(ExportacionExcel exportacion) {
        StringBuilder sb = new StringBuilder();
        sb.append("fecha,moneda,codigo,nombre,precio,cantidad,subtotal\n");

        String fecha = escaparCsv(exportacion.fecha);
        String moneda = escaparCsv(exportacion.moneda);
        for (ExportacionExcel.ProductoExcel producto : exportacion.productos) {
            sb.append(fecha).append(",")
                    .append(moneda).append(",")
                    .append(escaparCsv(producto.codigo)).append(",")
                    .append(escaparCsv(producto.nombre)).append(",")
                    .append(formatearNumero(producto.precio)).append(",")
                    .append(producto.cantidad).append(",")
                    .append(formatearNumero(producto.subtotal)).append("\n");
        }

        sb.append("\n");
        sb.append("total_venta,").append(moneda).append(",,,,,").append(formatearNumero(exportacion.totalVenta)).append("\n");
        return sb.toString();
    }

    private static String guardarEnDescargas(Context context, String nombreArchivo, String mimeType,
                                             String contenido) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                return null;
            }

            try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                if (os == null) {
                    return null;
                }
                os.write(contenido.getBytes(StandardCharsets.UTF_8));
                return Environment.DIRECTORY_DOWNLOADS + "/" + nombreArchivo;
            }
        }

        File carpeta = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (carpeta == null) {
            return null;
        }
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            return null;
        }

        File archivo = new File(carpeta, nombreArchivo);
        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            fos.write(contenido.getBytes(StandardCharsets.UTF_8));
            return archivo.getAbsolutePath();
        }
    }

    private static String escaparCsv(String valor) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.replace("\"", "\"\"");
        if (limpio.contains(",") || limpio.contains("\n") || limpio.contains("\"")) {
            return "\"" + limpio + "\"";
        }
        return limpio;
    }

    private static String formatearNumero(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static final class ExportacionExcel {
        @SerializedName("fecha")
        String fecha;

        @SerializedName("moneda")
        String moneda;

        @SerializedName("productos")
        List<ProductoExcel> productos = new ArrayList<>();

        @SerializedName("total_venta")
        double totalVenta;

        private static final class ProductoExcel {
            @SerializedName("codigo")
            String codigo;

            @SerializedName("nombre")
            String nombre;

            @SerializedName("precio")
            double precio;

            @SerializedName("cantidad")
            int cantidad;

            @SerializedName("subtotal")
            double subtotal;
        }
    }
}
