package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.Token

class AuthViewModel(
    private val appAuth: AppAuth,
    ) : ViewModel() {
    val authData: StateFlow<Token?> = appAuth.data

    fun logout() {
        appAuth.clearAuth()
    }

    val isAuthorized: Boolean
        get() = authData.value != null
}