package ru.netology.nmedia.repository.post

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post
import java.io.File

interface PostRepository {

    val data: Flow<PagingData<Post>>

    fun getNewer(id: Long): Flow<Int>

    fun getHiddenSyncedCount(): Flow<Int>

    suspend fun getAll()

    suspend fun save(post: Post): Post

    suspend fun save(post: Post, file: File): Post

    suspend fun removeById(id: Long)

    suspend fun likeById(id: Long): Post

    suspend fun dislikeById(id: Long): Post

    fun shareById(id: Long)

    fun viewById(id: Long)

    suspend fun update(post: Post)

    suspend fun unhideAllSyncedPosts()


}