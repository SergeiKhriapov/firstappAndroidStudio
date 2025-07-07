package ru.netology.nmedia.viewmodel

import AuthRepository
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.di.DependencyContainer
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.util.FileUtils
import java.io.File

class SignUpViewModel(
    private val appAuth: AppAuth,
) : ViewModel() {
    private val repository = AuthRepository()
    private val _registrationSuccess = MutableStateFlow(false)
    val registrationSuccess: StateFlow<Boolean> = _registrationSuccess.asStateFlow()

    private val _photo = MutableLiveData<PhotoModel?>(null)
    val photo: LiveData<PhotoModel?> = _photo

    fun changePhoto(uri: Uri, file: File) {
        _photo.value = PhotoModel(uri, file)
    }

    fun removePhoto() {
        _photo.value = null
    }

    fun register(name: String, login: String, password: String) {
        viewModelScope.launch {
            try {
                val token = repository.registerUser(name, login, password)
                appAuth.setAuth(token.id, token.token)
                _registrationSuccess.value = true
            } catch (e: Exception) {
                Log.e("SignUpViewModel", "Ошибка регистрации", e)
            }
        }
    }

    fun registerWithPhoto(
        name: String,
        login: String,
        password: String,
        fileUri: Uri,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val file = FileUtils.uriToFile(context, fileUri)
                val token = repository.registerUserWithPhoto(name, login, password, file)
                appAuth.setAuth(token.id, token.token)
                _registrationSuccess.value = true
            } catch (e: Exception) {
                Log.e("SignUpViewModel", "Ошибка регистрации с фото", e)
            }
        }
    }

    fun resetRegistrationSuccess() {
        _registrationSuccess.value = false
    }
}
