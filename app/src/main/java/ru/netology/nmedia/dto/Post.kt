package ru.netology.nmedia.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface FeedItem {
    val id: Long
}

@Parcelize
data class Post(
    override val id: Long,
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
    val attachment: Attachment? = null,
    val hidden: Boolean = false,
    val ownedByMe: Boolean = false
) : Parcelable, FeedItem

@Parcelize
data class Ad(
    override val id: Long,
    val image: String
) : Parcelable, FeedItem

@Parcelize
data class Separator(
    override val id: Long,
    val title: String
) : Parcelable, FeedItem

object SeparatorTitles {
    const val TODAY = "Сегодня"
    const val YESTERDAY = "Вчера"
    const val LAST_WEEK = "На прошлой неделе, ПРОПУСКАЕТСЯ ОДИН РЕКЛАМНЫЙ ПОСТ"
}
