package com.xunlocktool

import com.xunlocktool.data.unlock.Meaning
import com.xunlocktool.data.unlock.Region

object Constants {
    const val GITHUB_SOURCE_CODE_URL = "https://github.com/RohitVerma882/XUnlockTool"

    const val LOGIN_URL =
        "https://account.xiaomi.com/pass/serviceLogin?sid=unlockApi&json=false&passive=true&hidden=false&_snsDefault=facebook&checkSafePhone=true&_locale=en"
    const val LOGIN_URL2 =
        "https://account.xiaomi.com/pass/serviceLogin?sid=unlockApi&_json=true&passive=true&hidden=false"

    const val SID = "miui_unlocktool_client"
    const val CLIENT_VERSION = "5.5.224.55"
    const val NONCEV2 = "/api/v2/nonce"
    const val USERINFOV3 = "/api/v3/unlock/userinfo"
    const val DEVICECLEARV3 = "/api/v2/unlock/device/clear"
    const val AHAUNLOCKV3 = "/api/v3/ahaUnlock"

    const val UNLOCK_HMAC_KEY = "2tBeoEyJTunmWUGq7bQH2Abn0k2NhhurOaqBfyxCuLVgn4AVj7swcawe53uDUno"
    const val DEFAULT_IV = "0102030405060708"

    // @formatter:off
    val regions = listOf(
        Region("Global", "unlock.update.intl.miui.com"),
        Region("India", "in-unlock.update.intl.miui.com"),
        Region("Russia", "ru-unlock.update.intl.miui.com"),
        Region("Europe", "eu-unlock.update.intl.miui.com"),
        Region("China", "unlock.update.miui.com")
    )

    val meanings = listOf(
        Meaning(10000, "Request parameter error (This can be caused by entering invalid token or product)"),
        Meaning(10001, "Signature verification failed"),
        Meaning(10002, "The same IP request too often (Too many tries)"),
        Meaning(10003, "Internal server error"),
        Meaning(10004, "Request has expired"),
        Meaning(10005, "Invalid Nonce request"),
        Meaning(10006, "Client version is too low"),
        Meaning(20030, "You have already unlocked a device recently\nYou should wait at least 30 days from the first unlock to unlock another device"),
        Meaning(20031, "This device is not bound to your account\nTurn on your device and bind your account to the device by going in MIUI's Settings > Developer options > Mi Unlock status"),
        Meaning(20032, "Failed to generate signature value required to unlock"),
        Meaning(20033, "User portrait scores too low or black"),
        Meaning(20034, "Current account cannot unlock this device"),
        Meaning(20035, "This tool is outdated, contact the developers"),
        Meaning(20036, "Your account has been bound to this device for not enough time\nYou have to wait %d days and %d hours before you can unlock this device"),
        Meaning(20037, "Unlock number has reached the upper limit"),
        Meaning(20041, "Your Xiaomi account isn't associated with a phone number\nGo to account.xiaomi.com and associate it with your phone number")
    )
    // @formatter:on
}