package com.example.puntodeventa;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PantallaVentas extends AppCompatActivity {

    private static final String DB_NOMBRE = "administracion";
    private static final int DB_VERSION = 1;
    private static final String TABLA_PRODUCTOS = "productos";

    private EditText etCodigo;
    private EditText etCantidad;
    private TextView tvNombre;
    private TextView tvPrecio;
    private TextView tvSubtotal;
    private TextView tvTotal;
    private TableLayout tblProductosAgregados;

    private final List<ItemVenta> itemsVenta = new ArrayList<>();
    private Double precioActual = null;
    private boolean avisoCantidadSinProductoMostrado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.pantalla_ventas);
        View root = findViewById(R.id.mainVentas);
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

        etCodigo = findViewById(R.id.etCodigo);
        etCantidad = findViewById(R.id.etCantidad);
        tvNombre = findViewById(R.id.tvNombre);
        tvPrecio = findViewById(R.id.tvPrecio);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTotal = findViewById(R.id.tvTotal);
        tblProductosAgregados = findViewById(R.id.tblProductosAgregados);

        Button btnBuscarCalcular = findViewById(R.id.btnBuscarCalcular);
        Button btnAgregar = findViewById(R.id.btnAgregar);
        Button btnTerminarVenta = findViewById(R.id.btnTerminarVenta);
        Button btnNuevaVenta = findViewById(R.id.btnNuevaVenta);

        btnBuscarCalcular.setOnClickListener(this::buscarYCalcular);
        btnAgregar.setOnClickListener(this::agregarProducto);
        btnTerminarVenta.setOnClickListener(this::terminarVenta);
        btnNuevaVenta.setOnClickListener(this::nuevaVenta);

        etCantidad.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalcularSubtotal();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        tvSubtotal.setText("0.00");
        tvTotal.setText("0.00");
    }

    public void buscarYCalcular(View v) {
        Integer codigo = obtenerCodigoValido();
        if (codigo == null) {
            return;
        }

        Producto producto = obtenerProductoDesdeBD(codigo);
        if (producto == null) {
            limpiarCamposProducto();
            mostrarMensaje("No existe producto con ese codigo");
            return;
        }

        tvNombre.setText(producto.nombre);
        tvPrecio.setText(formatearMoneda(producto.precio));
        precioActual = producto.precio;

        if (etCantidad.getText().toString().trim().isEmpty()) {
            tvSubtotal.setText("0.00");
            mostrarMensaje("Producto encontrado. Ingresa cantidad para calcular subtotal");
            return;
        }

        Integer cantidad = obtenerCantidadValida();
        if (cantidad == null) {
            tvSubtotal.setText("0.00");
            return;
        }

        int cantidadReservada = obtenerCantidadReservadaPorCodigo(codigo);
        int disponible = producto.existencia - cantidadReservada;
        if (disponible <= 0) {
            tvSubtotal.setText("0.00");
            mostrarMensaje("Ya no hay existencia disponible para este producto en esta venta");
            return;
        }

        if (cantidad > disponible) {
            tvSubtotal.setText("0.00");
            mostrarMensaje("No hay existencia suficiente. Disponible: " + disponible);
            return;
        }

        double subtotal = producto.precio * cantidad;
        tvSubtotal.setText(formatearMoneda(subtotal));
        mostrarMensaje("Subtotal calculado");
    }

    public void agregarProducto(View v) {
        Integer codigo = obtenerCodigoValido();
        if (codigo == null) {
            return;
        }

        Integer cantidad = obtenerCantidadValida();
        if (cantidad == null) {
            return;
        }

        Producto producto = obtenerProductoDesdeBD(codigo);
        if (producto == null) {
            limpiarCamposProducto();
            mostrarMensaje("No existe producto con ese codigo");
            return;
        }

        int cantidadReservada = obtenerCantidadReservadaPorCodigo(codigo);
        int disponible = producto.existencia - cantidadReservada;
        if (disponible <= 0) {
            mostrarMensaje("Ya no hay existencia disponible para este producto en esta venta");
            return;
        }

        if (cantidad > disponible) {
            mostrarMensaje("No hay existencia suficiente. Disponible: " + disponible);
            return;
        }

        double subtotal = producto.precio * cantidad;
        ItemVenta item = new ItemVenta(codigo, producto.nombre, cantidad, subtotal);
        itemsVenta.add(item);
        agregarFilaTabla(item);

        tvNombre.setText(producto.nombre);
        tvPrecio.setText(formatearMoneda(producto.precio));
        tvSubtotal.setText(formatearMoneda(subtotal));
        precioActual = producto.precio;

        actualizarTotal();
        etCantidad.setText("");
        etCantidad.requestFocus();
        mostrarMensaje("Producto agregado a la venta");
    }

    public void terminarVenta(View v) {
        if (itemsVenta.isEmpty()) {
            mostrarMensaje("No hay productos agregados a la venta");
            return;
        }

        AdminSqLite admin = new AdminSqLite(this, DB_NOMBRE, null, DB_VERSION);
        SQLiteDatabase bd = null;

        try {
            bd = admin.getWritableDatabase();
            bd.beginTransaction();

            Map<Integer, Integer> cantidadesPorCodigo = new HashMap<>();
            for (ItemVenta item : itemsVenta) {
                int acumulada = cantidadesPorCodigo.containsKey(item.codigo)
                        ? cantidadesPorCodigo.get(item.codigo)
                        : 0;
                cantidadesPorCodigo.put(item.codigo, acumulada + item.cantidad);
            }

            for (Map.Entry<Integer, Integer> entrada : cantidadesPorCodigo.entrySet()) {
                int codigo = entrada.getKey();
                int cantidadTotal = entrada.getValue();
                Cursor fila = null;
                try {
                    fila = bd.rawQuery(
                            "select nombre, precio, existencia from " + TABLA_PRODUCTOS + " where codigo=?",
                            new String[]{String.valueOf(codigo)}
                    );

                    if (!fila.moveToFirst()) {
                        mostrarMensaje("Un producto ya no existe. Actualiza la venta");
                        return;
                    }

                    int existenciaActual = fila.getInt(2);
                    if (existenciaActual < cantidadTotal) {
                        mostrarMensaje("Existencia insuficiente para codigo " + codigo);
                        return;
                    }

                    ContentValues valores = new ContentValues();
                    valores.put("existencia", existenciaActual - cantidadTotal);
                    bd.update(TABLA_PRODUCTOS, valores, "codigo=?", new String[]{String.valueOf(codigo)});
                } finally {
                    if (fila != null) {
                        fila.close();
                    }
                }
            }

            bd.setTransactionSuccessful();
            mostrarMensaje("Gracias por tu compra");
            limpiarVentaCompleta();
        } catch (SQLiteException e) {
            mostrarMensaje("Error al finalizar venta en base de datos");
        } finally {
            if (bd != null) {
                if (bd.inTransaction()) {
                    bd.endTransaction();
                }
                bd.close();
            }
        }
    }

    public void nuevaVenta(View v) {
        limpiarVentaCompleta();
        mostrarMensaje("Venta cancelada");
    }

    private void recalcularSubtotal() {
        String cantidadTexto = etCantidad.getText().toString().trim();
        if (cantidadTexto.isEmpty()) {
            tvSubtotal.setText("0.00");
            avisoCantidadSinProductoMostrado = false;
            return;
        }

        if (precioActual == null) {
            tvSubtotal.setText("0.00");
            if (!avisoCantidadSinProductoMostrado) {
                mostrarMensaje("Primero busca un producto con codigo");
                avisoCantidadSinProductoMostrado = true;
            }
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantidadTexto);
            if (cantidad <= 0) {
                tvSubtotal.setText("0.00");
                return;
            }
            double subtotal = precioActual * cantidad;
            tvSubtotal.setText(formatearMoneda(subtotal));
            avisoCantidadSinProductoMostrado = false;
        } catch (NumberFormatException e) {
            tvSubtotal.setText("0.00");
        }
    }

    private Producto obtenerProductoDesdeBD(int codigo) {
        AdminSqLite admin = new AdminSqLite(this, DB_NOMBRE, null, DB_VERSION);
        SQLiteDatabase bd = null;
        Cursor fila = null;

        try {
            bd = admin.getReadableDatabase();
            fila = bd.rawQuery(
                    "select nombre, precio, existencia from " + TABLA_PRODUCTOS + " where codigo=?",
                    new String[]{String.valueOf(codigo)}
            );

            if (fila.moveToFirst()) {
                String nombre = fila.getString(0);
                double precio = fila.getDouble(1);
                int existencia = fila.getInt(2);
                return new Producto(codigo, nombre, precio, existencia);
            }
            return null;
        } catch (SQLiteException e) {
            mostrarMensaje("Error al consultar la base de datos");
            return null;
        } finally {
            if (fila != null) {
                fila.close();
            }
            if (bd != null) {
                bd.close();
            }
        }
    }

    private Integer obtenerCodigoValido() {
        String codigoTexto = etCodigo.getText().toString().trim();
        if (codigoTexto.isEmpty()) {
            etCodigo.requestFocus();
            mostrarMensaje("El codigo es obligatorio");
            return null;
        }

        try {
            int codigo = Integer.parseInt(codigoTexto);
            if (codigo <= 0) {
                etCodigo.requestFocus();
                mostrarMensaje("El codigo debe ser mayor que 0");
                return null;
            }
            return codigo;
        } catch (NumberFormatException e) {
            etCodigo.requestFocus();
            mostrarMensaje("Codigo invalido");
            return null;
        }
    }

    private Integer obtenerCantidadValida() {
        String cantidadTexto = etCantidad.getText().toString().trim();
        if (cantidadTexto.isEmpty()) {
            etCantidad.requestFocus();
            mostrarMensaje("La cantidad es obligatoria");
            return null;
        }

        try {
            int cantidad = Integer.parseInt(cantidadTexto);
            if (cantidad <= 0) {
                etCantidad.requestFocus();
                mostrarMensaje("La cantidad debe ser mayor que 0");
                return null;
            }
            return cantidad;
        } catch (NumberFormatException e) {
            etCantidad.requestFocus();
            mostrarMensaje("Cantidad invalida");
            return null;
        }
    }

    private int obtenerCantidadReservadaPorCodigo(int codigo) {
        int acumulado = 0;
        for (ItemVenta item : itemsVenta) {
            if (item.codigo == codigo) {
                acumulado += item.cantidad;
            }
        }
        return acumulado;
    }

    private void agregarFilaTabla(ItemVenta item) {
        TableRow fila = new TableRow(this);

        TextView tvNombreItem = crearCeldaTabla(item.nombre);
        TextView tvCantidadItem = crearCeldaTabla(String.valueOf(item.cantidad));
        TextView tvSubtotalItem = crearCeldaTabla(formatearMoneda(item.subtotal));

        fila.addView(tvNombreItem);
        fila.addView(tvCantidadItem);
        fila.addView(tvSubtotalItem);
        tblProductosAgregados.addView(fila);
    }

    private TextView crearCeldaTabla(String texto) {
        TextView tv = new TextView(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(params);
        tv.setText(texto);
        tv.setTextColor(getColor(R.color.text_primary));
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(4, 10, 4, 10);
        return tv;
    }

    private void actualizarTotal() {
        double total = 0;
        for (ItemVenta item : itemsVenta) {
            total += item.subtotal;
        }
        tvTotal.setText(formatearMoneda(total));
    }

    private void limpiarCamposProducto() {
        tvNombre.setText("");
        tvPrecio.setText("");
        tvSubtotal.setText("0.00");
        precioActual = null;
        avisoCantidadSinProductoMostrado = false;
    }

    private void limpiarVentaCompleta() {
        itemsVenta.clear();

        int filas = tblProductosAgregados.getChildCount();
        if (filas > 0) {
            tblProductosAgregados.removeViews(0, filas);
        }

        etCodigo.setText("");
        etCantidad.setText("");
        tvTotal.setText("0.00");
        limpiarCamposProducto();
        etCodigo.requestFocus();
    }

    private String formatearMoneda(double valor) {
        return String.format(Locale.getDefault(), "%.2f", valor);
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    private static class Producto {
        int codigo;
        String nombre;
        double precio;
        int existencia;

        Producto(int codigo, String nombre, double precio, int existencia) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.precio = precio;
            this.existencia = existencia;
        }
    }

    private static class ItemVenta {
        int codigo;
        String nombre;
        int cantidad;
        double subtotal;

        ItemVenta(int codigo, String nombre, int cantidad, double subtotal) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.subtotal = subtotal;
        }
    }
}
