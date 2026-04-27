package com.example.puntodeventa;

import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class PantallaHistorial extends AppCompatActivity {

    private TextView tvHistorialContenido;
    private TextView lblFolioHistorial;
    private Spinner spinnerFolios;
    private Button btnExportarPdfHistorial;
    private Button btnExportarExcelHistorial;
    private final List<HistorialComprasManager.Compra> comprasOrdenadas = new ArrayList<>();
    private HistorialComprasManager.Compra compraSeleccionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.pantalla_historial);

        View root = findViewById(R.id.mainHistorial);
        final int initialLeft = root.getPaddingLeft();
        final int initialTop = root.getPaddingTop();
        final int initialRight = root.getPaddingRight();
        final int initialBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    initialLeft + systemBars.left,
                    initialTop + systemBars.top,
                    initialRight + systemBars.right,
                    initialBottom + systemBars.bottom
            );
            return insets;
        });

        TextView tvTitulo = findViewById(R.id.tvTituloHistorial);
        lblFolioHistorial = findViewById(R.id.lblFolioHistorial);
        spinnerFolios = findViewById(R.id.spinnerFolios);
        tvHistorialContenido = findViewById(R.id.tvHistorialContenido);
        btnExportarPdfHistorial = findViewById(R.id.btnExportarPdfHistorial);
        btnExportarExcelHistorial = findViewById(R.id.btnExportarExcelHistorial);
        Button btnVolver = findViewById(R.id.btnVolverHistorial);

        tvTitulo.setText(GestorTraducciones.obtenerTexto(this, "lbl_titulo_historial", "Historial de compras"));
        lblFolioHistorial.setText(GestorTraducciones.obtenerTexto(this, "lbl_folio", "Folio:"));
        btnExportarPdfHistorial.setText(GestorTraducciones.obtenerTexto(this, "btn_exportar_pdf", "Exportar PDF"));
        btnExportarExcelHistorial.setText(GestorTraducciones.obtenerTexto(this, "btn_exportar_excel", "Exportar Excel"));
        btnVolver.setText(GestorTraducciones.obtenerTexto(this, "btn_volver", "Volver"));

        AppConfigManager.Configuracion config = AppConfigManager.obtenerConfiguracion(this);
        AppConfigManager.aplicarColorEnfasis(
            this,
            config.apariencia.colorEnfasis,
            btnExportarPdfHistorial,
            btnExportarExcelHistorial,
            btnVolver
        );
        AppConfigManager.aplicarEscalaTexto(findViewById(R.id.mainHistorial), config.apariencia.tamanoTexto);

        btnExportarPdfHistorial.setOnClickListener(v -> exportarCompraSeleccionada());
        btnExportarExcelHistorial.setOnClickListener(v -> exportarExcelCompraSeleccionada());
        btnVolver.setOnClickListener(v -> finish());

        cargarHistorial();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarHistorial();
    }

    private void cargarHistorial() {
        HistorialComprasManager.Historial historial = HistorialComprasManager.obtenerHistorial(this);
        List<HistorialComprasManager.Compra> compras = historial.compras;

        if (compras == null || compras.isEmpty()) {
            compraSeleccionada = null;
            spinnerFolios.setAdapter(null);
            spinnerFolios.setVisibility(View.GONE);
            lblFolioHistorial.setVisibility(View.GONE);
            btnExportarPdfHistorial.setVisibility(View.GONE);
            btnExportarExcelHistorial.setVisibility(View.GONE);
            tvHistorialContenido.setText(GestorTraducciones.obtenerTexto(this, "msg_historial_vacio", "No hay historial"));
            return;
        }

        comprasOrdenadas.clear();
        for (int i = compras.size() - 1; i >= 0; i--) {
            comprasOrdenadas.add(compras.get(i));
        }

        List<String> folios = new ArrayList<>();
        for (HistorialComprasManager.Compra compra : comprasOrdenadas) {
            folios.add(compra.folio);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                folios
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFolios.setAdapter(adapter);
        spinnerFolios.setVisibility(View.VISIBLE);
        lblFolioHistorial.setVisibility(View.VISIBLE);
        btnExportarPdfHistorial.setVisibility(View.VISIBLE);
        btnExportarExcelHistorial.setVisibility(View.VISIBLE);

        spinnerFolios.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mostrarDetalleCompra(comprasOrdenadas.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (!comprasOrdenadas.isEmpty()) {
            mostrarDetalleCompra(comprasOrdenadas.get(0));
        }
    }

    private void mostrarDetalleCompra(HistorialComprasManager.Compra compra) {
        if (compra == null) {
            compraSeleccionada = null;
            tvHistorialContenido.setText("");
            return;
        }

        compraSeleccionada = compra;

        StringBuilder sb = new StringBuilder();
        sb.append(GestorTraducciones.obtenerTexto(this, "lbl_folio", "Folio:"))
                .append(" ")
                .append(compra.folio)
                .append("\n");
        sb.append(GestorTraducciones.obtenerTexto(this, "lbl_fecha", "Fecha:"))
                .append(" ")
                .append(compra.fecha)
                .append("\n\n");

        if (compra.productos != null && !compra.productos.isEmpty()) {
            for (HistorialComprasManager.ProductoCompra producto : compra.productos) {
                sb.append(GestorTraducciones.obtenerTexto(this, "lbl_codigo", "Codigo:"))
                        .append(" ")
                        .append(producto.codigo)
                        .append("\n");
                sb.append(GestorTraducciones.obtenerTexto(this, "lbl_nombre", "Nombre:"))
                        .append(" ")
                        .append(producto.nombre)
                        .append("\n");
                sb.append(GestorTraducciones.obtenerTexto(this, "lbl_cantidad", "Cantidad:"))
                        .append(" ")
                        .append(producto.cantidad)
                        .append("\n");
                sb.append(GestorTraducciones.obtenerTexto(this, "lbl_precio", "Precio:"))
                        .append(" $")
                        .append(formatear(producto.precioUnitario))
                        .append("\n");
                sb.append(GestorTraducciones.obtenerTexto(this, "lbl_subtotal", "Subtotal:"))
                        .append(" $")
                        .append(formatear(producto.subtotal))
                        .append("\n\n");
            }
        }

        sb.append(GestorTraducciones.obtenerTexto(this, "lbl_total", "Total:"))
                .append(" $")
                .append(formatear(compra.total));

        tvHistorialContenido.setText(sb.toString().trim());
    }

    private void exportarCompraSeleccionada() {
        if (compraSeleccionada == null) {
            mostrarMensaje(GestorTraducciones.obtenerTexto(this, "msg_historial_vacio", "No hay historial"));
            return;
        }

        if (compraSeleccionada.productos == null || compraSeleccionada.productos.isEmpty()) {
            mostrarMensaje(GestorTraducciones.obtenerTexto(this, "msg_historial_vacio", "No hay historial"));
            return;
        }

        List<GeneradorReportes.FilaReporte> filas = new ArrayList<>();
        for (HistorialComprasManager.ProductoCompra producto : compraSeleccionada.productos) {
            filas.add(new GeneradorReportes.FilaReporte(
                    producto.nombre,
                    producto.cantidad,
                    "$" + formatear(producto.subtotal)
            ));
        }

        String rutaArchivo = GeneradorReportes.generarReporteVentas(
                this,
                filas,
                "$" + formatear(compraSeleccionada.total)
        );

        if (rutaArchivo == null) {
            mostrarMensaje("No se pudo generar el PDF");
            return;
        }

        mostrarMensaje("PDF generado en: " + rutaArchivo);
    }

    private void exportarExcelCompraSeleccionada() {
        if (compraSeleccionada == null) {
            mostrarMensaje(GestorTraducciones.obtenerTexto(this, "msg_historial_vacio", "No hay historial"));
            return;
        }

        String rutaArchivo = GeneradorExportacionExcel.exportarCompraDesdeJsonACsv(this, compraSeleccionada);
        if (rutaArchivo == null) {
            mostrarMensaje("No se pudo generar la exportacion");
            return;
        }

        mostrarMensaje("Exportacion generada en: " + rutaArchivo);
    }

    private String formatear(double valor) {
        return String.format(Locale.getDefault(), "%.2f", valor);
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
}
