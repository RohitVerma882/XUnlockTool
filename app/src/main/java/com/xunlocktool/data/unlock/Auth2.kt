package com.xunlocktool.data.unlock

data class Auth2(
    val userId: String = "",
    val pcId: String = "",
    val ssecurity: String = "",
    var serviceToken: String = "",
    var unlockApiSlh: String = "",
    var unlockApiPh: String = ""
)
