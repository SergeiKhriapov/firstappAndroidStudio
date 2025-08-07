package ru.netology.nmedia.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.entities.PostEntity

@Dao
interface PostDao {

    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getPagingSource(): PagingSource<Int, PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Query("SELECT * FROM PostEntity WHERE hidden = 0 ORDER BY id DESC")
    fun getVisiblePosts(): Flow<List<PostEntity>>

    @Query("SELECT COUNT(*) FROM PostEntity WHERE hidden = 1")
    fun getNewerCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    @Query("UPDATE PostEntity SET content = :text WHERE id = :id")
    suspend fun edit(id: Long, text: String)

    suspend fun save(post: PostEntity) {
        if (post.id == 0L) {
            insert(post)
        } else {
            // Обновляем только поля, которые есть — без isSynced
            update(post.id, post.content, post.published)
        }
    }

    @Query(
        """
        UPDATE PostEntity SET 
            likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
            likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
        WHERE id = :id
    """
    )
    suspend fun likeById(id: Long)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query(
        """
        UPDATE PostEntity SET
            shareCount = shareCount + 1
        WHERE id = :id
    """
    )
    suspend fun shareById(id: Long)

    @Query(
        """
        UPDATE PostEntity SET
            viewsCount = viewsCount + 1
        WHERE id = :id
    """
    )
    suspend fun viewById(id: Long)

    @Query("SELECT * FROM PostEntity WHERE id = :id")
    suspend fun getById(id: Long): PostEntity?

    @Query(
        """
        UPDATE PostEntity SET 
            content = :content,
            published = :published
        WHERE id = :id
    """
    )
    suspend fun update(
        id: Long,
        content: String,
        published: Long
    )

    @Query("SELECT * FROM PostEntity")
    suspend fun getAllNow(): List<PostEntity>

    @Query("DELETE FROM PostEntity")
    suspend fun clear()


}
