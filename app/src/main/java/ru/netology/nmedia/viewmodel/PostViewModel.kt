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

private val emptyPost = Post(
    id = 0,
    author = "",
    authorAvatar = "",
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
                repository.getAllAsync(object : PostRepository.AsyncCallback<List<Post>> {
                    override fun onSuccess(result: List<Post>) {
                        _data.postValue(FeedModel(posts = result, empty = result.isEmpty()))
                    }

                    override fun onError(e: Exception) {
                        _data.postValue(FeedModel(error = true))
                    }
                })
            } catch (e: Exception) {
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
            val currentTime = System.currentTimeMillis()
            val updatedPost = it.copy(content = content, published = currentTime)

            repository.save(updatedPost, object : PostRepository.AsyncCallback<Post> {
                override fun onSuccess(result: Post) {
                    _postCreated.postValue(Unit)
                }

                override fun onError(e: Exception) {
                }
            })
        }
        _edited.value = emptyPost
    }


    fun likeById(id: Long) {
        val post = _data.value?.posts?.find { it.id == id } ?: return
        val updatedPost = post.copy(
            likedByMe = !post.likedByMe,
            likes = if (post.likedByMe) post.likes - 1 else post.likes + 1
        )

        _data.value?.posts?.let { posts ->
            val updatedPosts = posts.map {
                if (it.id == id) updatedPost else it
            }
            _data.postValue(_data.value?.copy(posts = updatedPosts))
        }

        repository.likeById(post, object : PostRepository.AsyncCallback<Post> {
            override fun onSuccess(result: Post) {
                _data.value?.posts?.let { posts ->
                    val updatedPosts = posts.map {
                        if (it.id == id) {
                            result.copy(published = it.published)
                        } else it
                    }
                    _data.postValue(_data.value?.copy(posts = updatedPosts))
                }
            }

            override fun onError(e: Exception) {
                _data.value?.posts?.let { posts ->
                    val updatedPosts = posts.map {
                        if (it.id == id) post else it
                    }
                    _data.postValue(_data.value?.copy(posts = updatedPosts))
                }
            }
        })
    }


    fun shareById(id: Long) = repository.shareById(id)
    fun viewById(id: Long) = repository.viewById(id)

    fun removeById(id: Long) {
        val oldPosts = _data.value?.posts.orEmpty()
        _data.postValue(
            _data.value?.copy(
                posts = _data.value?.posts.orEmpty().filter { it.id != id })
        )
        repository.removeById(id, object : PostRepository.AsyncCallback<Unit> {
            override fun onSuccess(result: Unit) {
            }

            override fun onError(e: Exception) {
                _data.postValue(_data.value?.copy(posts = oldPosts))
            }
        })
    }
}