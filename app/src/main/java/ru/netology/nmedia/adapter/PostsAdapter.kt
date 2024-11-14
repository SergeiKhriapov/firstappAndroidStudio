package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet.Layout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.callbackFlow
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.formatting.formatCount

class PostsAdapter(
    private val onLike: (Post) -> Unit,
    private val onShare: (Post) -> Unit,
    private val onView: (Post) -> Unit
) : RecyclerView.Adapter<PostViewHolder>() {
    var list = emptyList<Post>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(
            CardPostBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onLike,
            onShare,
            onView
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onLike: (Post) -> Unit,
    private val onShare: (Post) -> Unit,
    private val onView: (Post) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) = with(binding) {
        content.text = post.content
        author.text = post.author
        published.text = post.published
        like.setImageResource(
            if (post.likeByMe) R.drawable.ic_baseline_favorite_24 else R.drawable.ic_baseline_favorite_border_24
        )
        likeCount.text = formatCount(post.likeCount)
        shareCount.text = formatCount(post.shareCount)
        viewsCount.text = formatCount(post.viewsCount)

        like.setOnClickListener {
            onLike(post)
        }
        share.setOnClickListener {
            onShare(post)
        }
        views.setOnClickListener {
            onView(post)
        }
    }
}