package ru.netology.nmedia.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.repository.AuthRepository

class SignUpViewModel : ViewModel() {
    private val repository = AuthRepository()
    private val appAuth = AppAuth.getInstance()

    private val _registrationSuccess = MutableStateFlow(false)
    val registrationSuccess: StateFlow<Boolean> = _registrationSuccess.asStateFlow()

    fun register(name: String, login: String, password: String) {
        viewModelScope.launch {
            try {
                val token = repository.registerUser(name, login, password)
                appAuth.setAuth(token.id, token.token)
                _registrationSuccess.value = true
            } catch (e: Exception) {
                Log.e("SignUp", "Ошибка регистрации", e)
            }
        }
    }
    fun resetRegistrationSuccess() {
        _registrationSuccess.value = false
    }
}

