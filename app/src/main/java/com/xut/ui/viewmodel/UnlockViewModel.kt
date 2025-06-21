package com.xut.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xut.Constants
import com.xut.domain.model.Auth
import com.xut.domain.model.User
import com.xut.domain.repository.UserRepository
import com.xut.unlock.UnlockException
import com.xut.unlock.UnlockRequest
import com.xut.unlock.UnlockUtilities
import com.xut.util.HttpUtils
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
    private val userRepository: UserRepository
) : ViewModel() {
    val userState: StateFlow<UserState> = combine(
        userRepository.user,
        userRepository.hasUser
    ) { user, hasUser ->
        if (hasUser) {
            UserState.HasUser(user)
        } else {
            UserState.NoUser
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserState.NoUser
    )

    private val _unlockState = MutableStateFlow(UnlockState())
    val unlockState: StateFlow<UnlockState> = _unlockState.asStateFlow()

    fun saveUser(user: User) {
        viewModelScope.launch {
            userRepository.save(user)
        }
    }

    fun clearUser() {
        viewModelScope.launch {
            userRepository.clear()
        }
    }

    fun startUnlock(user: User, host: String, product: String, token: String) {
        viewModelScope.launch {
            val pcId = DigestUtils.md5Hex(user.deviceId)
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
                        "userId" to user.userId,
                        "passToken" to user.passToken,
                        "deviceId" to user.deviceId
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

            val auth = Auth(
                userId = user.userId,
                pcId = pcId,
                ssecurity = ssecurity!!,
                serviceToken = serviceToken!!,
                slh = unlockApiSlh!!,
                ph = unlockApiPh!!
            )

            try {
                withContext(Dispatchers.IO) {
                    val info = UnlockRequest.Companion.userInfo(auth, host)
//                    Log.i(TAG, "Unlock request user info: $info")

                    val alert = UnlockRequest.Companion.deviceClear(auth, host, product)
//                    Log.i(TAG, "Unlock request device clear: $alert")
                }
            } catch (e: Exception) {
//                Log.w(TAG, "Pre-unlock requests failed: ${e.message}")
            }

            try {
                withContext(Dispatchers.IO) {
                    val json = UnlockRequest.Companion.ahaUnlock(auth, host, product, token)
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

@Suppress("UNCHECKED_CAST")
class UnlockViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UnlockViewModel::class.java)) {
            return UnlockViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class UserState {
    object NoUser : UserState()
    data class HasUser(val user: User) : UserState()
}

data class UnlockState(
    val isRunning: Boolean = false,
    val output: String? = null,
    val token: String? = null,
)