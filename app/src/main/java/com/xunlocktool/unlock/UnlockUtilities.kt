package com.xunlocktool.unlock

import com.xunlocktool.Constants
import com.xunlocktool.crypto.Hash
import com.xunlocktool.utils.HttpUtils

import org.json.JSONObject

object UnlockUtilities {
    fun getMeaning(code: Int, jsonObject: JSONObject): String {
        val meaning = Constants.meanings.find { it.code == code }
        if (meaning == null) return "Unknown"
        if (code == 20036) {
            val hours = try {
                jsonObject.getJSONObject("data").getInt("waitHour")
            } catch (_: Throwable) {
                -1
            }
            return if (hours >= 0) {
                val days = hours / 24
                val leftHours = hours % 24
                String.format(meaning.message, days, leftHours)
            } else {
                "You have to wait some days before you can unlock your device"
            }
        }
        return meaning.message
    }

    fun signLocation(location: String?, ssecurity: String?, nonce: String?): String? {
        if (location == null || ssecurity == null || nonce == null) return null
        val sign = HttpUtils.encodeUrl(Hash.sha1Base64("nonce=$nonce&$ssecurity")) ?: return null
        return "$location&clientSign=$sign"
    }

    fun findJsonStart(json: String?): String? {
        if (json == null) return null
        val chars = json.toCharArray()
        for (i in chars.indices) {
            if (chars[i] == '{') {
                return json.substring(i)
            }
        }
        return null
    }
}