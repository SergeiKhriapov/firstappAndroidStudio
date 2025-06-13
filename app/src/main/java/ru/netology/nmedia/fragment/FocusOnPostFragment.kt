package ru.netology.nmedia.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentFocusOnPostBinding
import ru.netology.nmedia.viewmodel.PostViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class FocusOnPostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFocusOnPostBinding.inflate(inflater, container, false)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collect { feedModel ->
                    val posts = feedModel.posts
                    val currentPost =
                        posts.find { it.id == arguments?.getLong("idFocusPost") } ?: return@collect

                    binding.content.text = currentPost.content
                    binding.author.text = currentPost.author

                    val formattedDate = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(currentPost.published * 1000)
                    binding.published.text = formattedDate

                    binding.like.text = currentPost.likes.toString()
                    binding.share.text = currentPost.shareCount.toString()
                    binding.views.text = currentPost.viewsCount.toString()
                    binding.like.isChecked = currentPost.likedByMe

                    val url = "http://10.0.2.2:9999/avatars/${currentPost.authorAvatar}"
                    Glide.with(binding.avatar)
                        .load(url)
                        .placeholder(R.drawable.hourglass_24_ic)
                        .error(R.drawable.ic_launcher_foreground)
                        .timeout(10_000)
                        .circleCrop()
                        .into(binding.avatar)

                    if (currentPost.attachment != null) {
                        binding.attachmentContainer.visibility = View.VISIBLE
                        val imageUrl = "http://10.0.2.2:9999/media/${currentPost.attachment.url}"
                        Glide.with(binding.attachmentContainer)
                            .load(imageUrl)
                            .placeholder(R.drawable.hourglass_24_ic)
                            .error { binding.attachmentContainer.isGone = true }
                            .timeout(10_000)
                            .into(binding.attachmentContainer)
                    } else {
                        binding.attachmentContainer.visibility = View.GONE
                    }

                    if (!currentPost.video.isNullOrEmpty()) {
                        binding.videoPreviewImage.visibility = View.VISIBLE
                    } else {
                        binding.videoPreviewImage.visibility = View.GONE
                    }
                    binding.videoPreviewImage.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentPost.video))
                        startActivity(intent)
                    }
                    binding.like.setOnClickListener {
                        viewModel.likeById(currentPost.id)
                    }

                    binding.share.setOnClickListener {
                        viewModel.shareById(currentPost.id)
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, currentPost.content)
                            type = "text/plain"
                        }
                        val shareIntent =
                            Intent.createChooser(intent, getString(R.string.chooser_share_post))
                        startActivity(shareIntent)
                    }

                    binding.views.setOnClickListener {
                        viewModel.viewById(currentPost.id)
                    }

                    binding.menu.isVisible = currentPost.ownedByMe

                    if (currentPost.ownedByMe) {
                        binding.menu.setOnClickListener {
                            PopupMenu(it.context, it).apply {
                                inflate(R.menu.menu_options)
                                setOnMenuItemClickListener { menuItem ->
                                    when (menuItem.itemId) {
                                        R.id.remove -> {
                                            viewModel.removeById(currentPost.id)
                                            findNavController().navigateUp()
                                            true
                                        }
                                        R.id.edit -> {
                                            viewModel.startEditing(currentPost)
                                            findNavController().navigate(
                                                R.id.action_focusOnPostFragment_to_editPostFragment
                                            )
                                            true
                                        }
                                        else -> false
                                    }
                                }
                            }.show()
                        }
                    } else {
                        binding.menu.setOnClickListener(null)
                    }
                }
            }
        }

        binding.back.setOnClickListener {
            viewModel.cancelEditing()
            findNavController().navigateUp()
        }

        return binding.root
    }
}
