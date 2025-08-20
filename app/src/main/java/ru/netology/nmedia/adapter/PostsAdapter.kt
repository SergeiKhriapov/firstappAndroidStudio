package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardAdBinding
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.databinding.CardSeparatorBinding
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.Separator
import ru.netology.nmedia.util.formatCount
import java.text.SimpleDateFormat
import java.util.*

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(PostDiffCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is Post -> R.layout.card_post
            is Ad -> R.layout.card_ad
            is Separator -> R.layout.card_separator
            null -> error("Неизвестный тип элемента")
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.card_post -> {
                val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding, onInteractionListener)
            }
            R.layout.card_ad -> {
                val binding = CardAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(binding)
            }
            R.layout.card_separator -> {
                val binding = CardSeparatorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SeparatorViewHolder(binding)
            }
            else -> error("Неизвестный тип элемента: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Post -> (holder as? PostViewHolder)?.bind(item)
            is Ad -> (holder as? AdViewHolder)?.bind(item)
            is Separator -> (holder as? SeparatorViewHolder)?.bind(item)
            null -> error("Неизвестный тип элемента")
        }
    }

    class PostViewHolder(
        private val binding: CardPostBinding,
        private val onInteractionListener: OnInteractionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) = with(binding) {
            content.text = post.content
            author.text = post.author

            val date = post.published * 1000
            published.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)

            like.isChecked = post.likedByMe
            like.text = formatCount(post.likes)
            share.text = formatCount(post.shareCount)
            views.text = formatCount(post.viewsCount)
            menu.isVisible = post.ownedByMe

            // Аватарка
            val avatarUrl = "http://10.0.2.2:9999/avatars/${post.authorAvatar}"
            Glide.with(avatar)
                .load(avatarUrl)
                .placeholder(R.drawable.hourglass_24_ic)
                .error(R.drawable.ic_launcher_foreground)
                .timeout(10_000)
                .circleCrop()
                .into(avatar)

            // Фото вложение
            if (post.attachment?.url != null) {
                attachmentContainer.visibility = View.VISIBLE
                val imageUrl = "http://10.0.2.2:9999/media/${post.attachment.url}"
                Glide.with(attachmentContainer)
                    .load(imageUrl)
                    .placeholder(R.drawable.hourglass_24_ic)
                    .error(R.drawable.error_ic)
                    .timeout(10_000)
                    .into(attachmentContainer)
            } else {
                attachmentContainer.visibility = View.GONE
            }

            videoPreviewImage.visibility = if (!post.video.isNullOrEmpty()) View.VISIBLE else View.GONE
            videoPreviewImage.setOnClickListener { onInteractionListener.onVideoClick(post) }

            like.setOnClickListener { onInteractionListener.onLike(post) }
            share.setOnClickListener { onInteractionListener.onShare(post) }
            views.setOnClickListener { onInteractionListener.onView(post) }
            content.setOnClickListener { onInteractionListener.focusOnPost(post) }
            attachmentContainer.setOnClickListener { onInteractionListener.focusOnAttachment(post) }

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.menu_options)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.remove -> { onInteractionListener.onRemove(post); true }
                            R.id.edit -> { onInteractionListener.onEdit(post); true }
                            else -> false
                        }
                    }
                }.show()
            }
        }
    }

    class AdViewHolder(private val binding: CardAdBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ad: Ad) {
            val imageUrl = "http://10.0.2.2:9999/media/${ad.image}"
            Glide.with(binding.image)
                .load(imageUrl)
                .placeholder(R.drawable.hourglass_24_ic)
                .error(R.drawable.error_ic)
                .timeout(10_000)
                .into(binding.image)
        }
    }

    class SeparatorViewHolder(private val binding: CardSeparatorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(separator: Separator) {
            binding.title.text = separator.title
        }
    }
}

object PostDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean =
        oldItem::class == newItem::class && oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean = oldItem == newItem
}
