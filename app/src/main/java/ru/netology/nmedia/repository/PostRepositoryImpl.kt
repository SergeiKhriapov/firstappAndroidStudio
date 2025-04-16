package ru.netology.nmedia.repository


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entities.PostEntity
import ru.netology.nmedia.entities.toPostEntity
import ru.netology.nmedia.entities.toPostDto

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override val data: LiveData<List<Post>> = dao.getAll().map { it.toPostDto() }

    override suspend fun getAll() {
        val posts = PostsApi.retrofitService.getAll()
        Log.d("PostRepositoryImpl", "Загружено с сервера: ${posts.size} постов")
        dao.insert(posts.toPostEntity())
    }

    override suspend fun save(post: Post): Post {
        return try {
            val saved = PostsApi.retrofitService.save(post)
            dao.insert(PostEntity.fromDto(saved.copy(isSynced = true))) // Помечаем синхронизированные посты
            saved
        } catch (e: Exception) {
            //
            throw e
        }
    }


    override suspend fun likeById(id: Long): Post {
        val post = dao.getById(id)?.toDto() ?: throw Exception("Post not found")
        val updated = post.copy(likedByMe = true, likes = post.likes + 1)
        dao.insert(PostEntity.fromDto(updated))      // Обновляю Room

        try {
            val response = PostsApi.retrofitService.likeById(id)
            dao.insert(PostEntity.fromDto(response)) // Синхронизация лайков в Room из сети.
            return response
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun dislikeById(id: Long): Post {
        val post = dao.getById(id)?.toDto() ?: throw Exception("Post not found")
        val updated = post.copy(likedByMe = false, likes = -1)
        dao.insert(PostEntity.fromDto(updated))
        try {
            val response = PostsApi.retrofitService.dislikeById(id)
            dao.insert(PostEntity.fromDto(response))
            return response
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun removeById(id: Long) {
        val post = dao.getById(id)
        post?.let { dao.removeById(id) }
        try {
            PostsApi.retrofitService.removeById(id)
        } catch (e: Exception) {
            post?.let { dao.insert(it) }
            throw e
        }
    }


    override fun shareById(id: Long) {
        // todo
    }

    override fun viewById(id: Long) {
        // todo
    }
}
