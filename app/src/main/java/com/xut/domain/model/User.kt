package com.xut.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId: String = "",
    val passToken: String = "",
    val deviceId: String = ""
) {
    val hasValues: Boolean
        get() = passToken.isNotEmpty() && userId.isNotEmpty() && deviceId.isNotEmpty()
}