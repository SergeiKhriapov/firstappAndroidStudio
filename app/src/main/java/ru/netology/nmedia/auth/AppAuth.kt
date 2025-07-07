package ru.netology.nmedia.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.di.DependencyContainer

class AppAuth(private val context: Context) {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val _data = MutableStateFlow<Token?>(null)
    val data: StateFlow<Token?> = _data.asStateFlow()

    init {
        val token = prefs.getString(TOKEN_KEY, null)
        val id = prefs.getLong(ID_KEY, 0L)
        if (token != null && id != 0L) {
            _data.value = Token(id, token)
        }
        sendPushTokenToServer()
    }

    @Synchronized
    fun setAuth(id: Long, token: String) {
        Log.d("AppAuth", "setAuth called with id=$id, token=$token")
        _data.value = Token(id, token)
        prefs.edit {
            putString(TOKEN_KEY, token)
            putLong(ID_KEY, id)
        }
        sendPushTokenToServer()
    }

    fun clearAuth() {
        _data.value = null
        prefs.edit { clear() }
        sendPushTokenToServer()
    }

    fun sendPushTokenToServer(token: String? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // берём apiService и pushToken из контейнера
                val api = DependencyContainer.getInstance().apiService
                val push = token ?: FirebaseMessaging.getInstance().token.await()
                api.sendPushToken(PushToken(push))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val TOKEN_KEY = "TOKEN_KEY"
        private const val ID_KEY = "ID_KEY"
    }
}
