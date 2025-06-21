package com.xut.domain.repository

import com.xut.domain.model.User

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val user: Flow<User>
    val hasUser: Flow<Boolean>

    suspend fun save(user: User)
    suspend fun clear()
}