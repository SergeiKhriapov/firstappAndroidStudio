package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post

class PostRepositoryInMemory : PostRepository {
    private var nextId = 1L
    private var posts = listOf(
        Post(
            id = nextId++,
            author = "Яндекс",
            published = "Что такое RecyclerView 14.11.2024",
            content = "Компоненты RecyclerView www.1c.ru\n",
            likeCount = 0,
            shareCount = 0,
            viewsCount = 0
        ),
        Post(
            id = nextId++,
            author = "Нетология",
            published = "14.11.2024",
            content = "Задача\n" +
                    "На лекции мы стилизовали Like в виде CheckBox, а затем в виде Button. Вам также нужно стилизовать кнопки Share и Options — меню с тремя точками.\n" +
                    "\n" +
                    "Изучите документацию на компоненты Buttons, чтобы реализовать работу этих элементов. Обратите внимание: это не CheckBox.\n" +
                    "\n" +
                    "Текст можно задавать сразу через атрибуты кнопки (количество лайков, шеринга). Поэтому от TextView можно будет избавиться.\n" +
                    "\n" +
                    "Создайте отдельный стиль, как мы сделали на лекции, и назначайте его кнопкам.\n" +
                    "\n" +
                    "Подсказка\n" +
                    "В качестве результата пришлите ссылку на ваш GitHub-проект в личном кабинете студента на сайте netology.ru.",
            likeCount = 0,
            shareCount = 0,
            viewsCount = 0
        )
    )
    private val data = MutableLiveData(posts)

    override fun getAll(): LiveData<List<Post>> = data

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
                posts + listOf(post.copy(id = nextId++, author = "Me", published = "19.11.2024"))
        } else {
            posts = posts.map {
                if (it.id !=post.id) it else it.copy(content = post.content)
            }
        }
        data.value = posts
    }
}