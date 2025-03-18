package com.xunlocktool.ui.unlock

data class UnlockState(
    val isRunning: Boolean = false,
    val output: String? = null,
    val token: String? = null,
)