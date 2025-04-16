package ru.netology.nmedia.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nmedia.dao.LocalPostEntityDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entities.LocalPostEntity
import ru.netology.nmedia.entities.toPostDto

class LocalPostRepositoryImpl(private val dao: LocalPostEntityDao) : PostRepository {

    override val data: LiveData<List<Post>>
        get() = dao.getAll().map { it.toPostDto() }

    override suspend fun getAll() {
        TODO("Not yet implemented")
    }


    // Сохранение черновика
    override suspend fun save(post: Post): Post {
        val entity = LocalPostEntity.fromDto(post)
        Log.d("LocalRepository", "Сохраняем черновик: ${entity.content}")
        dao.save(entity)  // сохраняем в Room
        return post
    }

    override suspend fun removeById(id: Long) {
        dao.removeByIdLocal(id)
    }


    // Лайк и дизлайк не поддерживаются
    override suspend fun likeById(id: Long): Post {
        throw UnsupportedOperationException("Likes are not supported for drafts")
    }

    override suspend fun dislikeById(id: Long): Post {
        throw UnsupportedOperationException("Likes are not supported for drafts")
    }

    override fun shareById(id: Long) {
        //
    }

    override fun viewById(id: Long) {
        //
    }
}
