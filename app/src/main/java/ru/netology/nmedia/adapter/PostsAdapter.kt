package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.formatting.formatCount

typealias LikeCallback = (Post) -> Unit
typealias ShareCallback = (Post) -> Unit
typealias ViewCallback = (Post) -> Unit


class PostsAdapter(
    private val onLike: LikeCallback,
    private val onShare: ShareCallback,
    private val onView: ViewCallback
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback) {

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
        holder.bind(getItem(position))
    }

    class PostViewHolder(
        private val binding: CardPostBinding,
        private val onLike: LikeCallback,
        private val onShare: ShareCallback,
        private val onView: ViewCallback
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
}

object PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
}