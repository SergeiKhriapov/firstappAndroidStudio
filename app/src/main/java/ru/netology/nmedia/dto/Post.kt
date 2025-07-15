package ru.netology.nmedia.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Post(
    val id: Long,
    val idLocal: Long,
    val author: String,
    val authorId: Long,
    val authorAvatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int,
    val shareCount: Int = 0,
    val viewsCount: Int = 0,
    val video: String? = null,
    val attachment: Attachment? = null,  // Attachment должен быть Parcelable
    val isSynced: Boolean = false,
    val hidden: Boolean = false,
    val ownedByMe: Boolean = false
) : Parcelable
