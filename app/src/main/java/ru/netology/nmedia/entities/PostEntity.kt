package ru.netology.nmedia.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val idLocal: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val shareCount: Int = 0,
    val viewsCount: Int = 0,
    val video: String? = null,
    val hidden: Boolean = true,

    // Поля для вложений
    val attachmentUrl: String? = null,
    val attachmentDescription: String? = null,
    val attachmentType: String? = null,

    val isSynced: Boolean = true
) {

    // Метод для преобразования PostEntity в Post DTO
    fun toDto(): Post = Post(
        id = id,
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
        hidden = hidden,

        // Преобразование вложения в DTO, если оно есть
        attachment = if (attachmentUrl != null && attachmentType != null) {
            Attachment(
                url = attachmentUrl,
                description = attachmentDescription,
                type = AttachmentType.valueOf(attachmentType)
            )
        } else null,

        isSynced = true
    )

    companion object {
        // Метод для преобразования Post DTO в PostEntity
        fun fromDto(post: Post, hidden: Boolean = post.hidden): PostEntity = PostEntity(
            id = post.id,
            idLocal = post.idLocal,
            author = post.author,
            authorAvatar = post.authorAvatar,
            content = post.content,
            published = post.published,
            likedByMe = post.likedByMe,
            likes = post.likes,
            shareCount = post.shareCount,
            viewsCount = post.viewsCount,
            video = post.video,
            hidden = hidden, // Параметр hidden передается в метод
            attachmentUrl = post.attachment?.url,
            attachmentDescription = post.attachment?.description,
            attachmentType = post.attachment?.type?.name,
            isSynced = true
        )
    }
}

// Расширение для преобразования списка Post в список PostEntity
fun List<Post>.toPostEntity(hidden: Boolean): List<PostEntity> =
    map { PostEntity.fromDto(it, hidden) }

// Расширение для преобразования списка PostEntity в список Post DTO
fun List<PostEntity>.toLocalPostDto(): List<Post> =
    map { it.toDto() }
