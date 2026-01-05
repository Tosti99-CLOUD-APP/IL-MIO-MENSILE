package com.tostiapp.a1

import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    // L'override di attachBaseContext non è più necessario.
    // AppCompatDelegate gestisce automaticamente l'impostazione della lingua.
}
