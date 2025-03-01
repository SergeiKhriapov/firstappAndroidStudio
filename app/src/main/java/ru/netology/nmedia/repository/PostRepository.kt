package ru.netology.nmedia.repository


import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAll(): List<Post>
    fun save(post: Post): Post
    fun removeById(id: Long)
    fun likeById(post: Post): Post
    fun shareById(id: Long)
    fun viewById(id: Long)
}
/* fun getAllAsync(callback: GetAllCallback)
 interface GetAllCallback {
     fun onSuccess(posts: List<Post>) {}
     fun onError(e: Exception) {}
 }

 interface LikeCallback {
     fun onSuccess(updatedPost: Post)
     fun onError(e: Exception)
 }*/
