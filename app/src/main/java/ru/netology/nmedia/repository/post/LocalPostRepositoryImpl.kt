package ru.netology.nmedia.repository.post

import android.util.Log
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dao.LocalPostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entities.LocalPostEntity
import ru.netology.nmedia.entities.toLocalPostDto
import ru.netology.nmedia.repository.media.MediaRepository
import java.io.File
import javax.inject.Inject


class LocalPostRepositoryImpl @Inject constructor(
    private val dao: LocalPostDao,
    private val mediaRepository: MediaRepository,
    private val apiService: PostsApiService

) : LocalPostRepository {
    override val data = dao.getAll()
        .map(List<LocalPostEntity>::toLocalPostDto)
        .flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        Log.d("LocalPostRepository", "Вызван getAll() - пока не реализован")
        TODO("Not yet implemented")
    }

    override suspend fun save(post: Post): Long {
        val id = dao.insert(LocalPostEntity.Companion.fromDto(post))
        Log.d(
            "LocalPostRepository",
            "Сохранён локальный пост idLocal=$id, content='${post.content.take(30)}...'"
        )
        return id
    }

    override suspend fun syncUnsyncedPosts(): Boolean {
        var wasError = false

        try {
            Log.d("LocalPostRepository", "Начинаем синхронизацию черновиков...")

            val unsyncedPosts = dao.getAllUnsynced()

            if (unsyncedPosts.isEmpty()) {
                Log.d("LocalPostRepository", "Нет черновиков для синхронизации.")
                return false
            }

            Log.d(
                "LocalPostRepository",
                "Найдено ${unsyncedPosts.size} несинхронизированных черновиков."
            )

            for ((index, localPost) in unsyncedPosts.withIndex()) {
                try {
                    Log.d(
                        "LocalPostRepository",
                        "Обработка черновика idLocal=${localPost.idLocal}, content='${
                            localPost.content.take(
                                30
                            )
                        }...', " +
                                "вложение ${if (localPost.attachment != null) "присутствует" else "отсутствует"}"
                    )

                    var postDto = localPost.toDto()
                    val originalAttachment = postDto.attachment

                    if (originalAttachment != null) {
                        val file = File(originalAttachment.url)
                        Log.d("LocalPostRepository", "Проверяем файл вложения: ${file.path}")

                        if (file.exists()) {
                            Log.d(
                                "LocalPostRepository",
                                "Файл найден, начинаем загрузку: ${file.name}"
                            )
                            val uploadedMedia = mediaRepository.upload(file)
                            postDto = postDto.copy(
                                attachment = Attachment(
                                    url = uploadedMedia.id.toString(),
                                    type = originalAttachment.type
                                )
                            )
                            Log.d(
                                "LocalPostRepository",
                                "Файл ${file.name} успешно загружен, mediaId=${uploadedMedia.id}"
                            )
                        } else {
                            Log.w(
                                "LocalPostRepository",
                                "Файл вложения не найден: ${originalAttachment.url}"
                            )
                        }
                    }

                    Log.d(
                        "LocalPostRepository",
                        "Отправляем пост idLocal=${localPost.idLocal} на сервер..."
                    )
                    apiService.save(postDto)
                    Log.d(
                        "LocalPostRepository",
                        "Пост успешно сохранён на сервере: idLocal=${localPost.idLocal}"
                    )
                    dao.removeByIdLocal(localPost.idLocal)
                    Log.d(
                        "LocalPostRepository",
                        "Локальный черновик удалён из БД: idLocal=${localPost.idLocal}"
                    )

                    val remaining = unsyncedPosts.size - index - 1
                    Log.d(
                        "LocalPostRepository",
                        "Осталось черновиков для синхронизации: $remaining"
                    )

                } catch (e: Exception) {
                    wasError = true
                    Log.e(
                        "LocalPostRepository",
                        "Ошибка при синхронизации черновика idLocal=${localPost.idLocal}",
                        e
                    )
                }
            }

        } catch (e: Exception) {
            wasError = true
            Log.e("LocalPostRepository", "Ошибка при общей синхронизации: ${e.message}", e)
        }

        Log.d("LocalPostRepository", "Синхронизация черновиков завершена, были ошибки: $wasError")
        return wasError
    }

    override suspend fun update(post: Post) {
        dao.updateLocal(LocalPostEntity.Companion.fromDto(post))
        Log.d(
            "LocalPostRepository",
            "Обновлён локальный пост idLocal=${post.id}, content='${post.content.take(30)}...'"
        )
    }

    override suspend fun removeById(id: Long) {
        dao.removeByIdLocal(id)
        Log.d("LocalPostRepository", "Удалён локальный пост idLocal=$id")
    }

    // Лайк и дизлайк не поддерживаются

    override suspend fun likeById(id: Long): Post {
        Log.w(
            "LocalPostRepository",
            "Попытка поставить лайк черновику idLocal=$id - не поддерживается"
        )
        throw UnsupportedOperationException("Likes are not supported for drafts")
    }

    override suspend fun dislikeById(id: Long): Post {
        Log.w(
            "LocalPostRepository",
            "Попытка поставить дизлайк черновику idLocal=$id - не поддерживается"
        )
        throw UnsupportedOperationException("Likes are not supported for drafts")
    }

    override fun shareById(id: Long) {
        Log.d("LocalPostRepository", "Вызван shareById для idLocal=$id (метод заглушка)")
        // пусто
    }

    override fun viewById(id: Long) {
        Log.d("LocalPostRepository", "Вызван viewById для idLocal=$id (метод заглушка)")
        // пусто
    }
}