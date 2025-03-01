package ru.netology.nmedia.repository

import okhttp3.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.dto.Post
import java.io.IOException
import java.util.concurrent.TimeUnit

class PostRepositoryImpl : PostRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
        private val jsonType = "application/json".toMediaType()
    }

    override fun getAll(): List<Post> {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        return client.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let {
                gson.fromJson(it, Array<Post>::class.java).toList()
            }
    }

    override fun save(post: Post): Post {
        val request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("$BASE_URL/api/slow/posts")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Ошибка сохранения: ${response.message}")
        }

        val responseBody = response.body?.string() ?: throw IOException("Пустой ответ")
        return gson.fromJson(responseBody, Post::class.java)
    }

    override fun likeById(post: Post): Post {
        val request: Request
        if (post.likedByMe) {
            request = Request.Builder()
                .delete()
                .url("$BASE_URL/api/posts/${post.id}/likes")
                .build()
        } else {
            request = Request.Builder()
                .post("".toRequestBody())
                .url("$BASE_URL/api/posts/${post.id}/likes")
                .build()
        }

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Ошибка обновления поста: ${response.message}")
        }

        val responseBody = response.body?.string() ?: throw IOException("Тело ответа пустое")
        return gson.fromJson(responseBody, Post::class.java)
    }

    override fun removeById(id: Long) {
        val request = Request.Builder()
            .delete()
            .url("$BASE_URL/api/slow/posts/$id")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Ошибка при удалении поста: ${response.message}")
        }
    }

    override fun shareById(id: Long) {
        // todo
    }

    override fun viewById(id: Long) {
        // todo
    }
}
