package com.xut

import android.content.Context

import com.xut.data.UserRepositoryImpl
import com.xut.domain.repository.UserRepository

interface AppContainer {
    val userRepository: UserRepository
}

class AppContainerImpl(private val context: Context) : AppContainer {
    override val userRepository: UserRepository get() = UserRepositoryImpl(context)
}