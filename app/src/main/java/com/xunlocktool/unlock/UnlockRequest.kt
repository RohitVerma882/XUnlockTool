package com.xunlocktool.unlock

import com.xunlocktool.Constants
import com.xunlocktool.Constants.UNLOCK_HMAC_KEY
import com.xunlocktool.data.unlock.Auth2
import com.xunlocktool.utils.HttpQuery
import com.xunlocktool.utils.HttpUtils
import com.xunlocktool.utils.StringUtils

import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

import org.apache.commons.codec.binary.Base64

import org.json.JSONObject

import java.io.IOException

class UnlockRequest private constructor(
    private val auth: Auth2,
    private val host: String,
    private val path: String
) {
    private var params = HttpQuery()

    @Throws(UnlockException::class, IOException::class)
    fun exec(): String {
        val signHmac = UnlockCrypto.signHmac(
            UNLOCK_HMAC_KEY.toByteArray(),
            "POST",
            path,
            params.sorted().toString()
        )
        params.put("sign", signHmac)
        params = params.sorted()

        try {
            UnlockCrypto.encryptRequestParams(auth.ssecurity, params)
        } catch (e: Exception) {
            throw UnlockException("Cannot encrypt post params: ${e.message}")
        }

        val signSha = UnlockCrypto.signSha1(auth.ssecurity, "POST", path, params.toString())
        params.put("signature", signSha)

        val client = OkHttpClient()
            .newBuilder()
            .followRedirects(false)
            .build()

        val url = HttpUrl.Builder()
            .scheme("https")
            .host(host)
            .addPathSegments(path.removePrefix("/"))
            .build()

        val cookiesMap = linkedMapOf(
            "serviceToken" to auth.serviceToken,
            "userId" to auth.userId,
            "unlockApi_slh" to auth.unlockApiSlh,
            "unlockApi_ph" to auth.unlockApiPh
        )

        val formBody = FormBody.Builder().apply {
            params.forEach { (key, value) ->
                add(key!!, value.toString())
            }
        }.build()

        var request = Request.Builder()
            .url(url)
            .header("Cookie", HttpUtils.map2CookieString(cookiesMap))
            .header("User-Agent", "XiaomiPCSuite")
            .post(formBody)
            .build()

        client.newCall(request).execute().use { response ->
            var body = response.body?.string()
            if (!response.isSuccessful || body.isNullOrEmpty()) {
                throw UnlockException("Invalid server response: code: ${response.code}, length: ${body?.length ?: 0}")
            }

            try {
                body = UnlockCrypto.decrypt(auth.ssecurity, body)
            } catch (e: Exception) {
                throw UnlockException("Cannot decrypt response data: ${e.message}")
            }

            try {
                body = String(Base64.decodeBase64(body))
            } catch (_: Throwable) {

            }
            return body
        }
    }

    fun addParam(key: String, value: Any?) {
        params.put(key, value)
    }

    @Throws(UnlockException::class, IOException::class)
    fun addNonce() {
        val body = nonceV2(auth, host)
        try {
            val jsonObject = JSONObject(body)
            val code = jsonObject.getInt("code")
            if (code != 0) {
                throw UnlockException("Response code of nonce request is not zero: $code")
            }
            val nonce = jsonObject.getString("nonce")
            params.put("nonce", nonce)
        } catch (e: Exception) {
            throw UnlockException("Exception while parsing nonce response: ${e.message}")
        }
    }

    companion object {
        @Throws(UnlockException::class, IOException::class)
        @JvmStatic
        private fun nonceV2(auth: Auth2, host: String): String {
            val request = UnlockRequest(auth, host, Constants.NONCEV2)
            request.addParam("r", StringUtils.randomWord(16).lowercase())
            request.addParam("sid", Constants.SID)
            return request.exec()
        }

        @Throws(UnlockException::class, IOException::class)
        @JvmStatic
        fun userInfo(auth: Auth2, host: String): String {
            val request = UnlockRequest(auth, host, Constants.USERINFOV3)
            val dataMap = linkedMapOf(
                "clientId" to "1",
                "clientVersion" to Constants.CLIENT_VERSION,
                "language" to "en",
                "pcId" to auth.pcId,
                "region" to "",
                "uid" to auth.userId
            )
            var data = JSONObject(dataMap).toString(3)
            data = Base64.encodeBase64String(data.toByteArray())
            request.addParam("data", data)
            request.addNonce()
            request.addParam("sid", Constants.SID)
            return request.exec()
        }

        @Throws(UnlockException::class, IOException::class)
        @JvmStatic
        fun deviceClear(auth: Auth2, host: String, product: String): String {
            val request = UnlockRequest(auth, host, Constants.DEVICECLEARV3)
            val dataMap = linkedMapOf(
                "clientId" to "1",
                "clientVersion" to Constants.CLIENT_VERSION,
                "language" to "en",
                "pcId" to auth.pcId,
                "product" to product,
                "region" to ""
            )
            var data = JSONObject(dataMap).toString(3)
            data = Base64.encodeBase64String(data.toByteArray())
            request.addParam("appId", "1")
            request.addParam("data", data)
            request.addNonce()
            request.addParam("sid", Constants.SID)
            return request.exec()
        }

        @Throws(UnlockException::class, IOException::class)
        @JvmStatic
        fun ahaUnlock(auth: Auth2, host: String, product: String, token: String): String {
            val request = UnlockRequest(auth, host, Constants.AHAUNLOCKV3)
            val deviceInfoMap = linkedMapOf(
                "boardVersion" to "",
                "deviceName" to "",
                "product" to product,
                "socId" to ""
            )
            val dataMap = linkedMapOf(
                "clientId" to "2",
                "clientVersion" to Constants.CLIENT_VERSION,
                "deviceInfo" to deviceInfoMap,
                "deviceToken" to token,
                "language" to "en",
                "operate" to "unlock",
                "pcId" to auth.pcId,
                "region" to "",
                "uid" to auth.userId
            )
            var data = StringUtils.map2json(dataMap, 3)
            data = Base64.encodeBase64String(data.toByteArray())
            request.addParam("appId", "1")
            request.addParam("data", data)
            request.addNonce()
            request.addParam("sid", Constants.SID)
            return request.exec()
        }
    }
}