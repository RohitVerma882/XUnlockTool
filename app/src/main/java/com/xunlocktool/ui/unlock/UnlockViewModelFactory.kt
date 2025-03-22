package com.xunlocktool.ui.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager

import com.xunlocktool.data.auth.AuthDataStore

@Suppress("UNCHECKED_CAST")
class UnlockViewModelFactory(
    private val authDataStore: AuthDataStore,
    private val workManager: WorkManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UnlockViewModel::class.java)) {
            return UnlockViewModel(authDataStore, workManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}