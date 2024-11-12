package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val author: String,
    val content: String,
    val published: String,
    val likeByMe: Boolean = false,
    val likeCount: Int = 0,
    val shareCount: Int = 0,
    val viewsCount: Int = 0
)