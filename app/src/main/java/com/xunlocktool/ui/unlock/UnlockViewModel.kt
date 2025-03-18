package com.xunlocktool.ui.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf

import com.xunlocktool.data.auth.Auth
import com.xunlocktool.data.auth.AuthDataSource
import com.xunlocktool.ui.login.LoginState
import com.xunlocktool.works.unlock.UnlockWorker

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class UnlockViewModel(
    private val authDataSource: AuthDataSource,
    private val workManager: WorkManager
) : ViewModel() {
    val loginState: StateFlow<LoginState> = combine(
        authDataSource.auth, authDataSource.isLoggedIn
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
            authDataSource.saveAuth(userId, passToken, deviceId)
        }
    }

    fun clearAuth() {
        viewModelScope.launch {
            authDataSource.clearAuth()
        }
    }

    fun startUnlock(auth: Auth, host: String, product: String, token: String) {
        viewModelScope.launch {
            val data = workDataOf(
                UnlockWorker.KEY_USER_ID to auth.userId,
                UnlockWorker.KEY_PASS_TOKEN to auth.passToken,
                UnlockWorker.KEY_DEVICE_ID to auth.deviceId,
                UnlockWorker.KEY_HOST to host,
                UnlockWorker.KEY_PRODUCT to product,
                UnlockWorker.KEY_TOKEN to token
            )

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val request = OneTimeWorkRequestBuilder<UnlockWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .addTag(UnlockWorker.TAG)
                .build()
            workManager.enqueueUniqueWork(UnlockWorker.TAG, ExistingWorkPolicy.REPLACE, request)

            workManager.getWorkInfoByIdFlow(request.id).collect { workInfo ->
                if (workInfo == null) return@collect
                val outputData = workInfo.outputData
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        _unlockState.update { currentState ->
                            currentState.copy(isRunning = true, output = null, token = null)
                        }
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        val output = outputData.getString(UnlockWorker.KEY_RESULT)
                        val token = outputData.getString(UnlockWorker.KEY_UNLOCK_TOKEN)
                        _unlockState.update { currentState ->
                            currentState.copy(isRunning = false, output = output, token = token)
                        }
                    }

                    WorkInfo.State.FAILED -> {
                        val error = outputData.getString(UnlockWorker.KEY_RESULT)
                        _unlockState.update { currentState ->
                            currentState.copy(isRunning = false, output = error, token = null)
                        }
                    }

                    else -> {
                        _unlockState.update { currentState ->
                            currentState.copy(isRunning = false, output = null, token = null)
                        }
                    }
                }
            }
        }
    }

    fun stopUnlock() {
        viewModelScope.launch {
            workManager.cancelUniqueWork(UnlockWorker.TAG)
        }
    }
}