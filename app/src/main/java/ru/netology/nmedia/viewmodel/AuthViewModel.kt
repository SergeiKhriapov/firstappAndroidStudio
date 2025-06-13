package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.Token

class AuthViewModel : ViewModel() {
    val authData: StateFlow<Token?> = AppAuth.getInstance().data
    val isAuthorized: Boolean
        get() = authData.value != null

}