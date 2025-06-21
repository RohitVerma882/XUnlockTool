package com.xut.data

import android.content.Context

import androidx.datastore.core.DataStore
import androidx.datastore.dataStore

import com.xut.domain.model.User
import com.xut.domain.repository.UserRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore: DataStore<User> by dataStore(
    fileName = "user.json",
    serializer = UserSerializer
)

class UserRepositoryImpl(context: Context) : UserRepository {
    private val dataStore: DataStore<User> = context.userDataStore

    override val user: Flow<User> get() = dataStore.data

    override val hasUser: Flow<Boolean> get() = dataStore.data.map { user -> user.hasValues }

    override suspend fun save(user: User) {
        dataStore.updateData { user }
    }

    override suspend fun clear() {
        dataStore.updateData { User() }
    }
}