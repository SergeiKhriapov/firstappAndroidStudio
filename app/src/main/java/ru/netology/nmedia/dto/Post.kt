package ru.netology.nmedia.dto

data class Post(
    val id: Long = 0,
    val content: String? = null,
    val published: String? = null,
    val author: String? = null,
    var likeByMe: Boolean = false,
    var likeCount: Int = 0,
    var shareCount: Int = 0,
    var viewsCount: Int = 0
)