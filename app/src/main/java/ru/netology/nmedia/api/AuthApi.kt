package ru.netology.nmedia.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import ru.netology.nmedia.auth.Token

interface AuthApiService {
    @FormUrlEncoded
    @POST("users/authentication")
    suspend fun authenticate(
        @Field("login") login: String,
        @Field("pass") pass: String
    ): Token

    @FormUrlEncoded
    @POST("users/registration")
    suspend fun register(
        @Field("login") login: String,
        @Field("pass") pass: String,
        @Field("name") name: String
    ): Token

    @Multipart
    @POST("users/registration")
    suspend fun registerWithPhoto(
        @Part("login") login: RequestBody,
        @Part("pass") pass: RequestBody,
        @Part("name") name: RequestBody,
        @Part media: MultipartBody.Part
    ): Token
}

