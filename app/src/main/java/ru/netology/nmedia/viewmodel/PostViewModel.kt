package ru.netology.nmedia.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepositoryInMemory

private val empty = Post()

class PostViewModel : ViewModel() {

    private val repository = PostRepositoryInMemory()

    val data = repository.getAll()

    private val _edited = MutableLiveData(empty)

    fun startEditing(post: Post) {
        _edited.value = post
    }

    fun cancelEditing() {
        _edited.value = empty
    }

    fun saveContent(content: String) {
        _edited.value?.let {
            repository.save(it.copy(content = content))
        }
        cancelEditing()
    }

    fun likeById(id: Long) = repository.likeById(id)
    fun shareById(id: Long) = repository.shareById(id)
    fun viewById(id: Long) = repository.viewById(id)
    fun removeById(id: Long) = repository.removeById(id)
}
