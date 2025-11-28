package com.cxt.robertytocerva.fenixbt

import android.R.attr.delay
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Splash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        GlobalScope.launch {
            delay(3000) // 2 segundos
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@Splash, MainActivity::class.java))
                finish() // Evita que regresen con el botón atrás
            }
        }
    }
}