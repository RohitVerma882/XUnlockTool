package com.xunlocktool

import android.content.Context

import com.xunlocktool.data.auth.AuthDataStore

interface AppContainer {
    val authDataStore: AuthDataStore
}

class AppContainerImpl(private val context: Context) : AppContainer {
    override val authDataStore: AuthDataStore
        get() = AuthDataStore(context)
}