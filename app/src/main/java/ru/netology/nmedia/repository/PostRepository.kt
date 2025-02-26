package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    /*fun getAll(): LiveData<List<Post>>*/
    fun getAll(): List<Post>
    fun likeById(id: Long, callback: PostRepository.LikeCallback)
    fun shareById(id: Long)
    fun viewById(id: Long)
    fun removeById(id: Long)
    fun save(post: Post): Post
    fun getAllAsync(callback: GetAllCallback)
    interface GetAllCallback {
        fun onSuccess(posts: List<Post>) {}
        fun onError(e: Exception) {}
    }

    interface LikeCallback {
        fun onSuccess(updatedPost: Post)
        fun onError(e: Exception)
    }
}