package com.xunlocktool.data.auth

import android.content.Context

import androidx.datastore.core.DataStore
import androidx.datastore.dataStore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.authDataStore: DataStore<Auth> by dataStore(
    fileName = "auth.json",
    serializer = AuthSerializer
)

class AuthDataSource(context: Context) {
    private val dataStore: DataStore<Auth> = context.authDataStore

    suspend fun saveAuth(userId: String, passToken: String, deviceId: String) {
        dataStore.updateData { Auth(userId, passToken, deviceId) }
    }

    suspend fun clearAuth() {
        dataStore.updateData { Auth() }
    }

    val auth: Flow<Auth> = dataStore.data.map { auth ->
        Auth(auth.userId, auth.passToken, auth.deviceId)
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { auth ->
        auth.userId.isNotEmpty() && auth.passToken.isNotEmpty() && auth.deviceId.isNotEmpty()
    }
}