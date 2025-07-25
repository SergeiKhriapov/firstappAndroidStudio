package ru.netology.nmedia.repository.post

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entities.PostEntity
import ru.netology.nmedia.entities.toLocalPostDto
import ru.netology.nmedia.entities.toPostEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.repository.media.MediaRepository
import java.io.File
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val mediaRepository: MediaRepository,
    private val apiService: PostsApiService
) : PostRepository {

    override val data: Flow<List<Post>> = dao.getAll()
        .map(List<PostEntity>::toLocalPostDto)
        .flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        try {
            Log.d("PostRepositoryImpl", "Начало загрузки постов с сервера")
            val response = apiService.getAll()
            if (!response.isSuccessful) throw RuntimeException("Ошибка загрузки: ${response.code()}")
            val postsFromServer = response.body() ?: throw RuntimeException("Пустой ответ от сервера")

            Log.d("PostRepositoryImpl", "Загружено ${postsFromServer.size} постов")

            val existingPosts = dao.getAllNow().associateBy { it.id }

            val updatedEntities = postsFromServer.map { post ->
                val existing = existingPosts[post.id]
                PostEntity.Companion.fromDto(post.copy(isSynced = true)).copy(hidden = existing?.hidden ?: true)
            }

            dao.insert(updatedEntities)
            Log.d("PostRepositoryImpl", "Посты успешно сохранены в БД")

        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Ошибка при получении постов", e)
            throw e
        }
    }

    override suspend fun save(post: Post): Post {
        Log.d("PostRepositoryImpl", "Сохранение поста: $post")
        return try {
            val response = apiService.save(post)
            if (!response.isSuccessful) throw RuntimeException("Ошибка сохранения: ${response.code()}")
            val saved = response.body() ?: throw RuntimeException("Пустой ответ при сохранении")
            dao.insert(PostEntity.Companion.fromDto(saved.copy(isSynced = true)))
            Log.d("PostRepositoryImpl", "Пост успешно сохранён МЕТОД БЕЗ ФАЙЛА: $saved")
            saved
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Ошибка сохранения поста", e)
            throw e
        }
    }

    override suspend fun likeById(id: Long): Post {
        Log.d("PostRepositoryImpl", "Лайк поста с id: $id")
        val post = dao.getById(id)?.toDto() ?: throw Exception("Post not found")
        val updated = post.copy(likedByMe = true, likes = if (post.likes == 0) 1 else post.likes + 1)
        dao.insert(PostEntity.Companion.fromDto(updated))

        return try {
            val response = apiService.likeById(id)
            if (!response.isSuccessful) throw RuntimeException("Ошибка лайка: ${response.code()}")
            val likedPost = response.body() ?: throw RuntimeException("Пустой ответ при лайке")
            dao.insert(PostEntity.Companion.fromDto(likedPost))
            Log.d("PostRepositoryImpl", "Пост успешно лайкнут: $likedPost")
            likedPost
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Ошибка лайка, откат", e)
            dao.insert(PostEntity.Companion.fromDto(post)) // rollback
            throw e
        }
    }

    override suspend fun dislikeById(id: Long): Post {
        Log.d("PostRepositoryImpl", "Дизлайк поста с id: $id")
        val post = dao.getById(id)?.toDto() ?: throw Exception("Post not found")
        val updated = post.copy(likedByMe = false, likes = if (post.likes == 1) 0 else post.likes - 1)
        dao.insert(PostEntity.Companion.fromDto(updated))

        return try {
            val response = apiService.dislikeById(id)
            if (!response.isSuccessful) throw RuntimeException("Ошибка дизлайка: ${response.code()}")
            val dislikedPost = response.body() ?: throw RuntimeException("Пустой ответ при дизлайке")
            dao.insert(PostEntity.Companion.fromDto(dislikedPost))
            Log.d("PostRepositoryImpl", "Пост успешно дизлайкнут: $dislikedPost")
            dislikedPost
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Ошибка дизлайка, откат", e)
            dao.insert(PostEntity.Companion.fromDto(post)) // rollback
            throw e
        }
    }

    override suspend fun removeById(id: Long) {
        Log.d("PostRepositoryImpl", "Удаление поста с id: $id")
        val post = dao.getById(id)
        post?.let { dao.removeById(id) }

        try {
            val response = apiService.removeById(id)
            if (!response.isSuccessful) throw RuntimeException("Ошибка удаления: ${response.code()}")
            Log.d("PostRepositoryImpl", "Пост успешно удалён с сервера")
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Ошибка удаления, откат", e)
            post?.let { dao.insert(it) }
            throw e
        }
    }

    override fun getNewer(id: Long): Flow<Int> = flow {
        Log.d("PostRepositoryImpl", "Старт потока новых постов новее id: $id")
        while (true) {
            delay(10_000)
            val response = apiService.getNewer(id)
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
            val posts = response.body() ?: throw ApiError(response.code(), response.message())
            val hiddenPosts = posts.map { it.copy(hidden = true) }
            dao.insert(hiddenPosts.toPostEntity(hidden = true))
            Log.d("PostRepositoryImpl", "Получено ${hiddenPosts.size} новых скрытых постов")
            emit(hiddenPosts.size)
        }
    }.catch { e ->
        Log.e("PostRepositoryImpl", "Ошибка при загрузке новых постов", e)
        emit(0)
    }

    override suspend fun update(post: Post) {
        Log.d("PostRepositoryImpl", "Обновление поста: $post")
        dao.update(post.id, post.content, post.published, post.isSynced)
    }

    override fun getHiddenSyncedCount(): Flow<Int> {
        Log.d("PostRepositoryImpl", "Получение количества скрытых синхронизированных постов")
        return dao.getHiddenSyncedCount()
    }

    override suspend fun unhideAllSyncedPosts() {
        Log.d("PostRepositoryImpl", "Отображение всех синхронизированных скрытых постов")
        dao.unhideAllSyncedPosts()
    }

    override suspend fun save(post: Post, file: File): Post {
        Log.d("PostRepositoryImpl", "Сохранение поста с изображением: $post, файл: ${file.name}")
        return try {
            val media = mediaRepository.upload(file)
            val response = apiService.save(
                post.copy(attachment = Attachment(media.id, AttachmentType.IMAGE))
            )
            if (!response.isSuccessful) throw RuntimeException("Ошибка сохранения: ${response.code()}")
            val saved = response.body() ?: throw RuntimeException("Пустой ответ при сохранении")
            dao.insert(PostEntity.Companion.fromDto(saved.copy(isSynced = true)))
            Log.d("PostRepositoryImpl", "Пост с изображением успешно сохранён: $saved")
            saved
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Ошибка сохранения поста с файлом", e)
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