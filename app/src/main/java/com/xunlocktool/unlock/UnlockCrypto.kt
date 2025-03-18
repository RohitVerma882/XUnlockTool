package com.xunlocktool.unlock

import com.xunlocktool.Constants.DEFAULT_IV
import com.xunlocktool.crypto.Aes
import com.xunlocktool.crypto.Hash
import com.xunlocktool.utils.HttpQuery

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils

import java.nio.charset.StandardCharsets

object UnlockCrypto {
    @Throws(Exception::class)
    fun encrypt(key: String, data: String): String {
        val decodedKey = Base64.decodeBase64(key)
        val encryptedData = Aes.aes128CbcEncrypt(
            decodedKey,
            DEFAULT_IV.toByteArray(StandardCharsets.ISO_8859_1),
            data.toByteArray()
        )
        return Base64.encodeBase64String(encryptedData)
    }

    @Throws(Exception::class)
    fun decrypt(key: String, data: String): String {
        val decodedKey = Base64.decodeBase64(key)
        val decodedData = Base64.decodeBase64(data)
        val decryptedData = Aes.aes128CbcDecrypt(
            decodedKey,
            DEFAULT_IV.toByteArray(StandardCharsets.ISO_8859_1),
            decodedData
        )
        return String(decryptedData)
    }

    @Throws(Exception::class)
    fun encryptRequestParams(key: String, params: HttpQuery) {
        for (entry in params.entries) {
            entry.setValue(encrypt(key, entry.value.toString()))
        }
    }

    fun signHmac(hmacKey: ByteArray, method: String, path: String, query: String): String {
        val hmacData = String.format("%s\n%s\n%s", method, path, query)
        return HmacUtils(HmacAlgorithms.HMAC_SHA_1, hmacKey).hmacHex(hmacData.toByteArray())
    }

    fun signSha1(key: String, method: String, path: String, query: String): String {
        val shaData = String.format("%s&%s&%s&%s", method, path, query, key)
        return Hash.sha1Base64(shaData)
    }
}