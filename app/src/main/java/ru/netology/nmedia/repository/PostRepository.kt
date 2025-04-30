package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post

interface PostRepository {
    val data: Flow<List<Post>>
    fun getNewer(id: Long): Flow<Int>
    fun getHiddenSyncedCount(): Flow<Int>
    suspend fun getAll()
    suspend fun save(post: Post): Post
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long): Post
    suspend fun dislikeById(id: Long): Post
    fun shareById(id: Long)
    fun viewById(id: Long)
    suspend fun update(post: Post)
    suspend fun unhideAllSyncedPosts()


}
