package com.xut.crypto

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils

object Hash {
    fun sha1Hex(data: String): String = DigestUtils.sha1Hex(data)

    fun sha1Base64(data: String): String = Base64.encodeBase64String(DigestUtils.sha1(data))

    fun md5Hex(data: String): String = DigestUtils.md5Hex(data)
}