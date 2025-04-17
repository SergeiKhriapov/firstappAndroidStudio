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
import kotlin.math.log

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
    private val localRepository: LocalPostRepositoryImpl =
        LocalPostRepositoryImpl(database.localPostDao)

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState> get() = _dataState

    private val _syncError = MutableLiveData<Boolean>()
    val syncError: LiveData<Boolean> get() = _syncError

    private val _likeError = SingleLiveEvent<Unit>()
    val likeError: LiveData<Unit> get() = _likeError

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
                .sortedByDescending { it.published }
        }



        addSource(repository.data) { newSynced ->
            synced = newSynced.filter { it.isSynced } // синхронизированные
            combine()
        }

        addSource(localRepository.data) { newUnsynced ->
            unsynced = newUnsynced.filter { !it.isSynced } // локальные
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
        syncPosts()
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
        syncPosts()
        try {
            _dataState.value = FeedModelState(refreshing = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            Log.e("PostViewModel", "Error refreshing posts", e)
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun syncPosts() = viewModelScope.launch {
        val hasError = localRepository.syncUnsyncedPosts()
        _syncError.value = hasError
    }

    fun saveContent(content: String) {
        edited.value?.let { post ->
            viewModelScope.launch {
                val updatedPost = post.copy(
                    content = content,
                    published = System.currentTimeMillis(),
                    isSynced = false
                )
                try {
                    if (post.isSynced) {
                       repository.save(updatedPost.copy(isSynced = true))
                        localRepository.update(updatedPost.copy(isSynced = true))
                    } else if (post.idLocal != 0L) {
                        localRepository.update(updatedPost)
                        try {
                            repository.save(updatedPost.copy(isSynced = true))
                            localRepository.removeById(updatedPost.idLocal)
                        } catch (e: Exception) {
                            Log.e("PostViewModel", "Error syncing local post", e)
                        }
                    } else {
                        val localId = localRepository.save(updatedPost)
                        val savedPost = updatedPost.copy(idLocal = localId)

                        try {
                            repository.save(savedPost.copy(isSynced = true))
                            localRepository.removeById(savedPost.idLocal)
                        } catch (e: Exception) {
                            Log.e("PostViewModel", "Error syncing new post", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PostViewModel", "Error saving content", e)
                }
                _postCreated.value = Unit
                _edited.value = emptyPost
            }
        }
    }




    fun likeById(id: Long) = viewModelScope.launch {
        try {
            val post = allPosts.value?.find { it.id == id || it.idLocal == id } ?: return@launch

            if (post.isSynced) {
                if (post.likedByMe) {
                    repository.dislikeById(post.id)
                } else {
                    repository.likeById(post.id)
                }
            } else {
                _likeError.postValue(Unit)
                return@launch
            }
        } catch (e: Exception) {
            Log.e("PostViewModel", "Error liking or disliking post", e)
            _likeError.postValue(Unit)
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
