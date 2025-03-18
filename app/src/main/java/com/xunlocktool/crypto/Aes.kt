package com.xunlocktool.crypto

import android.security.keystore.KeyProperties

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Aes {
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"

    @Throws(Exception::class)
    fun aes128CbcEncrypt(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        return aes128CbcEncryptDecrypt(Cipher.ENCRYPT_MODE, key, iv, data)
    }

    @Throws(Exception::class)
    fun aes128CbcDecrypt(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        return aes128CbcEncryptDecrypt(Cipher.DECRYPT_MODE, key, iv, data)
    }

    @Throws(Exception::class)
    private fun aes128CbcEncryptDecrypt(
        mode: Int,
        key: ByteArray,
        iv: ByteArray,
        data: ByteArray
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(mode, keySpec, ivSpec)
        return cipher.doFinal(data)
    }
}