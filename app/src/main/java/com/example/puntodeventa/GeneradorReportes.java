package com.example.puntodeventa;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GeneradorReportes {

    public static class FilaReporte {
        public final String nombre;
        public final int cantidad;
        public final String subtotal;

        public FilaReporte(String nombre, int cantidad, String subtotal) {
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.subtotal = subtotal;
        }
    }

    public static String generarReporteVentas(Context context, List<FilaReporte> filas, String total, String moneda) {
        if (filas == null || filas.isEmpty()) {
            return null;
        }

        PdfDocument document = new PdfDocument();
        Paint tituloPaint = new Paint();
        tituloPaint.setTextSize(18f);
        tituloPaint.setFakeBoldText(true);

        Paint encabezadoPaint = new Paint();
        encabezadoPaint.setTextSize(13f);
        encabezadoPaint.setFakeBoldText(true);

        Paint textoPaint = new Paint();
        textoPaint.setTextSize(12f);

        int pageWidth = 595;
        int pageHeight = 842;
        int startX = 40;
        int y = 50;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        canvas.drawText("Reporte de Ventas", startX, y, tituloPaint);
        y += 24;

        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Fecha: " + fecha, startX, y, textoPaint);
        y += 26;
        canvas.drawText("Moneda: " + valorSeguro(moneda), startX, y, textoPaint);
        y += 24;

        int colNombre = startX;
        int colCantidad = 320;
        int colSubtotal = 430;

        canvas.drawText("Nombre", colNombre, y, encabezadoPaint);
        canvas.drawText("Cantidad", colCantidad, y, encabezadoPaint);
        canvas.drawText("Subtotal", colSubtotal, y, encabezadoPaint);
        y += 16;
        canvas.drawLine(startX, y, pageWidth - startX, y, textoPaint);
        y += 18;

        int filasPorPagina = 32;
        int filasEnPagina = 0;
        int paginaActual = 1;

        for (FilaReporte fila : filas) {
            if (filasEnPagina >= filasPorPagina) {
                document.finishPage(page);
                paginaActual++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, paginaActual).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;

                canvas.drawText("Reporte de Ventas (continuacion)", startX, y, tituloPaint);
                y += 24;
                canvas.drawText("Nombre", colNombre, y, encabezadoPaint);
                canvas.drawText("Cantidad", colCantidad, y, encabezadoPaint);
                canvas.drawText("Subtotal", colSubtotal, y, encabezadoPaint);
                y += 16;
                canvas.drawLine(startX, y, pageWidth - startX, y, textoPaint);
                y += 18;
                filasEnPagina = 0;
            }

            canvas.drawText(valorSeguro(fila.nombre), colNombre, y, textoPaint);
            canvas.drawText(String.valueOf(fila.cantidad), colCantidad, y, textoPaint);
            canvas.drawText(valorSeguro(fila.subtotal), colSubtotal, y, textoPaint);
            y += 20;
            filasEnPagina++;
        }

        y += 10;
        canvas.drawLine(startX, y, pageWidth - startX, y, textoPaint);
        y += 20;
        canvas.drawText("Total: " + valorSeguro(total), colSubtotal - 20, y, encabezadoPaint);

        document.finishPage(page);
        String nombreArchivo = "reporte_ventas_" + System.currentTimeMillis() + ".pdf";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                document.close();
                return null;
            }

            try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                if (os == null) {
                    document.close();
                    return null;
                }
                document.writeTo(os);
                document.close();
                return Environment.DIRECTORY_DOWNLOADS + "/" + nombreArchivo;
            } catch (IOException e) {
                document.close();
                return null;
            }
        }

        File carpeta = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (carpeta == null) {
            document.close();
            return null;
        }

        if (!carpeta.exists() && !carpeta.mkdirs()) {
            document.close();
            return null;
        }

        File archivo = new File(carpeta, nombreArchivo);

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            document.writeTo(fos);
            document.close();
            return archivo.getAbsolutePath();
        } catch (IOException e) {
            document.close();
            return null;
        }
    }

    private static String valorSeguro(String valor) {
        if (valor == null) {
            return "";
        }
        return valor;
    }
}
