package ru.netology.nmedia.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.auth.Token

private const val BASE_URL = "${BuildConfig.BASE_URL}/api/"

private val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    )
    .addConverterFactory(GsonConverterFactory.create())
    .build()

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

object AuthApi {
    val service: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}

