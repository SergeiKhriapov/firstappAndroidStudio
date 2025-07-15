package ru.netology.nmedia.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.file.FileRepository
import ru.netology.nmedia.repository.post.LocalPostRepositoryImpl
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
    private val localRepository: LocalPostRepositoryImpl,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val emptyPost = Post(
        id = 0, idLocal = 0, author = "", authorId = 0,
        authorAvatar = "", content = "", published = 0,
        likedByMe = false, likes = 0
    )

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState> = _dataState

    val syncError = MutableLiveData<Boolean>()
    val likeError = SingleLiveEvent<Unit>()
    val postCreated = SingleLiveEvent<Unit>()
    val shouldShowAuthDialog = SingleLiveEvent<Unit>()

    private val _edited = MutableLiveData<Post?>(emptyPost)
    val edited: LiveData<Post?> = _edited

    private val _photo = MutableLiveData<PhotoModel?>(null)
    val photo: LiveData<PhotoModel?> = _photo

    private var draftContent: String? = null

    val hiddenSyncedCount: LiveData<Int> =
        repository.getHiddenSyncedCount().asLiveData(Dispatchers.Default)

    val isAuthenticated: StateFlow<Boolean> = appAuth.data
        .map { it?.id != null && it.id != 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Основной поток PagingData с объединением локальных unsynced постов
    val data: Flow<PagingData<Post>> = merge(
        localRepository.data.map { unsyncedPosts ->
            PagingData.from(
                unsyncedPosts.filter { !it.isSynced }
                    .sortedByDescending { it.published }
            )
        },
        repository.data
    )
        .map { pagingData ->
            val currentUserId = appAuth.data.value?.id
            pagingData.map { post -> post.copy(ownedByMe = currentUserId == post.authorId) }
        }
        .cachedIn(viewModelScope)

    val newerCount: Flow<Int> = repository.getNewer(0)
        .catch { _dataState.postValue(FeedModelState(error = true)) }

    init {
        loadPosts()
    }

    fun changePhoto(uri: Uri, file: File) {
        _photo.value = PhotoModel(uri, file)
    }

    fun removePhoto() {
        _photo.value = null
    }

    fun loadPosts() = viewModelScope.launch {
        syncPosts()
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
        syncError.value = hasError
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
                        isSynced = false,
                        attachment = attachment
                    )

                    when {
                        post.isSynced -> {
                            file?.let {
                                repository.save(updatedPost.copy(isSynced = true), it)
                            } ?: repository.save(updatedPost)
                            localRepository.update(updatedPost.copy(isSynced = true))
                        }

                        post.idLocal != 0L -> {
                            localRepository.update(updatedPost)
                            try {
                                file?.let {
                                    repository.save(updatedPost.copy(isSynced = true), it)
                                } ?: repository.save(updatedPost)
                                localRepository.removeById(updatedPost.idLocal)
                            } catch (_: Exception) {}
                        }

                        else -> {
                            val idLocal = localRepository.save(updatedPost)
                            val newPost = updatedPost.copy(idLocal = idLocal)
                            try {
                                file?.let {
                                    repository.save(newPost.copy(isSynced = true), it)
                                } ?: repository.save(newPost.copy(isSynced = true))
                                localRepository.removeById(newPost.idLocal)
                            } catch (_: Exception) {}
                        }
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

    fun unhideAllSyncedPosts() = viewModelScope.launch {
        repository.unhideAllSyncedPosts()
    }

    private fun saveToInternalStorage(file: File): File =
        fileRepository.saveToInternalStorage(file)

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
