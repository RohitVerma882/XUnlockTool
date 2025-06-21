package com.xunlocktool.ui.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.xunlocktool.data.auth.AuthDataStore

@Suppress("UNCHECKED_CAST")
class UnlockViewModelFactory(
    private val authDataStore: AuthDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UnlockViewModel::class.java)) {
            return UnlockViewModel(authDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}