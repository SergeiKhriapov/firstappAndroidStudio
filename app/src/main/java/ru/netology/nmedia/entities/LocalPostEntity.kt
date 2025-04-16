package ru.netology.nmedia.entities

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
    val isSynced: Boolean = false, // не синхронизирован

    // Исправил вложенные файлы. Спросить про конвертер json.
    val attachmentUrl: String? = null,
    val attachmentDescription: String? = null,
    val attachmentType: String? = null,

    ) {
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
        attachment = if (attachmentUrl != null && attachmentType != null)
            Attachment(
                url = attachmentUrl,
                description = attachmentDescription,
                type = AttachmentType.valueOf(attachmentType)
            )
        else null
    )

    companion object {
        fun fromDto(post: Post): LocalPostEntity = LocalPostEntity(
            idLocal = post.idLocal,
            id = post.id,
            author = post.author,
            authorAvatar = post.authorAvatar,
            content = post.content,
            published = post.published,
            likedByMe = post.likedByMe,
            likes = post.likes,
            shareCount = post.shareCount,
            viewsCount = post.viewsCount,
            video = post.video,
            attachmentUrl = post.attachment?.url,
            attachmentDescription = post.attachment?.description,
            attachmentType = post.attachment?.type?.name,
        )
    }
}

fun List<Post>.toLocalPostEntity(): List<LocalPostEntity> = map { LocalPostEntity.fromDto(it) }
fun List<LocalPostEntity>.toPostDto(): List<Post> = map { it.toDto() }