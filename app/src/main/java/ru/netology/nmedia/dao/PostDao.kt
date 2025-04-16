package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nmedia.entities.PostEntity

@Dao
interface PostDao {

    @Query("SELECT * FROM PostEntity ORDER BY ID DESC")
    fun getAll(): LiveData<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    @Query("UPDATE PostEntity SET content=:text WHERE id=:id")
    suspend fun edit(id: Long, text: String)

    suspend fun save(post: PostEntity) =
        if (post.id == 0L) insert(post) else edit(post.id, post.content)


    @Query(
        """
            UPDATE PostEntity SET
            likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
            likedByMe = CASE WHEN likedByMe THEN 0 else 1 END
            WHERE id = :id
        """
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
}