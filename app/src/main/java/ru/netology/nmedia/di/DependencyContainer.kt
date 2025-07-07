package ru.netology.nmedia.di

import android.content.Context
import androidx.room.Room
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.repository.LocalPostRepositoryImpl
import ru.netology.nmedia.repository.MediaRepository
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl

class DependencyContainer private constructor(
    context: Context
) {
    companion object {
        private const val BASE_URL = "${BuildConfig.BASE_URL}/api/slow/"

        @Volatile
        private var instance: DependencyContainer? = null

        fun initApp(context: Context) {
            instance = DependencyContainer(context.applicationContext)
        }

        fun getInstance(): DependencyContainer =
            instance ?: throw IllegalStateException("Call initApp() first")
    }

    val appAuth: AppAuth = AppAuth(context)

    private val logging = HttpLoggingInterceptor().apply {
        if (BuildConfig.DEBUG) level = HttpLoggingInterceptor.Level.BODY
    }

    private val okhttp = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val token = appAuth.data.value?.token
            val req = if (token != null) {
                chain.request().newBuilder().addHeader("Authorization", token).build()
            } else chain.request()
            chain.proceed(req)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okhttp)
        .build()

    val apiService: PostsApiService = retrofit.create(PostsApiService::class.java)

    private val db = Room.databaseBuilder(context, AppDb::class.java, "app.db").build()
    private val postDao = db.postDao
    private val localPostDao = db.localPostDao // <-- добавлено

    private val mediaRepository = MediaRepository(apiService)

    val repository: PostRepository =
        PostRepositoryImpl(postDao, mediaRepository, apiService)

    val localRepository = LocalPostRepositoryImpl(localPostDao, mediaRepository, apiService) // <-- добавлено
}
