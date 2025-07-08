package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.Token
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appAuth: AppAuth,
    ) : ViewModel() {
    val authData: StateFlow<Token?> = appAuth.data

    fun logout() {
        appAuth.clearAuth()
    }

    val isAuthorized: Boolean
        get() = authData.value != null
}