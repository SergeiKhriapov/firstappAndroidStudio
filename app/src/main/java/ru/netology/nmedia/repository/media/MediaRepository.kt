package ru.netology.nmedia.repository.media

import android.util.Log
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dto.Media
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val apiService: PostsApiService
) {
    suspend fun upload(file: File): Media {
        Log.d("MediaRepository", "Загрузка файла: ${file.name}")

        val part = MultipartBody.Part.createFormData(
            "file", file.name, file.asRequestBody()
        )
        val response = apiService.upload(part)
        if (response.isSuccessful) {
            val media = response.body()
            if (media != null) {
                Log.d("MediaRepository", "Файл успешно загружен. Media ID: ${media.id}")
                return media
            } else {
                throw RuntimeException("Ошибка: пустое тело ответа при загрузке файла")
            }
        } else {
            throw RuntimeException("Ошибка загрузки файла: ${response.code()} ${response.message()}")
        }
    }
}
