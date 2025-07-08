package ru.netology.nmedia.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.api.AuthApiService
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    private const val BASE_URL = "${BuildConfig.BASE_URL}/api/"

    @Provides
    @Singleton
    fun provideLogging(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        if (BuildConfig.DEBUG) {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /*@Provides
    @Singleton
    fun provideAppAuth(@ApplicationContext context: Context): AppAuth =
        AppAuth(context)*/

    @Provides
    @Singleton
    fun provideOkHttpClient(
        logging: HttpLoggingInterceptor,
        /*appAuth: AppAuth,*/
        appAuth: Provider<AppAuth>, // Lazy access

    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            appAuth.get().data.value?.token?.let {
                requestBuilder.addHeader("Authorization", it)
            }
            chain.proceed(requestBuilder.build())
        }
        .build()


    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun providePostsApiService(retrofit: Retrofit): PostsApiService =
        retrofit.create(PostsApiService::class.java)
}
