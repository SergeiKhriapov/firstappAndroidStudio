package ru.netology.nmedia.repository


import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    val data: LiveData<List<Post>>
    suspend fun getAll()
    suspend fun save(post: Post): Post
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long): Post
    suspend fun dislikeById(id: Long): Post
    fun shareById(id: Long)
    fun viewById(id: Long)

}
