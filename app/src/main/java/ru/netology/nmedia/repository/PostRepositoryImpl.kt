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

    override fun getAllAsync(callback: PostRepository.AsyncCallback<List<Post>>) {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val postsString =
                        response.body?.string() ?: throw RuntimeException("body is null")
                    try {
                        // Было callback.onSuccess(gson.fromJson(body, jsonType.type))
                        val posts = gson.fromJson(postsString, Array<Post>::class.java).toList()
                        callback.onSuccess(posts)
                    } catch (e: Exception) {
                        callback.onError(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }
            })
    }

    override fun save(post: Post, callback: PostRepository.AsyncCallback<Post>) {
        val request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("$BASE_URL/api/slow/posts")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val postsString =
                        response.body?.string() ?: throw RuntimeException("body is null")

                    val savedPost = gson.fromJson(postsString, Post::class.java)
                    callback.onSuccess(savedPost)
                } else {
                    callback.onError(Exception("Ошибка при сохранении поста"))
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }
        })
    }

    override fun likeById(post: Post, callback: PostRepository.AsyncCallback<Post>) {
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

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback.onError(IOException("Ошибка обновления поста: ${response.message}"))
                    return
                }

                val responseBody = response.body?.string() ?: run {
                    callback.onError(IOException("budy is null"))
                    return
                }

                val postResponse = gson.fromJson(responseBody, Post::class.java)
                callback.onSuccess(postResponse)
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }
        })
    }


    override fun removeById(id: Long, callback: PostRepository.AsyncCallback<Unit>) {
        val request = Request.Builder()
            .delete()
            .url("$BASE_URL/api/slow/posts/$id")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback.onError(IOException("Ошибка при удалении поста: ${response.message}"))
                    return
                }
                callback.onSuccess(Unit)
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }
        })
    }


    override fun shareById(id: Long) {
        // todo
    }

    override fun viewById(id: Long) {
        // todo
    }
}
