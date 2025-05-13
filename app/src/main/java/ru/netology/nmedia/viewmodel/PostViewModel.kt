package ru.netology.nmedia.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.LocalPostRepositoryImpl
import ru.netology.nmedia.repository.MediaRepository
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File

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

class PostViewModel(application: Application) :
    AndroidViewModel(application) {
    private val database = AppDb.getInstance(application)
    private val mediaRepository = MediaRepository()
    private val repository: PostRepository =
        PostRepositoryImpl(database.postDao, mediaRepository)

    private val localRepository: LocalPostRepositoryImpl =
        LocalPostRepositoryImpl(database.localPostDao, mediaRepository)

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

    val hiddenSyncedCount: LiveData<Int> = repository.getHiddenSyncedCount()
        .asLiveData(Dispatchers.Default)

    private val _photo = MutableLiveData<PhotoModel?>(null)
    val photo: LiveData<PhotoModel?>
        get() = _photo


    private var draftContent: String? = null

    private val allPosts: StateFlow<List<Post>> = combine(
        repository.data,
        localRepository.data
    ) { synced, unsynced ->
        val filteredSynced = synced.filter { it.isSynced }
        val filteredUnsynced = unsynced.filter { !it.isSynced }
        (filteredSynced + filteredUnsynced).sortedByDescending { it.published }
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    val data: LiveData<FeedModel> = allPosts
        .map { posts ->
            val visiblePosts = posts.filter { !it.hidden }
            FeedModel(visiblePosts)
        }
        .catch { it.printStackTrace() }
        .asLiveData(Dispatchers.Default)


    val newerCount = data.switchMap {
        repository.getNewer(it.posts.firstOrNull()?.id ?: 0)
            .catch { _dataState.postValue(FeedModelState(error(true))) }
            .asLiveData(Dispatchers.Default)
    }

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
                        } ?: repository.save(post)

                        localRepository.update(updatedPost.copy(isSynced = true))
                    } else if (post.idLocal != 0L) {
                        localRepository.update(updatedPost)
                        try {
                            file?.let {
                                repository.save(updatedPost.copy(isSynced = true), it)
                            } ?: repository.save(post)
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
                            } ?: repository.save(post)
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

    private fun saveToInternalStorage(file: File): File {
        val context = getApplication<Application>().applicationContext
        val internalDir = context.filesDir
        val internalFile = File(internalDir, file.name)
        file.copyTo(internalFile, overwrite = true)
        return internalFile
    }

    fun likeById(id: Long) = viewModelScope.launch {
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

    fun unhideAllSyncedPosts() {
        viewModelScope.launch {
            repository.unhideAllSyncedPosts()
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