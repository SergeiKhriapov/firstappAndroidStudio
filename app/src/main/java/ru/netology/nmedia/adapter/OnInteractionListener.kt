package ru.netology.nmedia.adapter

import ru.netology.nmedia.dto.Post

interface OnInteractionListener {
    fun onLike(post: Post)
    fun onShare(post: Post)
    fun onView(post: Post)
    fun onRemove(post: Post)
    fun onEdit(post: Post)
    fun onVideoClick(post: Post)
    fun focusOnPost(post: Post)
    fun focusOnAttachment(post: Post)
    fun showError(message: String)
}