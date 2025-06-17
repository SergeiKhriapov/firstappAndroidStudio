package ru.netology.nmedia.repository

import ru.netology.nmedia.api.AuthApi
import ru.netology.nmedia.auth.Token

class AuthRepository {
    suspend fun registerUser(name: String, login: String, password: String): Token {
        return AuthApi.service.register(login, password, name)
    }
}