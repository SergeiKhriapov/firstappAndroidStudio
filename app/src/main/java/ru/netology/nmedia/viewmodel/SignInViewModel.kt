package ru.netology.nmedia.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.AuthApiService
import ru.netology.nmedia.auth.AppAuth
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val authApiService: AuthApiService
) : ViewModel() {
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _authSuccess = MutableStateFlow(false)
    val registrationSuccess: StateFlow<Boolean> = _authSuccess

    fun login(login: String, pass: String) {
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            try {
                val token = authApiService.authenticate(login, pass)
                appAuth.setAuth(token.id, token.token)
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

    fun resetAuthSuccess() {
        _authSuccess.value = false
    }
}
