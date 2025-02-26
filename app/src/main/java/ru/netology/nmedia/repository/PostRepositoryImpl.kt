package ru.netology.nmedia.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
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
    private val postsType = object : TypeToken<List<Post>>() {}.type

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
        private val jsonType = "application/json".toMediaType()
    }

    override fun getAll(): List<Post> {
        val request = Request.Builder()
            .url("$BASE_URL/api/slow/posts")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Ошибка сервера: ${response.code}")

            val body = response.body?.string() ?: throw IOException("Тело ответа пустое")
            return gson.fromJson(body, postsType)
        }
    }

    override fun getAllAsync(callback: PostRepository.GetAllCallback) {
        val request = Request.Builder()
            .url("$BASE_URL/api/slow/posts")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback.onError(IOException("Ошибка сервера: ${it.code}"))
                        return
                    }
                    val body = it.body?.string() ?: return callback.onError(IOException("Тело ответа пустое"))
                    try {
                        callback.onSuccess(gson.fromJson(body, postsType))
                    } catch (e: Exception) {
                        callback.onError(e)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }
        })
    }

    override fun save(post: Post): Post {
        val request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("$BASE_URL/api/slow/posts")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Ошибка сервера: ${response.code}")

            val body = response.body?.string() ?: throw IOException("Тело ответа пустое")
            return gson.fromJson(body, Post::class.java)
        }
    }

    override fun likeById(id: Long, callback: PostRepository.LikeCallback) {
        getAllAsync(object : PostRepository.GetAllCallback {
            override fun onSuccess(posts: List<Post>) {

                val post = posts.find { it.id == id } ?: return
                val likeOrDislike = if (post.likedByMe) "DELETE" else "POST"

                val request = Request.Builder()
                    .method(likeOrDislike, if (likeOrDislike == "POST") "".toRequestBody() else null)
                    .url("$BASE_URL/api/posts/$id/likes")
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string()
                        val updatedPost = gson.fromJson(body, Post::class.java)
                        callback.onSuccess(updatedPost)
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        callback.onError(e)
                    }
                })
            }

            override fun onError(e: Exception) {
                callback.onError(e)
            }
        })
    }

    override fun removeById(id: Long) {
        val request = Request.Builder()
            .delete()
            .url("$BASE_URL/api/slow/posts/$id")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
                throw IOException("Ошибка сервера при удалении: ${response.code}")
        }
    }

    override fun shareById(id: Long) {
        // TODO:
    }

    override fun viewById(id: Long) {
        // TODO:
    }
}
