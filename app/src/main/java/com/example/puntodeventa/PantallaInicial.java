package com.example.puntodeventa;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PantallaInicial extends AppCompatActivity {

    private static final long ANIMATION_DURATION_MS = 1000L;
    private static final long LETTER_DELAY_MS = 70L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.pantalla_inicial);

        TextView titulo = findViewById(R.id.tvTitulo);
        LinearLayout loaderContainer = findViewById(R.id.loaderContainer);
        titulo.setOnClickListener(v -> abrirPantallaProductos());
        loaderContainer.setOnClickListener(v -> abrirPantallaProductos());
        crearAnimacionPrompt(loaderContainer, getString(R.string.tap_to_start));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void crearAnimacionPrompt(LinearLayout contenedor, String texto) {
        contenedor.removeAllViews();
        float startOffset = dp(24);
        PathInterpolator interpolador = new PathInterpolator(0.86f, 0f, 0.07f, 1f);

        for (int i = 0; i < texto.length(); i++) {
            char caracter = texto.charAt(i);
            View vista;

            if (caracter == ' ') {
                Space espacio = new Space(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) dp(10), 1);
                espacio.setLayoutParams(params);
                vista = espacio;
            } else {
                TextView letra = new TextView(this);
                letra.setText(String.valueOf(caracter));
                letra.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
                letra.setTypeface(letra.getTypeface(), android.graphics.Typeface.BOLD);
                letra.setOnClickListener(v -> abrirPantallaProductos());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMarginEnd((int) dp(1));
                letra.setLayoutParams(params);
                vista = letra;
            }

            vista.setTranslationY(startOffset);
            contenedor.addView(vista);

            ObjectAnimator animador = ObjectAnimator.ofFloat(vista, "translationY", startOffset, 0f);
            animador.setDuration(ANIMATION_DURATION_MS);
            animador.setStartDelay(LETTER_DELAY_MS * i);
            animador.setRepeatCount(ValueAnimator.INFINITE);
            animador.setRepeatMode(ValueAnimator.REVERSE);
            animador.setInterpolator(interpolador);
            animador.start();
        }
    }

    private void abrirPantallaProductos() {
        startActivity(new Intent(this, PantallaProductos.class));
    }

    private float dp(int valor) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                valor,
                getResources().getDisplayMetrics()
        );
    }
}