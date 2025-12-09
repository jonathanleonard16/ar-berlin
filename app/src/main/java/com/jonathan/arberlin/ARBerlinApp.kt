package com.jonathan.arberlin

import android.app.Application
import com.jonathan.arberlin.di.AppContainer
import com.jonathan.arberlin.di.DefaultAppContainer

class ARBerlinApp: Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}