package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post

class PostRepositoryInMemory : PostRepository {
    private val data = MutableLiveData<Post>(
        Post(
            id = 1,
            author = "Нетология. Университет интернет-профессий",
            published = "06 ноября в 21:20",
            content = "Привет, это новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия - помочь встать на путь роста и начать цепочку перемен - http://netolo.gy/lyb",
            likeCount = 0,
            shareCount = 0,
            viewsCount = 0
        )
    )

    override fun getPost(): LiveData<Post> = data

    override fun like() {
        val currentPost = data.value ?: return
        val updatedPost = currentPost.copy(
            likeByMe = !currentPost.likeByMe,
            likeCount = if (currentPost.likeByMe)
                currentPost.likeCount - 1 else currentPost.likeCount + 1
        )
        data.value = updatedPost
    }

    override fun share() {
        val currentPost = data.value ?: return
        val updatePost = currentPost.copy(shareCount = currentPost.shareCount + 1)
        data.value = updatePost
    }

    override fun view() {
        val currentPost = data.value ?: return
        val updatedPost = currentPost.copy(viewsCount = currentPost.viewsCount + 1)
        data.value = updatedPost
    }
}