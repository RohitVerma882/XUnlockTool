package com.xunlocktool.ui.unlock

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xunlocktool.Constants
import com.xunlocktool.data.auth.Auth
import com.xunlocktool.data.auth.AuthDataStore
import com.xunlocktool.data.unlock.Auth2
import com.xunlocktool.ui.login.LoginState
import com.xunlocktool.unlock.UnlockException
import com.xunlocktool.unlock.UnlockRequest
import com.xunlocktool.unlock.UnlockUtilities
import com.xunlocktool.utils.HttpUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.codec.digest.DigestUtils
import org.json.JSONException
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
class UnlockViewModel(
    private val authDataStore: AuthDataStore
) : ViewModel() {
    val loginState: StateFlow<LoginState> = combine(
        authDataStore.auth, authDataStore.isLoggedIn
    ) { auth, isLoggedIn ->
        LoginState(auth, isLoggedIn)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LoginState()
    )

    private val _unlockState = MutableStateFlow(UnlockState())
    val unlockState: StateFlow<UnlockState> = _unlockState.asStateFlow()

    fun saveAuth(userId: String, passToken: String, deviceId: String) {
        viewModelScope.launch {
            authDataStore.saveAuth(userId, passToken, deviceId)
        }
    }

    fun clearAuth() {
        viewModelScope.launch {
            authDataStore.clearAuth()
        }
    }

    fun startUnlock(auth: Auth, host: String, product: String, token: String) {
        viewModelScope.launch {
            val pcId = DigestUtils.md5Hex(auth.deviceId)
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
                        "userId" to auth.userId,
                        "passToken" to auth.passToken,
                        "deviceId" to auth.deviceId
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

            }

            val auth = Auth2(
                userId = auth.userId,
                pcId = pcId,
                ssecurity = ssecurity!!,
                serviceToken = serviceToken!!,
                unlockApiSlh = unlockApiSlh!!,
                unlockApiPh = unlockApiPh!!
            )

            try {
                withContext(Dispatchers.IO) {
                    val info = UnlockRequest.userInfo(auth, host)
//                    Log.i(TAG, "Unlock request user info: $info")

                    val alert = UnlockRequest.deviceClear(auth, host, product)
//                    Log.i(TAG, "Unlock request device clear: $alert")
                }
            } catch (e: Exception) {
//                Log.w(TAG, "Pre-unlock requests failed: ${e.message}")
            }

            try {
                withContext(Dispatchers.IO) {
                    val json = UnlockRequest.ahaUnlock(auth, host, product, token)
                    val jsonObject = JSONObject(json)
                    val code = jsonObject.getInt("code")
                    val description = jsonObject.optString("descEN", "Unknown")
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

            }

            delay(100)
        }
    }

    fun stopUnlock() {

    }
}