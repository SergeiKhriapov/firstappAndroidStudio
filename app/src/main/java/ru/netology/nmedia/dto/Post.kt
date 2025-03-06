package ru.netology.nmedia.dto
import ru.netology.nmedia.dt.Attachment

data class Post(
    val id: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int,
    val shareCount: Int = 0,
    val viewsCount: Int = 0,
    val video: String? = null,
    val attachment: Attachment? = null
)