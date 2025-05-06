package ru.netology.nmedia.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post

@Entity(tableName = "LocalPostEntity")
data class LocalPostEntity(
    @PrimaryKey(autoGenerate = true)
    val idLocal: Long,
    val id: Long = 0,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val shareCount: Int = 0,
    val viewsCount: Int = 0,
    val video: String? = null,
    val isSynced: Boolean = false,
    @Embedded
    val attachment: AttachmentEmbedded? = null,

    ) {
    companion object {
        fun fromDto(dto: Post): LocalPostEntity = LocalPostEntity(
            idLocal = dto.idLocal,
            id = dto.id,
            author = dto.author,
            authorAvatar = dto.authorAvatar,
            content = dto.content,
            published = dto.published,
            likedByMe = dto.likedByMe,
            likes = dto.likes,
            shareCount = dto.shareCount,
            viewsCount = dto.viewsCount,
            video = dto.video,
            attachment = dto.attachment?.let { AttachmentEmbedded.fromDto(it) },

            )
    }

    fun toDto() = Post(
        id = 0,
        idLocal = idLocal,
        author = author,
        authorAvatar = authorAvatar,
        content = content,
        published = published,
        likedByMe = likedByMe,
        likes = likes,
        shareCount = shareCount,
        viewsCount = viewsCount,
        video = video,
        attachment = attachment?.toDto(),
    )
}

fun List<Post>.toLocalPostEntity(): List<LocalPostEntity> = map { LocalPostEntity.fromDto(it) }
fun List<LocalPostEntity>.toLocalPostDto(): List<Post> = map { it.toDto() }