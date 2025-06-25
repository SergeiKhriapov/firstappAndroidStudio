package ru.netology.nmedia.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.Api
import ru.netology.nmedia.dto.PushToken

class AppAuth private constructor(context: Context) {

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

    /** Сохранить токен **/
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

    fun sendPushTokenToServer(token: String? = null) {
        CoroutineScope(Dispatchers.Default).apply {
            launch {
                try {
                    Api.retrofitService.sendPushToken(
                        PushToken(
                            token ?: FirebaseMessaging.getInstance().token.await()
                        )
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Synchronized
    fun clearAuth() {
        _data.value = null
        prefs.edit { clear() }
       sendPushTokenToServer()
    }

    companion object {
        private const val TOKEN_KEY = "TOKEN_KEY"
        private const val ID_KEY = "ID_KEY"

        @Volatile
        private var INSTANCE: AppAuth? = null

        fun init(context: Context) {
            if (INSTANCE == null) {          // double‑checked locking
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = AppAuth(context.applicationContext)
                    }
                }
            }
        }

        /** Получить готовый объект */
        fun getInstance(): AppAuth =
            requireNotNull(INSTANCE) { "Need call init(context) before" }
    }
}