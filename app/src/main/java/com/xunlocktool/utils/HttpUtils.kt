package com.xunlocktool.utils

import android.os.Build

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.Charset

object HttpUtils {
    @Suppress("DEPRECATION")
    fun encodeUrl(url: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            URLEncoder.encode(url, Charset.forName("UTF-8"))
        } else {
            URLEncoder.encode(url)
        }
    } catch (_: UnsupportedEncodingException) {
        null
    }

    fun map2CookieString(data: Map<String, String>): String {
        val cookieString = StringBuilder()
        for (entry in data.entries) {
            cookieString.apply {
                append(entry.key)
                append("=")
                append(entry.value)
                append("; ")
            }
        }
        return cookieString.toString()
    }
}