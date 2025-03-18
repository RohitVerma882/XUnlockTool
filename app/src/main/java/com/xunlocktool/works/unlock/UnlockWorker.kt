package com.xunlocktool.works.unlock

import android.content.Context
import android.util.Log

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

import com.xunlocktool.Constants
import com.xunlocktool.data.unlock.Auth2
import com.xunlocktool.unlock.UnlockException
import com.xunlocktool.unlock.UnlockRequest
import com.xunlocktool.unlock.UnlockUtilities
import com.xunlocktool.utils.HttpUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

import okhttp3.OkHttpClient
import okhttp3.Request

import org.apache.commons.codec.digest.DigestUtils

import org.json.JSONException
import org.json.JSONObject

class UnlockWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val host = inputData.getString(KEY_HOST)!!
        val product = inputData.getString(KEY_PRODUCT)!!
        val token = inputData.getString(KEY_TOKEN)!!
        val userId = inputData.getString(KEY_USER_ID)!!
        val passToken = inputData.getString(KEY_PASS_TOKEN)!!
        val deviceId = inputData.getString(KEY_DEVICE_ID)!!

        val pcId = DigestUtils.md5Hex(deviceId)
        var ssecurity: String? = null
        var nonce: String? = null
        var location: String? = null
        var serviceToken: String? = null
        var unlockApiSlh: String? = null
        var unlockApiPh: String? = null
        var unlockToken: String? = null

        try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                    .newBuilder()
                    .followRedirects(false)
                    .build()

                val cookiesMap = linkedMapOf(
                    KEY_USER_ID to userId,
                    KEY_PASS_TOKEN to passToken,
                    KEY_DEVICE_ID to deviceId
                )

                var request = Request.Builder()
                    .url(Constants.LOGIN_URL2)
                    .header("Cookie", HttpUtils.map2CookieString(cookiesMap))
                    .build()

                client.newCall(request).execute().use { response ->
                    val json = UnlockUtilities.findJsonStart(response.body?.string())
                    if (json == null) throw UnlockException("Unable to find ssecurity json")
                    try {
                        val jsonObject = JSONObject(json)
                        ssecurity = jsonObject.getString("ssecurity")
                        nonce = jsonObject.getString("nonce")
                        location = jsonObject.getString("location")
                    } catch (e: JSONException) {
                        throw UnlockException("Unable to parse ssecurity json: ${e.message}")
                    }
                }

                val signedLocation = UnlockUtilities.signLocation(location, ssecurity, nonce)
                if (signedLocation == null) {
                    throw UnlockException("Cannot sign location, maybe missing parameters or failed hash")
                }

                request = Request.Builder()
                    .url(signedLocation)
                    .build()

                client.newCall(request).execute().use { response ->
                    val cookiesList = response.headers.values("set-cookie")
                    cookiesList.forEach { cookies ->
                        cookies.split(";").forEach { cookie2 ->
                            val parts2 = cookie2.split("=", limit = 2)
                            if (parts2.size < 2) return@forEach
                            val name = parts2[0].trim()
                            val value = parts2[1].trim()

                            if ("serviceToken" == name) {
                                serviceToken = value
                            } else if ("unlockApi_slh" == name) {
                                unlockApiSlh = value
                            } else if ("unlockApi_ph" == name) {
                                unlockApiPh = value
                            }
                        }
                    }
                }

                if (serviceToken == null) {
                    throw UnlockException("Missing serviceToken cookie")
                }
            }
        } catch (e: Exception) {
            delay(100)
            return Result.failure(workDataOf(KEY_RESULT to e.message))
        }

        val auth = Auth2(
            userId = userId,
            pcId = pcId,
            ssecurity = ssecurity!!,
            serviceToken = serviceToken!!,
            unlockApiSlh = unlockApiSlh!!,
            unlockApiPh = unlockApiPh!!
        )

        try {
            withContext(Dispatchers.IO) {
                val info = UnlockRequest.userInfo(auth, host)
                Log.i(TAG, "Unlock request user info: $info")

                val alert = UnlockRequest.deviceClear(auth, host, product)
                Log.i(TAG, "Unlock request device clear: $alert")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pre-unlock requests failed: ${e.message}")
        }

        try {
            withContext(Dispatchers.IO) {
                val json = UnlockRequest.ahaUnlock(auth, host, product, token)
                val jsonObject = JSONObject(json)
                val code = jsonObject.getInt("code")
                val description = jsonObject.optString("descEN", "Empty")
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                unlockToken = jsonObject.optString("encryptData", null)

                if (code != 0 || unlockToken.isEmpty()) {
                    val description2 = UnlockUtilities.getMeaning(code, jsonObject)
                    val messageBuilder = StringBuilder().apply {
                        append("Xiaomi server returned error: ")
                        append(code)
                        append('\n')
                        append("Tool description: ")
                        append(description2)
                        append('\n')
                        append("Server description: ")
                        append(description)
                    }
                    throw UnlockException(messageBuilder.toString())
                }
            }
        } catch (e: Exception) {
            delay(100)
            return Result.failure(workDataOf(KEY_RESULT to e.message))
        }

        delay(100)
        return Result.success(
            workDataOf(
                KEY_RESULT to "Successfully acquired unlock token",
                KEY_UNLOCK_TOKEN to unlockToken
            )
        )
    }

    companion object {
        @JvmStatic
        val TAG = UnlockWorker::class.java.simpleName as String
        const val KEY_USER_ID = "userId"
        const val KEY_PASS_TOKEN = "passToken"
        const val KEY_DEVICE_ID = "deviceId"
        const val KEY_HOST = "host"
        const val KEY_PRODUCT = "product"
        const val KEY_TOKEN = "token"
        const val KEY_RESULT = "result"
        const val KEY_UNLOCK_TOKEN = "unlock_token"
    }
}