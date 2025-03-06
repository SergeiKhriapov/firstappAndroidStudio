package ru.netology.nmedia.repository


import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.PostsApi
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
        PostsApi.retrofitService.getAll().enqueue(object : Callback<List<Post>> {

            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    callback.onSuccess(response.body() ?: throw RuntimeException("body is null"))
                } else {
                    callback.onError(Exception("Ошибка при получении постов: ${response.code()} ${response.message()} "))
                }
            }
            override fun onFailure(call: Call<List<Post>>, e: Throwable) {
                callback.onError(Exception(e))
            }
        })
    }

    override fun save(post: Post, callback: PostRepository.AsyncCallback<Post>) {
        PostsApi.retrofitService.save(post).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    callback.onSuccess(response.body() ?: throw RuntimeException("body is null"))
                } else {
                    callback.onError(Exception("Ошибка при сохранении поста: ${response.code()} ${response.message()}"))
                }
            }

            override fun onFailure(call: Call<Post>, e: Throwable) {
                callback.onError(Exception(e))
            }
        })
    }

    override fun likeById(post: Post, callback: PostRepository.AsyncCallback<Post>) {
        val updatedPost = post.copy(
            likedByMe = !post.likedByMe,
            likes = if (post.likedByMe) post.likes - 1 else post.likes + 1
        )
        callback.onSuccess(updatedPost)
        val call = if (post.likedByMe) {
            PostsApi.retrofitService.dislikeById(post.id)
        } else {
            PostsApi.retrofitService.likeById(post.id)
        }
        call.enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful) {
                    callback.onError(Exception("Ошибка: ${response.code()} ${response.message()}"))
                }
            }
            override fun onFailure(call: Call<Post>, e: Throwable) {
                callback.onError(Exception(e))
            }
        })
    }




    override fun removeById(id: Long, callback: PostRepository.AsyncCallback<Unit>) {
        PostsApi.retrofitService.removeById(id).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    callback.onSuccess(Unit)
                } else {
                    callback.onError(Exception("Ошибка при удалении поста: ${response.code()} ${response.message()}"))
                }
            }
            override fun onFailure(call: Call<Unit>, e: Throwable) {
                callback.onError(Exception(e))
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
