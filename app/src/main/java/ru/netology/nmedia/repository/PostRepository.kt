package ru.netology.nmedia.repository


import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAllAsync(callback: AsyncCallback<List<Post>>)
    fun save(post: Post, callback: AsyncCallback<Post>)
    fun removeById(id: Long, callback: AsyncCallback<Unit>)
    fun likeById(post: Post, callback: AsyncCallback<Post>)
    fun shareById(id: Long)
    fun viewById(id: Long)

    interface AsyncCallback<T> {
        fun onSuccess(result: T)
        fun onError(e: Exception)
    }
}
