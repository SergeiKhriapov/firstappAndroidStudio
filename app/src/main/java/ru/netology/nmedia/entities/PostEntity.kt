package ru.netology.nmedia.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import kotlin.Long

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val idLocal: Long,
    val author: String,
    val authorId: Long,
    val authorAvatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val shareCount: Int = 0,
    val viewsCount: Int = 0,
    val video: String? = null,
    val hidden: Boolean = true,
    @Embedded
    val attachment: AttachmentEmbedded? = null,


    val isSynced: Boolean = true
) {

    companion object {
        fun fromDto(dto: Post, hidden: Boolean = dto.hidden): PostEntity = PostEntity(
            id = dto.id,
            idLocal = dto.idLocal,
            author = dto.author,
            authorId = dto.authorId,
            authorAvatar = dto.authorAvatar,
            content = dto.content,
            published = dto.published,
            likedByMe = dto.likedByMe,
            likes = dto.likes,
            shareCount = dto.shareCount,
            viewsCount = dto.viewsCount,
            video = dto.video,
            hidden = hidden,
            attachment = dto.attachment?.let { AttachmentEmbedded.fromDto(it) },
            isSynced = true
        )
    }

    fun toDto(): Post = Post(
        id = id,
        idLocal = idLocal,
        author = author,
        authorId = authorId,
        authorAvatar = authorAvatar,
        content = content,
        published = published,
        likedByMe = likedByMe,
        likes = likes,
        shareCount = shareCount,
        viewsCount = viewsCount,
        video = video,
        hidden = hidden,
        attachment = attachment?.toDto(),
        isSynced = true
    )
}

fun List<Post>.toPostEntity(hidden: Boolean): List<PostEntity> =
    map { PostEntity.fromDto(it, hidden) }

fun List<PostEntity>.toLocalPostDto(): List<Post> =
    map { it.toDto() }
