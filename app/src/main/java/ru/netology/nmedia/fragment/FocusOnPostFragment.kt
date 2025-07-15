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
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentFocusOnPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class FocusOnPostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    private var _binding: FragmentFocusOnPostBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFocusOnPostBinding.inflate(inflater, container, false)

        val post = arguments?.getParcelable<Post>("post")

        if (post == null) {
            // Если пост не передан — просто уйдем назад или покажем ошибку
            findNavController().navigateUp()
            return binding.root
        }

        // Заполняем UI сразу, без подписок
        with(binding) {
            content.text = post.content
            author.text = post.author

            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(post.published * 1000)
            published.text = formattedDate

            like.text = post.likes.toString()
            share.text = post.shareCount.toString()
            views.text = post.viewsCount.toString()
            like.isChecked = post.likedByMe

            val avatarUrl = "http://10.0.2.2:9999/avatars/${post.authorAvatar}"
            Glide.with(avatar)
                .load(avatarUrl)
                .placeholder(R.drawable.hourglass_24_ic)
                .error(R.drawable.ic_launcher_foreground)
                .timeout(10_000)
                .circleCrop()
                .into(avatar)

            if (post.attachment != null) {
                attachmentContainer.visibility = View.VISIBLE
                val imageUrl = "http://10.0.2.2:9999/media/${post.attachment.url}"
                Glide.with(attachmentContainer)
                    .load(imageUrl)
                    .placeholder(R.drawable.hourglass_24_ic)
                    .error { attachmentContainer.isGone = true }
                    .timeout(10_000)
                    .into(attachmentContainer)
            } else {
                attachmentContainer.visibility = View.GONE
            }

            if (!post.video.isNullOrEmpty()) {
                videoPreviewImage.visibility = View.VISIBLE
                videoPreviewImage.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.video))
                    startActivity(intent)
                }
            } else {
                videoPreviewImage.visibility = View.GONE
                videoPreviewImage.setOnClickListener(null)
            }

            like.setOnClickListener {
                viewModel.likeById(post.id)
            }

            share.setOnClickListener {
                viewModel.shareById(post.id)
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }

            views.setOnClickListener {
                viewModel.viewById(post.id)
            }

            menu.isVisible = post.ownedByMe
            if (post.ownedByMe) {
                menu.setOnClickListener { view ->
                    PopupMenu(view.context, view).apply {
                        inflate(R.menu.menu_options)
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.remove -> {
                                    viewModel.removeById(post.id)
                                    findNavController().navigateUp()
                                    true
                                }
                                R.id.edit -> {
                                    viewModel.startEditing(post)
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
                menu.setOnClickListener(null)
            }

            back.setOnClickListener {
                viewModel.cancelEditing()
                findNavController().navigateUp()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
