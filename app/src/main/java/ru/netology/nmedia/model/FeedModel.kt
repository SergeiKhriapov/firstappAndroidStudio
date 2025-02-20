package ru.netology.nmedia.model

import com.google.android.gms.dynamite.DynamiteModule.LoadingException
import ru.netology.nmedia.dto.Post

data class FeedModel(
    val posts: List<Post> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
    val empty: Boolean = false,
)