package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.entities.PostEntity

@Dao
interface PostDao {

    @Query("SELECT * FROM PostEntity ORDER BY ID DESC")
    fun getAll(): Flow<List<PostEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Query("SELECT * FROM PostEntity WHERE hidden = 0 ORDER BY id DESC")
    fun getVisiblePosts(): Flow<List<PostEntity>>

    @Query("SELECT COUNT(*) FROM PostEntity WHERE hidden = 1")
    fun getNewerCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM PostEntity WHERE hidden = 1 AND isSynced = 1")
    fun getHiddenSyncedCount(): Flow<Int>

    @Query("UPDATE PostEntity SET hidden = 0 WHERE hidden = 1 AND isSynced = 1")
    suspend fun unhideAllSyncedPosts()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    @Query("UPDATE PostEntity SET content=:text WHERE id=:id")
    suspend fun edit(id: Long, text: String)

    suspend fun save(post: PostEntity) {
        if (post.id == 0L) {
            insert(post)
        } else {
            update(post.id, post.content, post.published, post.isSynced)
        }
    }

    @Query(
        """UPDATE PostEntity SET likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
            likedByMe = CASE WHEN likedByMe THEN 0 else 1 END
            WHERE id = :id"""
    )
    suspend fun likeById(id: Long)


    @Query("DELETE FROM PostEntity WHERE ID=:id")
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
            published = :published,
            isSynced = :isSynced
        WHERE id = :id
    """
    )
    suspend fun update(
        id: Long,
        content: String,
        published: Long,
        isSynced: Boolean
    )
    @Query("SELECT * FROM PostEntity")
    suspend fun getAllNow(): List<PostEntity>
}