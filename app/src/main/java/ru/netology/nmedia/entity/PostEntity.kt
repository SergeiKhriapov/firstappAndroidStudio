package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val author: String = "",
    val content: String = "",
    val published: String = "",
    val likeByMe: Boolean = false,
    val likeCount: Int = 0,
    val shareCount: Int = 0,
    val viewsCount: Int = 0,
    val video: String? = null
) {
    fun toDto() =
        Post(id, author, content, published, likeByMe, likeCount, shareCount, viewsCount, video)

    companion object {
        fun fromDto(post: Post) = PostEntity(
            post.id,
            post.author,
            post.content,
            post.published,
            post.likeByMe,
            post.likeCount,
            post.shareCount,
            post.viewsCount,
            post.video
        )
    }

}
