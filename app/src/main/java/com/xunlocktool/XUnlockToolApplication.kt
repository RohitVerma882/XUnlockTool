package com.xunlocktool

import android.app.Application
import android.util.Log

import androidx.work.Configuration

class XUnlockToolApplication : Application(), Configuration.Provider {
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()
}