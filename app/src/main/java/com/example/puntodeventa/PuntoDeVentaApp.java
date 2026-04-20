package com.example.puntodeventa;

import android.app.Application;

public class PuntoDeVentaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applySavedTheme(this);
    }
}