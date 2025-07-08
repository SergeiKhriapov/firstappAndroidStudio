package ru.netology.nmedia.repository.post

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post

interface LocalPostRepository {

    val data: Flow<List<Post>>

    suspend fun getAll()

    suspend fun save(post: Post): Long

    suspend fun syncUnsyncedPosts(): Boolean

    suspend fun update(post: Post)

    suspend fun removeById(id: Long)

    suspend fun likeById(id: Long): Post

    suspend fun dislikeById(id: Long): Post

    fun shareById(id: Long)

    fun viewById(id: Long)
}