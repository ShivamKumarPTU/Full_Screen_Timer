package com.example.monktemple
import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp // This annotation tells Hilt to generate the necessary code
class MonkTempleApp : Application() {
    // You can leave the body empty for now.
    // Hilt uses this class as an entry point for dependency injection.
}