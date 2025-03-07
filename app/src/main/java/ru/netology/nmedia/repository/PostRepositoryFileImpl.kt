/*
package ru.netology.nmedia.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nmedia.dto.Post

class PostRepositoryFileImpl(private val context: Context) : PostRepository {
    companion object {
        private val gson = Gson()
        private val token = TypeToken.getParameterized(List::class.java, Post::class.java).type
        private const val FILENAME = "posts.json"
    }

    private var nextId = 1L
    private var posts = listOf(
        Post(
            id = nextId++,
            author = "Яндекс",
            published = "14.11.2024",
            content = "Пустой пост\n",
            likeCount = 0,
            shareCount = 0,
            viewsCount = 0,
        ),
        Post(
            id = nextId++,
            author = "Яндекс",
            published = "14.11.2024",
            content = "Компоненты RecyclerView www.1c.ru\n",
            likeCount = 0,
            shareCount = 0,
            viewsCount = 0,
            video = "https://www.youtube.com/watch?v=WhWc3b3KhnY"
        ),
        Post(
            id = nextId++,
            author = "Яндекс",
            published = "14.11.2015",
            content = "Пустой пост два\n",
            likeCount = 0,
            shareCount = 0,
            viewsCount = 0,
        ),
        Post(
            id = nextId++,
            author = "Яндекс",
            published = "14.11.2024",
            content = "Мультик 12 месяцев\n",
            likeCount = 0,
            shareCount = 0,
            viewsCount = 0,
            video = "https://www.youtube.com/watch?v=_l_dNQnN0uU"
        ),
        Post(
            id = nextId++,
            author = "Тест",
            published = "14.11.2024",
            content = "Мультик 12 месяцев\n",
            likeCount = 0,
            shareCount = 0,
            viewsCount = 0,
            video = "https://www.youtube.com/watch?v=_l_dNQnN0uU"
        )
    )
        set(value) {
            field = value
            sync()
        }
    private val data = MutableLiveData(posts)

    init {
        val file = context.filesDir.resolve(FILENAME)
        if (file.exists()) {
            context.openFileInput(FILENAME).bufferedReader().use {
                posts = gson.fromJson(it, token)
                nextId = (posts.maxOfOrNull { it.id } ?: 0L) + 1
                data.value = posts
            }
        } else {
            sync()
        }
    }

    */
/*override fun getAll(): LiveData<List<Post>> = data*//*

    override fun getAll(): List<Post> = posts

    override fun likeById(id: Long) {
        posts = posts.map {
            if (it.id != id) it else it.copy(
                likeByMe = !it.likeByMe,
                likeCount = if (!it.likeByMe) it.likeCount + 1 else it.likeCount - 1
            )
        }
        data.value = posts
    }

    override fun shareById(id: Long) {
        posts = posts.map {
            if (it.id == id) {
                it.copy(shareCount = it.shareCount + 1)
            } else {
                it
            }
        }
        data.value = posts
    }

    override fun viewById(id: Long) {
        posts = posts.map {
            if (it.id == id) {
                it.copy(viewsCount = it.viewsCount + 1)
            } else {
                it
            }
        }
        data.value = posts
    }

    override fun removeById(id: Long) {
        posts = posts.filter { it.id != id }
        data.value = posts
    }

    override fun save(post: Post) {
        if (post.id == 0L) {
            posts =
                posts + listOf(post.copy(id = nextId++, author = "Me", published = "01.01.2024"))
        } else {
            posts = posts.map {
                if (it.id != post.id) it else it.copy(content = post.content)
            }
        }
        data.value = posts
    }

    private fun sync() {
        context.openFileOutput(FILENAME, Context.MODE_PRIVATE).bufferedWriter().use {
            it.write(gson.toJson(posts))
        }
    }
}*/
