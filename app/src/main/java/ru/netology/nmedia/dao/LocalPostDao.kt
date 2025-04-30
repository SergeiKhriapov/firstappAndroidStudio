package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.entities.LocalPostEntity
import ru.netology.nmedia.entities.PostEntity


@Dao
interface LocalPostDao {

    // Получаем все черновики
    @Query("SELECT * FROM LocalPostEntity ORDER BY published DESC")
    fun getAll(): Flow<List<LocalPostEntity>> // Возвращаем Flow с черновиками

    // Вставляем один черновик
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: LocalPostEntity): Long


    // Удаляем черновик по id
    @Query("DELETE FROM LocalPostEntity WHERE idLocal=:id")
    suspend fun removeByIdLocal(id: Long) // Переименовал метод для уникальности

    // Получаем черновик по id
    @Query("SELECT * FROM LocalPostEntity WHERE idLocal = :id")
    suspend fun getById(id: Long): LocalPostEntity?

    // Получаем все черновики, которые еще не синхронизированы
    @Query("SELECT * FROM LocalPostEntity WHERE isSynced = 0 ORDER BY published DESC")
    fun getUnsynced(): Flow<List<LocalPostEntity>>

    @Query("SELECT * FROM LocalPostEntity WHERE isSynced = 0")
    suspend fun getAllUnsynced(): List<LocalPostEntity>

    // Обновляем черновик по id
    @Query("UPDATE LocalPostEntity SET content=:text WHERE idLocal=:id")
    suspend fun edit(id: Long, text: String)

    // если id == 0L то добавляем, иначе обновляем)
    suspend fun save(post: LocalPostEntity) =
        if (post.idLocal== 0L) insert(post) else edit(post.idLocal, post.content)

    @Query("SELECT * FROM LocalPostEntity WHERE isSynced = 0")
    suspend fun getUnsyncedPosts(): List<LocalPostEntity>

    @Update
    suspend fun update(post: PostEntity)

    @Update
    suspend fun updateLocal(post: LocalPostEntity)


    @Query("UPDATE LocalPostEntity SET isSynced = 1 WHERE idLocal = :id")
    suspend fun markAsSynced(id: Long)


}