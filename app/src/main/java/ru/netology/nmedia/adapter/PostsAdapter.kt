package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.formatCount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(
            CardPostBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ),
            onInteractionListener
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostViewHolder(
        private val binding: CardPostBinding,
        private val onInteractionListener: OnInteractionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) = with(binding) {
            content.text = post.content
            author.text = post.author
            val date = if (post.isSynced) Date(post.published * 1000) else Date(post.published)
            val formattedDate =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
            published.text = formattedDate

            like.isChecked = post.likedByMe
            like.setText(formatCount(post.likes))
            share.setText(formatCount(post.shareCount))
            views.setText(formatCount(post.viewsCount))

            // Загрузка аватарки
            val avatarUrl = "http://10.0.2.2:9999/avatars/${post.authorAvatar}"
            Glide.with(binding.avatar)
                .load(avatarUrl)
                .placeholder(R.drawable.hourglass_24_ic)
                .error(R.drawable.ic_launcher_foreground)
                .timeout(10_000)
                .circleCrop()
                .into(binding.avatar)

            // Отображение изображения localPostEntaty и PostEntaty
            if (post.attachment?.url != null) {
                attachmentContainer.visibility = View.VISIBLE
                val imageUrl = if (post.isSynced)
                    "http://10.0.2.2:9999/media/${post.attachment.url}"
                else
                    post.attachment.url

                Glide.with(binding.attachmentContainer)
                    .load(imageUrl)
                    .placeholder(R.drawable.hourglass_24_ic)
                    .error(R.drawable.error_ic)
                    .timeout(10_000)
                    .into(binding.attachmentContainer)
            } else {
                attachmentContainer.visibility = View.GONE
            }


            if (!post.video.isNullOrEmpty()) {
                videoPreviewImage.visibility = View.VISIBLE
                videoPreviewImage.setOnClickListener {
                    onInteractionListener.onVideoClick(post)
                }
            } else {
                videoPreviewImage.visibility = View.GONE
            }
            sync.visibility = if (post.isSynced) View.VISIBLE else View.GONE

            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }
            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }
            views.setOnClickListener {
                onInteractionListener.onView(post)
            }

            content.setOnClickListener {
                onInteractionListener.focusOnPost(post)
            }
            attachmentContainer.setOnClickListener {
                onInteractionListener.focusOnAttachment(post)
            }

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.menu_options)
                    setOnMenuItemClickListener {
                        when (it.itemId) {
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
    override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
}