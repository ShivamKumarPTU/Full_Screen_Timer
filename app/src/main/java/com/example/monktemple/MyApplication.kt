package com.example.monktemple

import android.app.Application
import com.example.monktemple.Utlis.DialogManager
import com.example.monktemple.Utlis.SessionManager

class MyApplication: Application() {
    override fun onCreate(){
        super.onCreate()
        initializeAppComponents()
    }
    private fun initializeAppComponents() {
        // Pre-initialize SessionManager
        SessionManager(this)


        // Pre-load any other critical components
        // This runs before any activity starts

        // Note: Don't do heavy work here, just light initialization
    }
}