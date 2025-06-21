package com.xut.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer

import com.xut.domain.model.User

import kotlinx.serialization.json.Json

import java.io.InputStream
import java.io.OutputStream

object UserSerializer : Serializer<User> {
    override val defaultValue: User get() = User()

    override suspend fun readFrom(input: InputStream): User {
        return try {
            Json.decodeFromString(input.readBytes().decodeToString())
        } catch (e: Exception) {
            throw CorruptionException("Cannot read JSON.", e)
        }
    }

    override suspend fun writeTo(
        t: User, output: OutputStream
    ) {
        output.write(Json.encodeToString(t).encodeToByteArray())
    }
}