package com.example.puntodeventa;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class PantallaInicial extends AppCompatActivity {

    private Button btnProductos;
    private Button btnVentas;
    private Button btnConfiguracion;
    private Button btnIdioma;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.pantalla_inicial);

        btnProductos = findViewById(R.id.btnProductos);
        btnVentas = findViewById(R.id.btnVentas);
        btnConfiguracion = findViewById(R.id.btnConfiguracion);
        btnIdioma = findViewById(R.id.btnIdioma);

        aplicarTextosTraducidos(btnProductos, btnVentas, btnConfiguracion, btnIdioma);

        btnProductos.setOnClickListener(v -> abrirPantallaProductos());
        btnVentas.setOnClickListener(v -> abrirPantallaVentas());
        btnConfiguracion.setOnClickListener(v -> abrirPantallaConfiguracion());
        btnIdioma.setOnClickListener(v -> {
            GestorTraducciones.alternarIdioma(this);
            aplicarTextosTraducidos(btnProductos, btnVentas, btnConfiguracion, btnIdioma);
        });

        aplicarConfiguracionVisual(btnProductos, btnVentas, btnConfiguracion, btnIdioma);

        iniciarAnimacionTitulo();
        iniciarAnimacionLoader();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void abrirPantallaProductos() {
        startActivity(new Intent(this, PantallaProductos.class));
    }

    private void abrirPantallaVentas() {
        startActivity(new Intent(this, PantallaVentas.class));
    }

    private void abrirPantallaConfiguracion() {
        startActivity(new Intent(this, PantallaConfiguracion.class));
    }

    private void aplicarTextosTraducidos(Button btnProductos, Button btnVentas, Button btnConfiguracion, Button btnIdioma) {
        btnProductos.setText(GestorTraducciones.obtenerTexto(this, "btn_productos", "Productos"));
        btnVentas.setText(GestorTraducciones.obtenerTexto(this, "btn_ventas", "Ventas"));
        btnConfiguracion.setText(GestorTraducciones.obtenerTexto(this, "btn_configuracion", "Configuracion"));
        btnIdioma.setText(GestorTraducciones.obtenerTexto(this, "btn_idioma", "Cambiar idioma"));
    }

    private void iniciarAnimacionTitulo() {
        LinearLayout rowTop = findViewById(R.id.titleRowTop);
        LinearLayout rowBottom = findViewById(R.id.titleRowBottom);
        String nombreTienda = AppConfigManager.obtenerNombreTienda(this).toUpperCase(Locale.getDefault());
        String[] lineas = separarNombreTienda(nombreTienda);

        crearTituloAnimado(rowTop, lineas[0], 0);
        if (lineas[1].isEmpty()) {
            rowBottom.removeAllViews();
        } else {
            crearTituloAnimado(rowBottom, lineas[1], 3);
        }
    }

    private void aplicarConfiguracionVisual(Button btnProductos, Button btnVentas, Button btnConfiguracion, Button btnIdioma) {
        AppConfigManager.Configuracion config = AppConfigManager.obtenerConfiguracion(this);

        AppConfigManager.aplicarColorEnfasis(
                this,
                config.apariencia.colorEnfasis,
                btnProductos,
                btnVentas,
                btnConfiguracion,
                btnIdioma
        );

        AppConfigManager.aplicarEscalaTexto(findViewById(R.id.main), config.apariencia.tamanoTexto);
    }

    @Override
    protected void onResume() {
        super.onResume();
        aplicarTextosTraducidos(btnProductos, btnVentas, btnConfiguracion, btnIdioma);
        aplicarConfiguracionVisual(btnProductos, btnVentas, btnConfiguracion, btnIdioma);
        iniciarAnimacionTitulo();
    }

    private String[] separarNombreTienda(String nombreTienda) {
        String limpio = nombreTienda == null ? "" : nombreTienda.trim();
        if (limpio.isEmpty()) {
            return new String[]{"PUNTO", "DE VENTA"};
        }

        if (!limpio.contains(" ")) {
            return new String[]{limpio, ""};
        }

        int mitad = limpio.length() / 2;
        int corte = limpio.indexOf(' ', mitad);
        if (corte < 0) {
            corte = limpio.lastIndexOf(' ', mitad);
        }

        if (corte <= 0 || corte >= limpio.length() - 1) {
            return new String[]{limpio, ""};
        }

        String linea1 = limpio.substring(0, corte).trim();
        String linea2 = limpio.substring(corte + 1).trim();
        return new String[]{linea1, linea2};
    }

    private void crearTituloAnimado(LinearLayout contenedor, String texto, int delayOffset) {
        contenedor.removeAllViews();
        float startOffset = dp(28);
        PathInterpolator interpolador = new PathInterpolator(0.86f, 0f, 0.07f, 1f);

        for (int i = 0; i < texto.length(); i++) {
            char letra = texto.charAt(i);
            View vista;

            if (letra == ' ') {
                Space espacio = new Space(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) dp(14), 1);
                espacio.setLayoutParams(params);
                vista = espacio;
            } else {
                TextView tv = new TextView(this);
                tv.setText(String.valueOf(letra));
                tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
                tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMarginEnd((int) dp(2));
                tv.setLayoutParams(params);
                vista = tv;
            }

            vista.setTranslationY(startOffset);
            contenedor.addView(vista);

            ObjectAnimator animador = ObjectAnimator.ofFloat(vista, "translationY", startOffset, 0f);
            animador.setDuration(1000);
            animador.setStartDelay((long) (70 * (i + delayOffset)));
            animador.setRepeatCount(ObjectAnimator.INFINITE);
            animador.setRepeatMode(ObjectAnimator.REVERSE);
            animador.setInterpolator(interpolador);
            animador.start();
        }
    }

    private void iniciarAnimacionLoader() {
        ImageView truckBody = findViewById(R.id.truckBody);
        ImageView tireLeft = findViewById(R.id.tireLeft);
        ImageView tireRight = findViewById(R.id.tireRight);
        View roadDash1 = findViewById(R.id.roadDash1);
        View roadDash2 = findViewById(R.id.roadDash2);

        ObjectAnimator bounce = ObjectAnimator.ofFloat(truckBody, "translationY", 0f, 3f, 0f);
        bounce.setDuration(1000);
        bounce.setRepeatCount(ObjectAnimator.INFINITE);
        bounce.setInterpolator(new LinearInterpolator());
        bounce.start();

        ObjectAnimator wheelLeft = ObjectAnimator.ofFloat(tireLeft, "rotation", 0f, 360f);
        wheelLeft.setDuration(650);
        wheelLeft.setRepeatCount(ObjectAnimator.INFINITE);
        wheelLeft.setInterpolator(new LinearInterpolator());
        wheelLeft.start();

        ObjectAnimator wheelRight = ObjectAnimator.ofFloat(tireRight, "rotation", 0f, 360f);
        wheelRight.setDuration(650);
        wheelRight.setRepeatCount(ObjectAnimator.INFINITE);
        wheelRight.setInterpolator(new LinearInterpolator());
        wheelRight.start();

        ObjectAnimator dash1 = ObjectAnimator.ofFloat(roadDash1, "translationX", 0f, -350f);
        dash1.setDuration(1400);
        dash1.setRepeatCount(ObjectAnimator.INFINITE);
        dash1.setInterpolator(new LinearInterpolator());
        dash1.start();

        ObjectAnimator dash2 = ObjectAnimator.ofFloat(roadDash2, "translationX", 0f, -350f);
        dash2.setDuration(1400);
        dash2.setStartDelay(250);
        dash2.setRepeatCount(ObjectAnimator.INFINITE);
        dash2.setInterpolator(new LinearInterpolator());
        dash2.start();
    }

    private float dp(int valor) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                valor,
                getResources().getDisplayMetrics()
        );
    }
}