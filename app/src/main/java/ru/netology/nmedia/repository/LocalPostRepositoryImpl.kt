package ru.netology.nmedia.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nmedia.api.Api
import ru.netology.nmedia.dao.LocalPostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entities.LocalPostEntity
import ru.netology.nmedia.entities.toLocalPostDto
import java.io.File

class LocalPostRepositoryImpl(private val dao: LocalPostDao, private val mediaRepository: MediaRepository) {
    val data = dao.getAll()
        .map(List<LocalPostEntity>::toLocalPostDto)
        .flowOn(Dispatchers.Default)
    suspend fun getAll() {
        TODO("Not yet implemented")
    }
    suspend fun save(post: Post): Long {
        return dao.insert(LocalPostEntity.fromDto(post))
    }
    suspend fun syncUnsyncedPosts(): Boolean {
        var wasError = false

        try {
            Log.d("LocalPostRepository", "Начинаем синхронизацию черновиков...")

            val unsyncedPosts = dao.getAllUnsynced()

            if (unsyncedPosts.isEmpty()) {
                Log.d("LocalPostRepository", "Нет черновиков для синхронизации.")
                return false
            }

            Log.d("LocalPostRepository", "Найдено ${unsyncedPosts.size} несинхронизированных черновиков.")

            for (localPost in unsyncedPosts) {
                try {
                    var postDto = localPost.toDto()
                    Log.d("LocalPostRepository", "Обработка черновика ${localPost.idLocal}...")

                    val originalAttachment = postDto.attachment

                    if (originalAttachment != null) {
                        val file = File(originalAttachment.url)
                        if (file.exists()) {
                            val uploadedMedia = mediaRepository.upload(file)
                            postDto = postDto.copy(
                                attachment = Attachment(
                                    url = uploadedMedia.id.toString(), // передаём mediaId как url
                                    type = originalAttachment.type
                                )
                            )
                            Log.d("LocalPostRepository", "Файл ${file.name} успешно загружен, mediaId=${uploadedMedia.id}")
                        } else {
                            Log.w("LocalPostRepository", "Файл не найден: ${originalAttachment.url}")
                        }
                    }

                    Api.retrofitService.save(postDto)
                    dao.removeByIdLocal(localPost.idLocal)

                    Log.d("LocalPostRepository", "Черновик ${localPost.idLocal} синхронизирован и удалён.")
                } catch (e: Exception) {
                    wasError = true
                    Log.e("LocalPostRepository", "Ошибка при синхронизации черновика ${localPost.idLocal}", e)
                }
            }

        } catch (e: Exception) {
            wasError = true
            Log.e("LocalPostRepository", "Ошибка при общей синхронизации: ${e.message}", e)
        }

        return wasError
    }


    /*suspend fun syncUnsyncedPosts(): Boolean {
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

            for (localPost in unsyncedPosts) {
                try {
                    val postDto = localPost.toDto()
                    Log.d(
                        "LocalPostRepository",
                        "Отправка черновика ${localPost.idLocal} на сервер..."
                    )

                    Api.retrofitService.save(postDto)
                    dao.removeByIdLocal(localPost.idLocal)

                    Log.d(
                        "LocalPostRepository",
                        "Черновик ${localPost.idLocal} синхронизирован и удалён."
                    )
                } catch (e: Exception) {
                    wasError = true
                    Log.d(
                        "LocalPostRepository",
                        "Ошибка при синхронизации черновика ${localPost.idLocal}",
                        e
                    )
                }
            }

        } catch (e: Exception) {
            wasError = true
            Log.e("LocalPostRepository", "Ошибка при общей синхронизации: ${e.message}", e)
        }

        return wasError
    }*/


    suspend fun update(post: Post) {
        dao.updateLocal(LocalPostEntity.fromDto(post))
    }

    suspend fun removeById(id: Long) {
        dao.removeByIdLocal(id)
    }

    // Лайк и дизлайк не поддерживаются
    suspend fun likeById(id: Long): Post {
        throw UnsupportedOperationException("Likes are not supported for drafts")
    }

    suspend fun dislikeById(id: Long): Post {
        throw UnsupportedOperationException("Likes are not supported for drafts")
    }

    fun shareById(id: Long) {
        //
    }

    fun viewById(id: Long) {
        //
    }
}
