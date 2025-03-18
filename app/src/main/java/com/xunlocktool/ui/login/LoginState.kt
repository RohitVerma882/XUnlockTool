package com.xunlocktool.ui.login

import com.xunlocktool.data.auth.Auth

data class LoginState(
    val auth: Auth = Auth(),
    val isLoggedIn: Boolean = false
)