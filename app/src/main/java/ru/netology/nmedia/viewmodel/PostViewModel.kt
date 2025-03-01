package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException
import kotlin.concurrent.thread

private val emptyPost = Post(
    id = 0,
    author = "",
    content = "",
    published = 0,
    likedByMe = false,
    likes = 0,
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositoryImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel> = _data
    private val _edited = MutableLiveData<Post?>(emptyPost)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit> get() = _postCreated

    private var draftContent: String? = null

    init {
        loadPosts()
    }

    fun loadPosts() {
        thread {
            _data.postValue(FeedModel(loading = true))
            try {
                val posts = repository.getAll()
                val updatedPosts = posts.map {
                    it.copy(published = it.published * 1000)
                }
                _data.postValue(
                    FeedModel(posts = updatedPosts, empty = posts.isEmpty())
                )
            } catch (e: IOException) {
                _data.postValue(FeedModel(error = true))
            }
        }
    }

    val edited: LiveData<Post?> get() = _edited

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

    fun saveContent(content: String) {
        edited.value?.let {
            thread {
                val currentTime = System.currentTimeMillis()
                repository.save(
                    it.copy(content = content, published = currentTime)
                )
                _postCreated.postValue(Unit)
            }
        }
        _edited.value = emptyPost
    }

    fun likeById(id: Long) {
        val post = _data.value?.posts?.find { it.id == id } ?: return
        val updatedPost = post.copy(
            likedByMe = !post.likedByMe,
            likes = if (post.likedByMe) post.likes - 1 else post.likes + 1
        )
        _data.value?.posts?.map { if (it.id == id) updatedPost else it }?.let {
            _data.postValue(_data.value?.copy(posts = it))
        }
        thread {
            try {
                val postAfterLike = repository.likeById(post)
                _data.value?.posts?.let { posts ->
                    val updatedPosts = posts.map {
                        if (it.id == id) {
                            postAfterLike.copy(published = it.published)
                        } else it
                    }
                    _data.postValue(_data.value?.copy(posts = updatedPosts))
                }
            } catch (e: IOException) {
                _data.value?.posts?.let { posts ->
                    val updatedPosts = posts.map {
                        if (it.id == id) post else it
                    }
                    _data.postValue(_data.value?.copy(posts = updatedPosts))
                }
            }
        }
    }

    fun shareById(id: Long) = repository.shareById(id)
    fun viewById(id: Long) = repository.viewById(id)

    fun removeById(id: Long) {
        thread {
            val oldPosts = _data.value?.posts.orEmpty()
            _data.postValue(
                _data.value?.copy(
                    posts = _data.value?.posts.orEmpty().filter { it.id != id })
            )
            try {
                repository.removeById(id)
            } catch (e: Exception) {
                _data.postValue(_data.value?.copy(posts = oldPosts))
            }
        }
    }
}
