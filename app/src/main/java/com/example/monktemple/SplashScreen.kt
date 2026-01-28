package com.example.monktemple

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.Utlis.ShowToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import kotlin.jvm.java

class SplashScreen : AppCompatActivity() {

private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen=installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
       splashScreen.setKeepOnScreenCondition { false }
    var splashScreenMessage=findViewById<TextView>(R.id.splashScreenText)
        sessionManager=SessionManager(this)
        if(sessionManager.getUserName().isNullOrEmpty()){
            splashScreenMessage.setText("Hello Guest Welcome")
        }
        else {
            splashScreenMessage.setText("Hello ${sessionManager.getUserName()} Welcome")
        }


        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LauncherActivity::class.java))
            finish()
        }, 2000)
    }

}