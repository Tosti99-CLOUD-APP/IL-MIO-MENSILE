package com.tostiapp.a1

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val language = sharedPreferences.getString("language", "system")
        if (language != "system") {
            val locale = LocaleListCompat.forLanguageTags(language)
            AppCompatDelegate.setApplicationLocales(locale)
        }
    }
}