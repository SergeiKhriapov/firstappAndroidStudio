package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {

    @Query("SELECT * FROM PostEntity ORDER BY ID DESC")
    fun getAll(): LiveData<List<PostEntity>>

    @Insert
    fun insert(post: PostEntity)

    @Query("UPDATE PostEntity SET content=:text WHERE id=:id")
    fun edit(id: Long, text: String)

    fun save(post: PostEntity) = if (post.id == 0L) insert(post) else edit(post.id, post.content)

    @Query(
        """
            UPDATE PostEntity SET
            likeCount = likeCount + CASE WHEN likeByMe THEN -1 ELSE 1 END,
            likeByMe = CASE WHEN likeByMe THEN 0 else 1 END
            WHERE id = :id
        """
    )
    fun likeById(id: Long)

    @Query(
        "DELETE FROM PostEntity WHERE ID=:id"
    )
    fun removeById(id: Long)


    @Query(
        """
            UPDATE PostEntity SET
            shareCount = shareCount + 1
            WHERE id = :id
        """
    )
    fun shareById(id: Long)

    @Query(
        """
            UPDATE PostEntity SET
            viewsCount = viewsCount + 1
            WHERE id = :id
        """
    )
    fun viewById(id: Long)
}
