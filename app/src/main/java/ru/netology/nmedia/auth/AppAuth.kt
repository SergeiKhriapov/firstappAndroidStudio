package ru.netology.nmedia.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    }

    @Synchronized
    fun clearAuth() {
        _data.value = null
        prefs.edit { clear() }
    }

    companion object {
        private const val TOKEN_KEY = "TOKEN_KEY"
        private const val ID_KEY = "ID_KEY"

        @Volatile
        private var INSTANCE: AppAuth? = null

        /** Вызывает один раз при старте приложения */
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