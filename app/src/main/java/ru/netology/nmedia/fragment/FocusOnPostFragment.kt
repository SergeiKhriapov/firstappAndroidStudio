package ru.netology.nmedia.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.liveData
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentFocusOnPostBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class FocusOnPostFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentFocusOnPostBinding.inflate(inflater, container, false)
        /*viewModel.data.observe(viewLifecycleOwner) { posts ->
            val post = posts.find { it.id == viewModel.edited.value?.id }*/
        viewModel.data.observe(viewLifecycleOwner) { posts ->
            val currentPost =
                posts.find { it.id == arguments?.getLong("idFocusPost") } ?: return@observe

            currentPost?.let { currentPost ->
                binding.content.text = currentPost.content
                binding.author.text = currentPost.author
                binding.published.text = currentPost.published
                binding.like.text = currentPost.likeCount.toString()
                binding.share.text = currentPost.shareCount.toString()
                binding.views.text = currentPost.viewsCount.toString()
                binding.like.isChecked = currentPost.likeByMe

                if (!currentPost.video.isNullOrEmpty()) {
                    binding.videoPreviewImage.visibility = View.VISIBLE
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
                                    findNavController().navigate(R.id.action_focusOnPostFragment_to_editPostFragment)
                                    true
                                }

                                else -> false
                            }
                        }
                    }.show()
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
