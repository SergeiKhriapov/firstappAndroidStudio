package ru.netology.nmedia.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.file.FileRepository
import ru.netology.nmedia.repository.post.PostRepository
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject

private val emptyPost = Post(
    id = 0,
    idLocal = 0,
    author = "",
    authorId = 0,
    authorAvatar = "",
    content = "",
    published = 0,
    likedByMe = false,
    likes = 0,
)

@HiltViewModel
class PostViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val repository: PostRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState> = _dataState

    val likeError = SingleLiveEvent<Unit>()
    val postCreated = SingleLiveEvent<Unit>()
    val shouldShowAuthDialog = SingleLiveEvent<Unit>()

    private val _edited = MutableLiveData<Post?>(emptyPost)
    val edited: LiveData<Post?> = _edited

    private val _photo = MutableLiveData<PhotoModel?>(null)
    val photo: LiveData<PhotoModel?> = _photo

    val isAuthenticated: StateFlow<Boolean> = appAuth.data
        .map { it?.id != null && it.id != 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val data: Flow<PagingData<Post>> = appAuth.data
        .flatMapLatest { auth ->
            val currentUserId = auth?.id
            repository.data.map { pagingData ->
                pagingData.map { post ->
                    post.copy(ownedByMe = currentUserId == post.authorId)
                }
            }
        }
        .cachedIn(viewModelScope)


    val newerCount: Flow<Int> = repository.getNewer(0)
        .catch { _dataState.postValue(FeedModelState(error = true)) }

    fun changePhoto(uri: Uri, file: File) {
        _photo.value = PhotoModel(uri, file)
    }

    fun removePhoto() {
        _photo.value = null
    }

    init {
        viewModelScope.launch {
            appAuth.data.collect {
                loadPosts()
            }
        }
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAll()
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
                try {
                    val photo = _photo.value
                    val file = photo?.file
                    var attachment: Attachment? = null

                    file?.let {
                        val savedFile = saveToInternalStorage(it)
                        attachment = Attachment(savedFile.absolutePath, AttachmentType.IMAGE)
                    }

                    val updatedPost = post.copy(
                        content = content,
                        published = System.currentTimeMillis(),
                        attachment = attachment
                    )

                    if (file != null) {
                        repository.save(updatedPost, file)
                    } else {
                        repository.save(updatedPost)
                    }

                    postCreated.value = Unit
                    _edited.value = emptyPost
                    _photo.value = null
                } catch (e: Exception) {
                    Log.e("PostViewModel", "Error saving", e)
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
    }

    fun likeById(id: Long) = viewModelScope.launch {
        if (!isAuthenticated.value) {
            shouldShowAuthDialog.postValue(Unit)
            return@launch
        }

        try {
            repository.likeById(id)
        } catch (e: Exception) {
            Log.e("PostViewModel", "Like error", e)
            likeError.postValue(Unit)
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        try {
            repository.removeById(id)
        } catch (e: Exception) {
            Log.e("PostViewModel", "Remove error", e)
            _dataState.postValue(FeedModelState(error = true))
        }
    }

    /*fun unhideAllSyncedPosts() = viewModelScope.launch {
        repository.unhideAllSyncedPosts()
    }*/

    private fun saveToInternalStorage(file: File): File =
        fileRepository.saveToInternalStorage(file)

    fun startEditing(post: Post) {
        _edited.value = post
    }

    fun cancelEditing() {
        _edited.value = emptyPost
    }

    fun shareById(id: Long) = Unit
    fun viewById(id: Long) = Unit
}
