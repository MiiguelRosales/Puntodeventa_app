package com.example.puntodeventa;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class PantallaConfiguracion extends AppCompatActivity {

    private EditText etNombreTienda;
    private EditText etColorEnfasis;
    private Spinner spinnerTamanoTexto;
    private Button btnModoOscuro;
    private Button btnGuardarConfiguracion;

    private final List<String> valoresTamano = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.pantalla_configuracion);

        View root = findViewById(R.id.mainConfiguracion);
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

        etNombreTienda = findViewById(R.id.etNombreTienda);
        etColorEnfasis = findViewById(R.id.etColorEnfasis);
        spinnerTamanoTexto = findViewById(R.id.spinnerTamanoTexto);
        btnModoOscuro = findViewById(R.id.btnModoOscuro);
        btnGuardarConfiguracion = findViewById(R.id.btnGuardarConfiguracion);
        Button btnVolver = findViewById(R.id.btnVolverConfiguracion);

        configurarSpinnerTamanoTexto();
        aplicarTextosTraducidos(btnVolver);
        cargarValoresActuales();
        aplicarEstilosVisuales(btnVolver);

        btnModoOscuro.setOnClickListener(v -> {
            ThemeManager.toggleTheme(this);
            actualizarTextoBotonTema();
            recreate();
        });

        btnGuardarConfiguracion.setOnClickListener(v -> guardarConfiguracion());
        btnVolver.setOnClickListener(v -> finish());
    }

    private void configurarSpinnerTamanoTexto() {
        valoresTamano.clear();
        valoresTamano.add("pequeno");
        valoresTamano.add("mediano");
        valoresTamano.add("grande");

        List<String> etiquetas = new ArrayList<>();
        etiquetas.add(GestorTraducciones.obtenerTexto(this, "tam_texto_pequeno", "Pequeno"));
        etiquetas.add(GestorTraducciones.obtenerTexto(this, "tam_texto_mediano", "Mediano"));
        etiquetas.add(GestorTraducciones.obtenerTexto(this, "tam_texto_grande", "Grande"));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                etiquetas
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTamanoTexto.setAdapter(adapter);
    }

    private void aplicarTextosTraducidos(Button btnVolver) {
        TextView tvTitulo = findViewById(R.id.tvTituloConfiguracion);
        TextView lblNombreTienda = findViewById(R.id.lblNombreTienda);
        TextView lblColorEnfasis = findViewById(R.id.lblColorEnfasis);
        TextView lblTamanoTexto = findViewById(R.id.lblTamanoTexto);

        tvTitulo.setText(GestorTraducciones.obtenerTexto(this, "lbl_titulo_configuracion", "Configuracion"));
        lblNombreTienda.setText(GestorTraducciones.obtenerTexto(this, "lbl_nombre_tienda", "Nombre de tienda:"));
        lblColorEnfasis.setText(GestorTraducciones.obtenerTexto(this, "lbl_color_enfasis", "Color de enfasis (#RRGGBB):"));
        lblTamanoTexto.setText(GestorTraducciones.obtenerTexto(this, "lbl_tamano_texto", "Tamano de texto:"));

        etNombreTienda.setHint(GestorTraducciones.obtenerTexto(this, "hint_nombre_tienda", "Escribe el nombre de tu tienda"));
        etColorEnfasis.setHint(GestorTraducciones.obtenerTexto(this, "hint_color_enfasis", "Ejemplo: #4CAF50"));

        btnGuardarConfiguracion.setText(GestorTraducciones.obtenerTexto(this, "btn_guardar_configuracion", "Guardar configuracion"));
        btnVolver.setText(GestorTraducciones.obtenerTexto(this, "btn_volver", "Volver"));
        actualizarTextoBotonTema();
    }

    private void cargarValoresActuales() {
        AppConfigManager.Configuracion config = AppConfigManager.obtenerConfiguracion(this);

        etNombreTienda.setText(config.negocio.nombreTienda);
        etColorEnfasis.setText(config.apariencia.colorEnfasis);

        int posicion = valoresTamano.indexOf(config.apariencia.tamanoTexto);
        spinnerTamanoTexto.setSelection(posicion >= 0 ? posicion : 1);
    }

    private void aplicarEstilosVisuales(Button btnVolver) {
        AppConfigManager.Configuracion config = AppConfigManager.obtenerConfiguracion(this);

        AppConfigManager.aplicarColorEnfasis(
                this,
                config.apariencia.colorEnfasis,
                btnModoOscuro,
                btnGuardarConfiguracion,
                btnVolver
        );

        AppConfigManager.aplicarEscalaTexto(findViewById(R.id.mainConfiguracion), config.apariencia.tamanoTexto);
    }

    private void guardarConfiguracion() {
        String nombre = etNombreTienda.getText().toString().trim();
        String color = etColorEnfasis.getText().toString().trim();
        String tamano = valoresTamano.get(spinnerTamanoTexto.getSelectedItemPosition());

        boolean guardado = AppConfigManager.guardarConfiguracion(this, nombre, color, tamano);
        if (!guardado) {
            mostrarMensaje(GestorTraducciones.obtenerTexto(this, "msg_error_guardar_config", "No se pudo guardar la configuracion"));
            return;
        }

        mostrarMensaje(GestorTraducciones.obtenerTexto(this, "msg_config_guardada", "Configuracion guardada"));
        recreate();
    }

    private void actualizarTextoBotonTema() {
        if (ThemeManager.isNight(this)) {
            btnModoOscuro.setText(GestorTraducciones.obtenerTexto(this, "btn_tema_claro", "Modo claro"));
        } else {
            btnModoOscuro.setText(GestorTraducciones.obtenerTexto(this, "btn_tema_oscuro", "Modo oscuro"));
        }
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
}
