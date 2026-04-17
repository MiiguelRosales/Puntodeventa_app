package com.example.puntodeventa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PantallaInicial extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.pantalla_inicial);

        Button btnProductos = findViewById(R.id.btnProductos);
        Button btnVentas = findViewById(R.id.btnVentas);

        btnProductos.setOnClickListener(v -> abrirPantallaProductos());
        btnVentas.setOnClickListener(v -> abrirPantallaVentas());

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
}