package ru.netology.nmedia.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.LocalPostRepositoryImpl
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent

private val emptyPost = Post(
    id = 0,
    idLocal = 0,
    author = "",
    authorAvatar = "",
    content = "",
    published = 0,
    likedByMe = false,
    likes = 0,
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDb.getInstance(application)
    private val repository: PostRepository = PostRepositoryImpl(database.postDao)
    private val localRepository: LocalPostRepositoryImpl = LocalPostRepositoryImpl(database.localPostEntityDao)

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState> get() = _dataState

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit> get() = _postCreated

    private val _edited = MutableLiveData<Post?>(emptyPost)
    val edited: LiveData<Post?> get() = _edited

    private var draftContent: String? = null

    private val allPosts = MediatorLiveData<List<Post>>().apply {
        var synced: List<Post> = emptyList()
        var unsynced: List<Post> = emptyList()

        fun combine() {
            value = (synced + unsynced)
                .distinctBy { it.idLocal } // Убираем дубликаты
                .sortedByDescending { it.published }
        }

        addSource(repository.data) { newSynced ->
            synced = newSynced.filter { it.isSynced } // Только синхронизированные
            combine()
        }

        addSource(localRepository.data) { newUnsynced ->
            unsynced = newUnsynced.filter { !it.isSynced } // Только локальные
            combine()
        }
    }

    val data: LiveData<FeedModel> = allPosts.map { posts ->
        FeedModel(posts = posts, empty = posts.isEmpty())
    }

    init {
        loadPosts()
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAll() // загружаем с сервера
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            Log.e("PostViewModel", "Error loading posts", e)
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun refreshPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(refreshing = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            Log.e("PostViewModel", "Error refreshing posts", e)
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun saveContent(content: String) {
        edited.value?.let { post ->
            viewModelScope.launch {
                val newPost = post.copy(
                    content = content,
                    published = System.currentTimeMillis(),
                    isSynced = false, //
                    idLocal = if (post.idLocal == 0L) 0 else post.idLocal,
                    id = 0
                )
                try {
                    // Сохраняем в локальную БД
                    Log.d("PostViewModel", "saveContent: $newPost ")
                    localRepository.save(newPost)

                    // Пробуем синхронизировать с сервером
                    try {
                        val syncedPost = repository.save(newPost.copy(isSynced = true))
                        localRepository.removeById(syncedPost.idLocal)
                    } catch (e: Exception) {
                        Log.d("PostViewModel", "Нет соединения с сервером. Сохранили только локально: ${e.message}")
                    }

                    _postCreated.postValue(Unit)
                    _edited.postValue(emptyPost)
                } catch (e: Exception) {
                    Log.e("PostViewModel", "Ошибка при сохранении", e)
                    _dataState.postValue(FeedModelState(error = true))
                }
            }
        }
    }


    fun likeById(id: Long) = viewModelScope.launch {
        try {
            val post = allPosts.value?.find { it.id == id || it.idLocal == id } ?: return@launch
            if (post.isSynced) {
                repository.likeById(post.id)
            } else {
                localRepository.save(post.copy(likedByMe = !post.likedByMe))
            }
        } catch (e: Exception) {
            Log.e("PostViewModel", "Like error", e)
            _dataState.postValue(FeedModelState(error = true))
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        try {
            val post = allPosts.value?.find { it.id == id || it.idLocal == id } ?: return@launch
            if (post.isSynced) {
                repository.removeById(post.id)
            } else {
                localRepository.removeById(post.idLocal)
            }
        } catch (e: Exception) {
            Log.e("PostViewModel", "Delete error", e)
            _dataState.postValue(FeedModelState(error = true))
        }
    }

    fun saveDraft(content: String) {
        draftContent = content
    }

    fun getDraft(): String? = draftContent

    fun startEditing(post: Post) {
        _edited.value = post
    }

    fun cancelEditing() {
        _edited.value = emptyPost
    }

    fun shareById(id: Long) = Unit
    fun viewById(id: Long) = Unit
}
