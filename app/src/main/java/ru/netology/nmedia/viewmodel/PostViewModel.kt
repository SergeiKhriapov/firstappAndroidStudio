package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import ru.netology.nmedia.repository.PostRepositoryInMemory

class PostViewModel : ViewModel() {
    private val repository = PostRepositoryInMemory()
    val data = repository.getAll()
    fun like(id: Long) {
        repository.like(id)
    }

    fun share(id: Long) {
        repository.share(id)
    }

    fun view(id: Long) {
        repository.view(id)
    }
}