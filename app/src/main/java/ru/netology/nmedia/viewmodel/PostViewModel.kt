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
import kotlin.concurrent.thread

private val empty = Post(
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
    private val _edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() {
        _data.value = FeedModel(loading = true)
        repository.getAllAsync(object : PostRepository.GetAllCallback {
            override fun onSuccess(posts: List<Post>) {
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModel(error = true))
            }
        })
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
        edited.value?.let {
            thread {
                val currentTime = System.currentTimeMillis()
                repository.save(
                    it.copy(
                        content = content,
                        published = currentTime
                    )
                )
                _postCreated.postValue(Unit)
            }
        }
        _edited.value = empty
    }

    fun likeById(id: Long) {
        val posts = _data.value?.posts.orEmpty()
        val post = posts.find { it.id == id } ?: return
        val likedNow = !post.likedByMe
        val updatedPost = post.copy(
            likedByMe = likedNow,
            likes = if (likedNow) post.likes + 1 else post.likes - 1
        )

        _data.postValue(_data.value?.copy(posts = _data.value?.posts?.map { currentPost ->
            if (currentPost.id == id) updatedPost else currentPost
        } ?: emptyList()))

        repository.likeById(id, object : PostRepository.LikeCallback {
            override fun onSuccess(serverPost: Post) {

                _data.postValue(_data.value?.copy(posts = _data.value?.posts?.map { currentPost ->
                    if (currentPost.id == id) serverPost else currentPost
                } ?: emptyList()))
            }

            override fun onError(e: Exception) {

                _data.postValue(_data.value?.copy(posts = posts))
            }
        })
    }

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
