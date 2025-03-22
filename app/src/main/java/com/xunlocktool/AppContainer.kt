package com.xunlocktool

import android.content.Context

import androidx.work.WorkManager

import com.xunlocktool.data.auth.AuthDataStore

class AppContainer(context: Context) {
    val authDataStore: AuthDataStore = AuthDataStore(context)
    val workManager: WorkManager = WorkManager.getInstance(context)
}