package com.xunlocktool

import android.content.Context

import androidx.work.WorkManager

import com.xunlocktool.data.auth.AuthDataSource

class AppContainer(context: Context) {
    val authDataSource: AuthDataSource = AuthDataSource(context)
    val workManager: WorkManager = WorkManager.getInstance(context)
}