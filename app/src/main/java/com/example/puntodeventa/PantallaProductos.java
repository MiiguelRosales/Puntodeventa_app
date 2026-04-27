package com.example.puntodeventa;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PantallaProductos extends AppCompatActivity {

    private static final String DB_NOMBRE = "administracion";
    private static final int DB_VERSION = 1;
    private static final String TABLA_PRODUCTOS = "productos";

    private EditText inputCodigo;
    private EditText inputNombre;
    private EditText inputDescripcion;
    private EditText inputExistencia;
    private EditText inputPrecio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.pantalla_productos);
        View root = findViewById(R.id.mainProductos);
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

        inputCodigo = findViewById(R.id.inputcodigo);
        inputNombre = findViewById(R.id.inputnombre);
        inputDescripcion = findViewById(R.id.inputdescripcion);
        inputExistencia = findViewById(R.id.inputexistencia);
        inputPrecio = findViewById(R.id.inputprecio);

        Button btnGuardar = findViewById(R.id.btnGuardar);
        Button btnConsultar = findViewById(R.id.btnConsultar);
        Button btnEliminar = findViewById(R.id.btnEliminar);
        Button btnModificar = findViewById(R.id.btnModificar);
        Button btnLimpiar = findViewById(R.id.btnLimpiar);

        aplicarTextosTraducidos(btnGuardar, btnConsultar, btnEliminar, btnModificar, btnLimpiar);

        btnGuardar.setOnClickListener(this::guardarProducto);
        btnConsultar.setOnClickListener(this::consultarProducto);
        btnEliminar.setOnClickListener(this::eliminarProducto);
        btnModificar.setOnClickListener(this::modificarProducto);
        btnLimpiar.setOnClickListener(this::limpiarFormulario);

        aplicarConfiguracionVisual(btnGuardar, btnConsultar, btnEliminar, btnModificar, btnLimpiar);
    }

    private void aplicarConfiguracionVisual(Button btnGuardar, Button btnConsultar, Button btnEliminar,
                                            Button btnModificar, Button btnLimpiar) {
        AppConfigManager.Configuracion config = AppConfigManager.obtenerConfiguracion(this);

        AppConfigManager.aplicarColorEnfasis(
                this,
                config.apariencia.colorEnfasis,
                btnGuardar,
                btnConsultar,
                btnEliminar,
                btnModificar,
                btnLimpiar
        );

        AppConfigManager.aplicarEscalaTexto(findViewById(R.id.mainProductos), config.apariencia.tamanoTexto);
    }

    private void aplicarTextosTraducidos(Button btnGuardar, Button btnConsultar, Button btnEliminar,
                                         Button btnModificar, Button btnLimpiar) {
        TextView prodTitulo = findViewById(R.id.ProdTitulo);
        TextView txtcodigo = findViewById(R.id.txtcodigo);
        TextView txtnombre = findViewById(R.id.txtnombre);
        TextView txtdescripcion = findViewById(R.id.txtdescripcion);
        TextView txtexistencia = findViewById(R.id.txtexistencia);
        TextView txtprecio = findViewById(R.id.txtprecio);

        prodTitulo.setText(GestorTraducciones.obtenerTexto(this, "lbl_titulo_productos", "Registro de Productos"));
        txtcodigo.setText(GestorTraducciones.obtenerTexto(this, "lbl_codigo", "Código:"));
        txtnombre.setText(GestorTraducciones.obtenerTexto(this, "lbl_nombre", "Nombre:"));
        txtdescripcion.setText(GestorTraducciones.obtenerTexto(this, "lbl_descripcion", "Descripción:"));
        txtexistencia.setText(GestorTraducciones.obtenerTexto(this, "lbl_existencia", "Existencia:"));
        txtprecio.setText(GestorTraducciones.obtenerTexto(this, "lbl_precio", "Precio:"));

        inputCodigo.setHint(GestorTraducciones.obtenerTexto(this, "hint_codigo", "Ingresa código..."));
        inputNombre.setHint(GestorTraducciones.obtenerTexto(this, "hint_nombre", "Ingresa nombre..."));
        inputDescripcion.setHint(GestorTraducciones.obtenerTexto(this, "hint_descripcion", "Ingresa descripción..."));
        inputExistencia.setHint(GestorTraducciones.obtenerTexto(this, "hint_existencia", "Ingresa existencia..."));
        inputPrecio.setHint(GestorTraducciones.obtenerTexto(this, "hint_precio", "Ingresa precio..."));

        btnGuardar.setText(GestorTraducciones.obtenerTexto(this, "btn_guardar", "Guardar"));
        btnConsultar.setText(GestorTraducciones.obtenerTexto(this, "btn_consultar", "Consultar"));
        btnEliminar.setText(GestorTraducciones.obtenerTexto(this, "btn_eliminar", "Eliminar"));
        btnModificar.setText(GestorTraducciones.obtenerTexto(this, "btn_modificar", "Modificar"));
        btnLimpiar.setText(GestorTraducciones.obtenerTexto(this, "btn_limpiar", "Limpiar"));
    }

    public void limpiarFormulario(View v) {
        limpiarCampos();
        inputCodigo.requestFocus();
        mostrarMensaje("Campos limpiados");
    }

    public void guardarProducto(View v) {
        if (!validarCamposCompletos()) {
            return;
        }

        Integer codigo = obtenerCodigoValido();
        Integer existencia = obtenerExistenciaValida();
        Double precio = obtenerPrecioValido();

        if (codigo == null || existencia == null || precio == null) {
            return;
        }

        String nombre = inputNombre.getText().toString().trim();
        String descripcion = inputDescripcion.getText().toString().trim();

        AdminSqLite admin = new AdminSqLite(this, DB_NOMBRE, null, DB_VERSION);
        SQLiteDatabase bd = null;
        Cursor fila = null;

        try {
            bd = admin.getWritableDatabase();
            fila = bd.rawQuery(
                    "select codigo from " + TABLA_PRODUCTOS + " where codigo=?",
                    new String[]{String.valueOf(codigo)}
            );

            if (fila.moveToFirst()) {
                inputCodigo.requestFocus();
                mostrarMensaje("Ya existe un producto con ese codigo");
                return;
            }

            ContentValues registro = new ContentValues();
            registro.put("codigo", codigo);
            registro.put("nombre", nombre);
            registro.put("descripcion", descripcion);
            registro.put("existencia", existencia);
            registro.put("precio", precio);

            long resultado = bd.insert(TABLA_PRODUCTOS, null, registro);
            if (resultado == -1) {
                mostrarMensaje("No se pudo guardar el producto");
                return;
            }

            limpiarCampos();
            mostrarMensaje("Producto guardado correctamente");
        } catch (SQLiteException e) {
            mostrarMensaje("Error al guardar en base de datos");
        } finally {
            if (fila != null) {
                fila.close();
            }
            if (bd != null) {
                bd.close();
            }
        }
    }

    public void consultarProducto(View v) {
        Integer codigo = obtenerCodigoValido();
        if (codigo == null) {
            return;
        }

        AdminSqLite admin = new AdminSqLite(this, DB_NOMBRE, null, DB_VERSION);
        SQLiteDatabase bd = null;
        Cursor fila = null;

        try {
            bd = admin.getReadableDatabase();
            fila = bd.rawQuery(
                    "select nombre, descripcion, existencia, precio from " + TABLA_PRODUCTOS + " where codigo=?",
                    new String[]{String.valueOf(codigo)}
            );

            if (fila.moveToFirst()) {
                inputNombre.setText(fila.getString(0));
                inputDescripcion.setText(fila.getString(1));
                inputExistencia.setText(String.valueOf(fila.getInt(2)));
                inputPrecio.setText(String.valueOf(fila.getDouble(3)));
                mostrarMensaje("Producto encontrado");
            } else {
                inputNombre.setText("");
                inputDescripcion.setText("");
                inputExistencia.setText("");
                inputPrecio.setText("");
                mostrarMensaje("No existe producto con ese codigo");
            }
        } catch (SQLiteException e) {
            mostrarMensaje("Error al consultar la base de datos");
        } finally {
            if (fila != null) {
                fila.close();
            }
            if (bd != null) {
                bd.close();
            }
        }
    }

    public void eliminarProducto(View v) {
        Integer codigo = obtenerCodigoValido();
        if (codigo == null) {
            return;
        }

        AdminSqLite admin = new AdminSqLite(this, DB_NOMBRE, null, DB_VERSION);
        SQLiteDatabase bd = null;

        try {
            bd = admin.getWritableDatabase();
            int bandera = bd.delete(TABLA_PRODUCTOS, "codigo=?", new String[]{String.valueOf(codigo)});

            if (bandera == 1) {
                limpiarCampos();
                mostrarMensaje("Producto eliminado");
            } else {
                mostrarMensaje("No existe producto en la base de datos");
            }
        } catch (SQLiteException e) {
            mostrarMensaje("Error al eliminar en base de datos");
        } finally {
            if (bd != null) {
                bd.close();
            }
        }
    }

    public void modificarProducto(View v) {
        if (!validarCamposCompletos()) {
            return;
        }

        Integer codigo = obtenerCodigoValido();
        Integer existencia = obtenerExistenciaValida();
        Double precio = obtenerPrecioValido();

        if (codigo == null || existencia == null || precio == null) {
            return;
        }

        String nombre = inputNombre.getText().toString().trim();
        String descripcion = inputDescripcion.getText().toString().trim();

        AdminSqLite admin = new AdminSqLite(this, DB_NOMBRE, null, DB_VERSION);
        SQLiteDatabase bd = null;

        try {
            bd = admin.getWritableDatabase();

            ContentValues registro = new ContentValues();
            registro.put("codigo", codigo);
            registro.put("nombre", nombre);
            registro.put("descripcion", descripcion);
            registro.put("existencia", existencia);
            registro.put("precio", precio);

            int cant = bd.update(TABLA_PRODUCTOS, registro, "codigo=?", new String[]{String.valueOf(codigo)});

            if (cant == 1) {
                mostrarMensaje("Datos actualizados correctamente");
            } else {
                mostrarMensaje("No existe producto en la base de datos");
            }
        } catch (SQLiteException e) {
            mostrarMensaje("Error al actualizar en base de datos");
        } finally {
            if (bd != null) {
                bd.close();
            }
        }
    }

    private boolean validarCamposCompletos() {
        if (inputCodigo.getText().toString().trim().isEmpty()) {
            inputCodigo.requestFocus();
            mostrarMensaje("El codigo es obligatorio");
            return false;
        }
        if (inputNombre.getText().toString().trim().isEmpty()) {
            inputNombre.requestFocus();
            mostrarMensaje("El nombre es obligatorio");
            return false;
        }
        if (inputDescripcion.getText().toString().trim().isEmpty()) {
            inputDescripcion.requestFocus();
            mostrarMensaje("La descripcion es obligatoria");
            return false;
        }
        if (inputExistencia.getText().toString().trim().isEmpty()) {
            inputExistencia.requestFocus();
            mostrarMensaje("La existencia es obligatoria");
            return false;
        }
        if (inputPrecio.getText().toString().trim().isEmpty()) {
            inputPrecio.requestFocus();
            mostrarMensaje("El precio es obligatorio");
            return false;
        }
        return true;
    }

    private Integer obtenerCodigoValido() {
        String codigoTexto = inputCodigo.getText().toString().trim();
        if (codigoTexto.isEmpty()) {
            inputCodigo.requestFocus();
            mostrarMensaje("El codigo es obligatorio");
            return null;
        }
        try {
            int codigo = Integer.parseInt(codigoTexto);
            if (codigo <= 0) {
                inputCodigo.requestFocus();
                mostrarMensaje("El codigo debe ser mayor que 0");
                return null;
            }
            return codigo;
        } catch (NumberFormatException e) {
            inputCodigo.requestFocus();
            mostrarMensaje("Codigo invalido");
            return null;
        }
    }

    private Integer obtenerExistenciaValida() {
        String existenciaTexto = inputExistencia.getText().toString().trim();
        try {
            int existencia = Integer.parseInt(existenciaTexto);
            if (existencia < 0) {
                inputExistencia.requestFocus();
                mostrarMensaje("La existencia no puede ser negativa");
                return null;
            }
            return existencia;
        } catch (NumberFormatException e) {
            inputExistencia.requestFocus();
            mostrarMensaje("Existencia invalida");
            return null;
        }
    }

    private Double obtenerPrecioValido() {
        String precioTexto = inputPrecio.getText().toString().trim();
        try {
            double precio = Double.parseDouble(precioTexto);
            if (precio <= 0) {
                inputPrecio.requestFocus();
                mostrarMensaje("El precio debe ser mayor que 0");
                return null;
            }
            return precio;
        } catch (NumberFormatException e) {
            inputPrecio.requestFocus();
            mostrarMensaje("Precio invalido");
            return null;
        }
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    private void limpiarCampos() {
        inputCodigo.setText("");
        inputNombre.setText("");
        inputDescripcion.setText("");
        inputExistencia.setText("");
        inputPrecio.setText("");
    }
}
