package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import ru.netology.nmedia.entities.LocalPostEntity





@Dao
interface LocalPostEntityDao {

    // Получаем все черновики
    @Query("SELECT * FROM LocalPostEntity ORDER BY published DESC")
    fun getAll(): LiveData<List<LocalPostEntity>> // Возвращаем LiveData с черновиками

    // Вставляем один черновик
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: LocalPostEntity)

    // Удаляем черновик по id
    @Query("DELETE FROM LocalPostEntity WHERE idLocal=:id")
    suspend fun removeByIdLocal(id: Long) // Переименовал метод для уникальности

    // Получаем черновик по id
    @Query("SELECT * FROM LocalPostEntity WHERE idLocal = :id")
    suspend fun getById(id: Long): LocalPostEntity?

    // Получаем все черновики, которые еще не синхронизированы
    @Query("SELECT * FROM LocalPostEntity WHERE isSynced = 0 ORDER BY published DESC")
    fun getUnsynced(): LiveData<List<LocalPostEntity>>

    // Обновляем черновик по id
    @Query("UPDATE LocalPostEntity SET content=:text WHERE idLocal=:id")
    suspend fun edit(id: Long, text: String)

    // Сохраняем черновик (если id == 0L, то добавляем, иначе обновляем)
    suspend fun save(post: LocalPostEntity) =
        if (post.idLocal== 0L) insert(post) else edit(post.idLocal, post.content)
}
