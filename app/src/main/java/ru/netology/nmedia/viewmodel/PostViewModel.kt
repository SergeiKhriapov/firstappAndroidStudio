package ru.netology.nmedia.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.di.DependencyContainer
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.FileRepository
import ru.netology.nmedia.repository.LocalPostRepositoryImpl
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File

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

class PostViewModel(
    private val appAuth: AppAuth,
    private val repository: PostRepository,
    private val localRepository: LocalPostRepositoryImpl,
    private val fileRepository: FileRepository
) : ViewModel() {

    /*private val dependencies = DependencyContainer.getInstance()
    private val appAuth = dependencies.appAuth
    private val repository: PostRepository = dependencies.repository
    private val localRepository: LocalPostRepositoryImpl = dependencies.localRepository*/

    val isAuthenticated: StateFlow<Boolean> = appAuth.data
        .map { it?.id != null && it.id != 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    private val _photo = MutableLiveData<PhotoModel?>(null)
    val photo: LiveData<PhotoModel?> get() = _photo

    private var draftContent: String? = null

    val hiddenSyncedCount: LiveData<Int> = repository.getHiddenSyncedCount()
        .asLiveData(Dispatchers.Default)

    private val allPosts: StateFlow<List<Post>> = combine(
        repository.data,
        localRepository.data
    ) { synced, unsynced ->
        val filteredSynced = synced.filter { it.isSynced }
        val filteredUnsynced = unsynced.filter { !it.isSynced }
        (filteredSynced + filteredUnsynced).sortedByDescending { it.published }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val data: Flow<FeedModel> = appAuth.data
        .flatMapLatest { token ->
            allPosts
                .map { posts ->
                    FeedModel(
                        posts.filter { !it.hidden }
                            .map { post -> post.copy(ownedByMe = token?.id == post.authorId) }
                    )
                }
                .catch { e -> Log.e("PostViewModel", "Error in data flow", e) }
        }

    val newerCount: Flow<Int> = data.flatMapLatest {
        repository.getNewer(it.posts.firstOrNull()?.id ?: 0)
            .catch { _dataState.postValue(FeedModelState(error = true)) }
    }

    val shouldShowAuthDialog = SingleLiveEvent<Unit>()

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
        _syncError.value = hasError
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
                        attachment = Attachment(
                            url = savedFile.absolutePath,
                            type = AttachmentType.IMAGE
                        )
                    }

                    val updatedPost = post.copy(
                        content = content,
                        published = System.currentTimeMillis(),
                        isSynced = false,
                        attachment = attachment
                    )

                    if (post.isSynced) {
                        file?.let {
                            repository.save(updatedPost.copy(isSynced = true), it)
                        } ?: repository.save(updatedPost)

                        localRepository.update(updatedPost.copy(isSynced = true))
                    } else if (post.idLocal != 0L) {
                        localRepository.update(updatedPost)
                        try {
                            file?.let {
                                repository.save(updatedPost.copy(isSynced = true), it)
                            } ?: repository.save(updatedPost)
                            localRepository.removeById(updatedPost.idLocal)
                        } catch (e: Exception) {
                            Log.e("PostViewModel", "Error syncing local post", e)
                        }
                    } else {
                        val localId = localRepository.save(updatedPost)
                        val savedPost = updatedPost.copy(idLocal = localId)

                        try {
                            file?.let {
                                repository.save(savedPost.copy(isSynced = true), it)
                            } ?: repository.save(savedPost.copy(isSynced = true))
                            localRepository.removeById(savedPost.idLocal)
                        } catch (e: Exception) {
                            Log.e("PostViewModel", "Error syncing new post", e)
                        }
                    }

                    _postCreated.value = Unit
                    _edited.value = emptyPost
                    _photo.value = null

                } catch (e: Exception) {
                    Log.e("PostViewModel", "Error saving content", e)
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
    }

    private fun saveToInternalStorage(file: File): File =
        fileRepository.saveToInternalStorage(file)


    fun likeById(id: Long) = viewModelScope.launch {
        if (!isAuthenticated.value) {
            shouldShowAuthDialog.postValue(Unit)
            return@launch
        }

        try {
            val post =
                repository.data.first().find { it.id == id || it.idLocal == id } ?: return@launch
            if (post.isSynced) {
                if (post.likedByMe) {
                    repository.dislikeById(post.id)
                } else {
                    repository.likeById(post.id)
                }
            } else {
                _likeError.postValue(Unit)
            }
        } catch (e: Exception) {
            Log.e("PostViewModel", "Error liking post", e)
            _likeError.postValue(Unit)
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        try {
            val post = allPosts.value.find { it.id == id || it.idLocal == id } ?: return@launch
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

    fun unhideAllSyncedPosts() = viewModelScope.launch {
        repository.unhideAllSyncedPosts()
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