package com.tostiapp.a1

import android.app.Application

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // L'impostazione della lingua con AppCompatDelegate.setApplicationLocales()
        // viene mantenuta automaticamente tra i riavvii dell'app.
        // Non è necessario aggiungere altro codice qui.
    }
}
