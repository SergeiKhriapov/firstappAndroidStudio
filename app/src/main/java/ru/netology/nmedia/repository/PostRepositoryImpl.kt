package ru.netology.nmedia.repository


import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entities.PostEntity
import ru.netology.nmedia.entities.toPostEntity
import ru.netology.nmedia.entities.toLocalPostDto
import ru.netology.nmedia.error.ApiError

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {

    override val data = dao.getAll()
        .map(List<PostEntity>::toLocalPostDto)
        .flowOn(Dispatchers.Default)

    /* override suspend fun getAll() {
         try {
             val response = PostsApi.retrofitService.getAll()
             if (!response.isSuccessful) throw RuntimeException("Ошибка загрузки: ${response.code()}")

             val posts = response.body() ?: throw RuntimeException("Пустой ответ")
             Log.d("PostRepositoryImpl", "Загружено с сервера: ${posts.size} постов")
             dao.insert(posts.toPostEntity(true))

         } catch (e: Exception) {
             Log.e("PostRepositoryImpl", "Ошибка загрузки", e)
             throw e
         }
     }*/
    override suspend fun getAll() {
        try {
            val response = PostsApi.retrofitService.getAll()
            if (!response.isSuccessful) throw RuntimeException("Ошибка загрузки: ${response.code()}")

            val postsFromServer = response.body() ?: throw RuntimeException("Пустой ответ")
            Log.d("PostRepositoryImpl", "Загружено с сервера: ${postsFromServer.size} постов")

            // Получаем все текущие посты из БД чтобы сохранить их hidden-статусы
            val existingPosts = dao.getAllNow().associateBy { it.id }

            // Преобразуем каждый полученный пост, сохраняя hidden у уже существующих
            val updatedEntities = postsFromServer.map { post ->
                val existing = existingPosts[post.id]
                PostEntity.fromDto(post.copy(isSynced = true)).copy(
                    hidden = existing?.hidden ?: true
                )
            }

            dao.insert(updatedEntities)

        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Ошибка загрузки", e)
            throw e
        }
    }

    override suspend fun save(post: Post): Post {
        return try {
            val response = PostsApi.retrofitService.save(post)
            if (!response.isSuccessful) throw RuntimeException("Ошибка сохранения: ${response.code()}")
            val saved = response.body() ?: throw RuntimeException("Пустой ответ при сохранении")
            dao.insert(PostEntity.fromDto(saved.copy(isSynced = true)))
            saved
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Ошибка сохранения", e)
            throw e
        }
    }

    override suspend fun likeById(id: Long): Post {
        val post = dao.getById(id)?.toDto() ?: throw Exception("Post not found")
        val updated = post.copy(
            likedByMe = true,
            likes = if (post.likes == 0) 1 else post.likes + 1
        )
        dao.insert(PostEntity.fromDto(updated))
        try {
            val response = PostsApi.retrofitService.likeById(id)
            if (!response.isSuccessful) throw RuntimeException("Ошибка лайка: ${response.code()}")
            val likedPost = response.body() ?: throw RuntimeException("Пустой ответ при лайке")
            dao.insert(PostEntity.fromDto(likedPost))
            return likedPost
        } catch (e: Exception) {
            dao.insert(PostEntity.fromDto(post)) // откат
            throw e
        }
    }

    override suspend fun dislikeById(id: Long): Post {
        val post = dao.getById(id)?.toDto() ?: throw Exception("Post not found")
        val updated = post.copy(
            likedByMe = false,
            likes = if (post.likes == 1) 0 else post.likes - 1
        )
        dao.insert(PostEntity.fromDto(updated))
        try {
            val response = PostsApi.retrofitService.dislikeById(id)
            if (!response.isSuccessful) throw RuntimeException("Ошибка дизлайка: ${response.code()}")
            val dislikedPost =
                response.body() ?: throw RuntimeException("Пустой ответ при дизлайке")
            dao.insert(PostEntity.fromDto(dislikedPost))
            return dislikedPost
        } catch (e: Exception) {
            dao.insert(PostEntity.fromDto(post)) // откат
            throw e
        }
    }

    override suspend fun removeById(id: Long) {
        val post = dao.getById(id)
        post?.let { dao.removeById(id) }
        try {
            val response = PostsApi.retrofitService.removeById(id)
            if (!response.isSuccessful) throw RuntimeException("Ошибка удаления: ${response.code()}")
        } catch (e: Exception) {
            post?.let { dao.insert(it) }
            throw e
        }
    }

    override fun getNewer(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000)
            val response = PostsApi.retrofitService.getNewer(id)
            if (!response.isSuccessful)
                throw ApiError(response.code(), response.message())
            val posts = response.body() ?: throw ApiError(response.code(), response.message())
            val hiddenPosts = posts.map { it.copy(hidden = true) }
            dao.insert(hiddenPosts.toPostEntity(hidden = true))
            Log.d("PostRepositoryImpl", "hiddenPosts.size:$hiddenPosts.size ")
            emit(hiddenPosts.size)
        }
    }.catch { e ->
        Log.e("PostRepositoryImpl", "Ошибка при загрузке новых постов", e)
        emit(0)
    }

    override suspend fun update(post: Post) {
        dao.update(post.id, post.content, post.published, post.isSynced)
    }

    override fun getHiddenSyncedCount(): Flow<Int> =
        dao.getHiddenSyncedCount()

    override suspend fun unhideAllSyncedPosts() {
        dao.unhideAllSyncedPosts()
    }

    override fun shareById(id: Long) {
        // todo
    }

    override fun viewById(id: Long) {
        // todo
    }
}