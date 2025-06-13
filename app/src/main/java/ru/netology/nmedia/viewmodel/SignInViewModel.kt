package ru.netology.nmedia.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.AuthApi
import ru.netology.nmedia.auth.AppAuth
import java.io.IOException

class SignInViewModel : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _authSuccess = MutableLiveData(false)
    val authSuccess: LiveData<Boolean> = _authSuccess

    fun login(login: String, pass: String) {
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            try {
                val token = AuthApi.service.authenticate(login, pass)
                AppAuth.getInstance().setAuth(token.id, token.token)
                _authSuccess.value = true
            } catch (e: IOException) {
                _errorMessage.value = "Проблема с сетью: ${e.localizedMessage}"
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка авторизации: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }
}

