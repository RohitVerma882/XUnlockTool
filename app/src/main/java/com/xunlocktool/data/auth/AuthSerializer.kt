package com.xunlocktool.data.auth

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer

import kotlinx.serialization.json.Json

import java.io.InputStream
import java.io.OutputStream

object AuthSerializer : Serializer<Auth> {
    override val defaultValue: Auth get() = Auth()

    override suspend fun readFrom(input: InputStream): Auth {
        return try {
            Json.decodeFromString(input.readBytes().decodeToString())
        } catch (e: Exception) {
            throw CorruptionException("Cannot read JSON.", e)
        }
    }

    override suspend fun writeTo(
        t: Auth,
        output: OutputStream
    ) {
        output.write(Json.encodeToString(t).encodeToByteArray())
    }
}