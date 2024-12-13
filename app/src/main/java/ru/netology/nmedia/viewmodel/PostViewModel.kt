package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositorySQLiteImpl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val empty = Post()

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PostRepository = PostRepositorySQLiteImpl(
        AppDb.getInstance(application).postDao
    )

    val data = repository.getAll()

    private val _edited = MutableLiveData(empty)

    val edited: LiveData<Post?>
        get() = _edited

    private var draftContent: String? = null

    fun saveDraft(content: String) {
        draftContent = content
    }

    fun getDraft(): String? = draftContent

    fun startEditing(post: Post) {
        /* _edited.value = post*/
        _edited.value = if (!draftContent.isNullOrBlank()) {
            post.copy(content = draftContent!!)
        } else {
            post
        }
    }

    fun cancelEditing() {
        _edited.value = empty
    }

    fun saveContent(content: String) {
        _edited.value?.let {
            repository.save(
                it.copy(
                    content = content,
                    published = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date()
                    )
                )
            )
        }
        cancelEditing()
        draftContent = null
    }

    fun likeById(id: Long) = repository.likeById(id)
    fun shareById(id: Long) = repository.shareById(id)
    fun viewById(id: Long) = repository.viewById(id)
    fun removeById(id: Long) = repository.removeById(id)
}
