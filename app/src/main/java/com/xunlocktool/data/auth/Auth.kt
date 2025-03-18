package com.xunlocktool.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class Auth(
    val userId: String = "",
    val passToken: String = "",
    val deviceId: String = ""
)