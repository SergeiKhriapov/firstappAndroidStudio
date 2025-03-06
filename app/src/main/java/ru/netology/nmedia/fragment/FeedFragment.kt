package ru.netology.nmedia.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel
import kotlin.getValue

class FeedFragment : Fragment() {

    /*private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireActivity)*/
    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentFeedBinding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onLike(post: Post) {
                viewModel.likeById(post.id)
            }

            override fun onShare(post: Post) {
                viewModel.shareById(post.id)
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }

            override fun onView(post: Post) {
                viewModel.viewById(post.id)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onEdit(post: Post) {
                viewModel.startEditing(post)
                findNavController().navigate(R.id.action_feedFragment_to_editPostFragment)
            }

            override fun onVideoClick(post: Post) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.video))
                startActivity(intent)
            }

            override fun focusOnPost(post: Post) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_focusOnPostFragment,
                    bundleOf("idFocusPost" to post.id)
                )
            }


        })

        binding.container.adapter = adapter
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadPosts()
        }

        viewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts)
            binding.empty.isVisible = state.empty
            binding.errorGroup.isVisible = state.error
            binding.progress.isVisible = state.loading
            binding.swipeRefreshLayout.isRefreshing = state.loading
        }

        binding.retry.setOnClickListener {
            viewModel.loadPosts()
        }

        binding.save.setOnClickListener {
            viewModel.cancelEditing()
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        viewModel.postCreated.observe(viewLifecycleOwner) {
            viewModel.loadPosts()
            findNavController().navigateUp()
        }
        return binding.root
    }
}
