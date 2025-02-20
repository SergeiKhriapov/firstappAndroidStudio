package ru.netology.nmedia.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent
import kotlin.concurrent.thread

private val empty = Post()

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositoryImpl()

    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel> = _data
    private val _edited = MutableLiveData(empty)

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit> = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() {
        thread {
            _data.postValue(FeedModel(loading = true))
            val newState = try {
                val posts = repository.getAll()
                FeedModel(posts = posts, empty = posts.isEmpty())
            } catch (e: Exception) {
                FeedModel(error = true)
            }
            _data.postValue(newState)
        }
    }

    val edited: LiveData<Post?>
        get() = _edited

    private var draftContent: String? = null

    fun saveDraft(content: String) {
        draftContent = content
    }

    fun getDraft(): String? {
        return draftContent
    }

    fun startEditing(post: Post) {
        _edited.value = post
    }

    fun cancelEditing() {
        _edited.postValue(empty)
    }

    fun saveContent(content: String) {
        thread {
            _edited.value?.let {
                val currentTime = System.currentTimeMillis()
                repository.save(
                    it.copy(
                        content = content,
                        published = currentTime
                    )
                )
                _postCreated.postValue(Unit)
                loadPosts()
            }
            cancelEditing()
            draftContent = null
        }
    }

    fun likeById(id: Long) = repository.likeById(id)
    fun shareById(id: Long) = repository.shareById(id)
    fun viewById(id: Long) = repository.viewById(id)

    fun removeById(id: Long) {
        thread {
            val old = _data.value?.posts.orEmpty()
            _data.postValue(
                _data.value?.copy(posts = _data.value?.posts.orEmpty()
                    .filter { it.id != id }
                )
            )
            try {
                repository.removeById(id)
            } catch (_: Exception) {
                _data.postValue(_data.value?.copy(posts = old))
            }
        }
    }
}
