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
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.formatCount
import java.text.SimpleDateFormat
import java.util.*

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener
) : PagingDataAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {

        // В PagingDataAdapter нужно обрабатывать null-объекты
        getItem(position)?.let { holder.bind(it) }
    }

    class PostViewHolder(
        private val binding: CardPostBinding,
        private val onInteractionListener: OnInteractionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) = with(binding) {
            content.text = post.content
            author.text = post.author

            val date = post.published * 1000
            published.text =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)

            like.isChecked = post.likedByMe
            like.text = formatCount(post.likes)
            share.text = formatCount(post.shareCount)
            views.text = formatCount(post.viewsCount)

            menu.isVisible = post.ownedByMe

            // Загрузка аватарки
            val avatarUrl = "http://10.0.2.2:9999/avatars/${post.authorAvatar}"
            Glide.with(avatar)
                .load(avatarUrl)
                .placeholder(R.drawable.hourglass_24_ic)
                .error(R.drawable.ic_launcher_foreground)
                .timeout(10_000)
                .circleCrop()
                .into(avatar)

            // Обработка вложения (фото)
            if (post.attachment?.url != null) {
                attachmentContainer.visibility = View.VISIBLE
                /*
                                val imageUrl = post.attachment.url
                */
                val imageUrl =
                    "http://10.0.2.2:9999/media/${post.attachment.url}" // post.attachment.url хранит только имя файла)


                Glide.with(attachmentContainer)
                    .load(imageUrl)
                    .placeholder(R.drawable.hourglass_24_ic)
                    .error(R.drawable.error_ic)
                    .timeout(10_000)
                    .into(attachmentContainer)
            } else {
                attachmentContainer.visibility = View.GONE
            }

            videoPreviewImage.visibility =
                if (!post.video.isNullOrEmpty()) View.VISIBLE else View.GONE
            videoPreviewImage.setOnClickListener {
                onInteractionListener.onVideoClick(post)
            }

            like.setOnClickListener { onInteractionListener.onLike(post) }
            share.setOnClickListener { onInteractionListener.onShare(post) }
            views.setOnClickListener { onInteractionListener.onView(post) }
            content.setOnClickListener { onInteractionListener.focusOnPost(post) }
            attachmentContainer.setOnClickListener { onInteractionListener.focusOnAttachment(post) }

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.menu_options)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }

                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }
        }
    }
}

object PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean =
        oldItem == newItem
}