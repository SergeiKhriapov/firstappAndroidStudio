package ru.netology.nmedia.repository.post

import android.util.Log
import androidx.paging.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.*
import ru.netology.nmedia.entities.PostEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.repository.media.MediaRepository
import ru.netology.nmedia.repository.paging.PostRemoteMediator
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val mediaRepository: MediaRepository,
    private val apiService: PostsApiService,
    val postRemoteKeyDao: PostRemoteKeyDao,
    val appDb: AppDb,
) : PostRepository {

    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<FeedItem>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = { dao.getPagingSource() },
        remoteMediator = PostRemoteMediator(
            apiService = apiService,
            postDao = dao,
            appDb = appDb,
            postRemoteKeyDao = postRemoteKeyDao
        )
    ).flow.map { pagingData ->
        pagingData
            .map<PostEntity, FeedItem> { it.toDto() }
            .insertSeparators<FeedItem, FeedItem> { before, after ->

                fun getTitle(post: Post): String {
                    val now = System.currentTimeMillis()
                    val publishedMillis = post.published * 1000
                    val diff = now - publishedMillis
                    val oneDay = 24 * 60 * 60 * 1000L
                    val twoDays = 2 * oneDay

                    return when {
                        diff < oneDay -> SeparatorTitles.TODAY
                        diff < twoDays -> SeparatorTitles.YESTERDAY
                        else -> SeparatorTitles.LAST_WEEK
                    }
                }

                // Сначала сепаратор по дате
                if (after is Post && (before == null || before !is Post)) {
                    return@insertSeparators Separator(-Random.nextLong(), getTitle(after))
                }

                if (before is Post && after is Post) {
                    val beforeTitle = getTitle(before)
                    val afterTitle = getTitle(after)
                    if (beforeTitle != afterTitle) {
                        return@insertSeparators Separator(-Random.nextLong(), afterTitle)
                    }
                }

                // Вставка рекламы каждые 5 постов
                if (before is Post && before.id % 5 == 0L) {
                    return@insertSeparators Ad(Random.nextLong(), "figma.jpg")
                }

                null
            }


    }

    override suspend fun getAll() {
        try {
            val response = apiService.getAll()
            if (!response.isSuccessful) throw RuntimeException("Ошибка загрузки: ${response.code()}")
            val posts = response.body() ?: throw RuntimeException("Пустой ответ от сервера")
            dao.insert(posts.map { PostEntity.fromDto(it) })
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Ошибка при получении постов", e)
            throw e
        }
    }

    override suspend fun save(post: Post): Post {
        return try {
            val response = apiService.save(post)
            if (!response.isSuccessful) throw RuntimeException("Ошибка сохранения: ${response.code()}")
            val saved = response.body() ?: throw RuntimeException("Пустой ответ при сохранении")
            dao.insert(PostEntity.fromDto(saved))
            saved
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Ошибка сохранения поста", e)
            throw e
        }
    }

    override suspend fun save(post: Post, file: File): Post {
        return try {
            val media = mediaRepository.upload(file)
            val response = apiService.save(
                post.copy(attachment = Attachment(media.id, AttachmentType.IMAGE))
            )
            if (!response.isSuccessful) throw RuntimeException("Ошибка сохранения: ${response.code()}")
            val saved = response.body() ?: throw RuntimeException("Пустой ответ при сохранении")
            dao.insert(PostEntity.fromDto(saved))
            saved
        } catch (e: Exception) {
            Log.e("PostRepositoryImpl", "Ошибка сохранения поста с файлом", e)
            throw e
        }
    }

    override suspend fun likeById(id: Long): Post {
        val post = dao.getById(id)?.toDto() ?: throw Exception("Post not found")
        val updated = post.copy(likedByMe = true, likes = post.likes + 1)
        dao.insert(PostEntity.fromDto(updated))

        return try {
            val response = apiService.likeById(id)
            if (!response.isSuccessful) throw RuntimeException("Ошибка лайка: ${response.code()}")
            val likedPost = response.body() ?: throw RuntimeException("Пустой ответ при лайке")
            dao.insert(PostEntity.fromDto(likedPost))
            likedPost
        } catch (e: Exception) {
            dao.insert(PostEntity.fromDto(post)) // откат
            throw e
        }
    }

    override suspend fun dislikeById(id: Long): Post {
        val post = dao.getById(id)?.toDto() ?: throw Exception("Post not found")
        val updated = post.copy(likedByMe = false, likes = post.likes - 1)
        dao.insert(PostEntity.fromDto(updated))

        return try {
            val response = apiService.dislikeById(id)
            if (!response.isSuccessful) throw RuntimeException("Ошибка дизлайка: ${response.code()}")
            val dislikedPost = response.body() ?: throw RuntimeException("Пустой ответ при дизлайке")
            dao.insert(PostEntity.fromDto(dislikedPost))
            dislikedPost
        } catch (e: Exception) {
            dao.insert(PostEntity.fromDto(post)) // откат
            throw e
        }
    }

    override suspend fun removeById(id: Long) {
        val post = dao.getById(id)
        post?.let { dao.removeById(id) }

        try {
            val response = apiService.removeById(id)
            if (!response.isSuccessful) throw RuntimeException("Ошибка удаления: ${response.code()}")
        } catch (e: Exception) {
            post?.let { dao.insert(it) }
            throw e
        }
    }

    override fun getNewer(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000)
            val response = apiService.getNewer(id)
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
            val posts = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(posts.map { PostEntity.fromDto(it) })
            emit(posts.size)
        }
    }.catch { e ->
        Log.e("PostRepositoryImpl", "Ошибка при загрузке новых постов", e)
        emit(0)
    }

    override suspend fun update(post: Post) {
        dao.update(post.id, post.content, post.published)
    }

    override fun shareById(id: Long) { /* TODO */ }

    override fun viewById(id: Long) { /* TODO */ }
}
