package ru.netology.nmedia.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

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
                startActivity(Intent.createChooser(intent, getString(R.string.chooser_share_post)))
            }
            override fun onView(post: Post) = viewModel.viewById(post.id)
            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }
            override fun onEdit(post: Post) {
                viewModel.startEditing(post)
                findNavController().navigate(R.id.action_feedFragment_to_editPostFragment)
            }
            override fun onVideoClick(post: Post) {
                post.video?.takeIf { it.isNotBlank() }?.let {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                    startActivity(intent)
                }
            }
            override fun focusOnPost(post: Post) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_focusOnPostFragment,
                    bundleOf("post" to post)
                )
            }
            override fun focusOnAttachment(post: Post) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_focusOnAttachmentFragment,
                    bundleOf("post" to post)
                )
            }
            override fun showError(message: String) {
                Log.e("FeedFragment", "Ошибка: $message")
            }
        })

        binding.list.adapter = adapter

        // Подписка на PagingData из ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
            }
        }

        // Управление состоянием загрузки PagingData
        adapter.addLoadStateListener { loadState ->
            val isLoading = loadState.refresh is LoadState.Loading
            binding.progressLoadPosts.isVisible = isLoading
            binding.swipeRefreshLayout.isRefreshing = isLoading

            val errorState = loadState.source.refresh as? LoadState.Error
                ?: loadState.source.append as? LoadState.Error
                ?: loadState.source.prepend as? LoadState.Error
                ?: loadState.refresh as? LoadState.Error

            errorState?.let {
                Snackbar.make(binding.root, R.string.error_text, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry) { adapter.retry() }
                    .show()
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            adapter.refresh()
        }

        // Подписка на ошибки из ViewModel
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progressLoadPosts.isVisible = state.loading
            binding.swipeRefreshLayout.isRefreshing = state.loading

            if (state.error) {
                Snackbar.make(binding.root, R.string.error_text, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.retry) {
                        viewModel.loadPosts()
                    }
                    .show()
            }
        }

 /*       viewModel.syncError.observe(viewLifecycleOwner) { hasError ->
            if (hasError) {
                Snackbar.make(binding.root, R.string.error_synchronization, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.retry) {
                        viewModel.syncPosts()
                    }
                    .show()
            }
        }

        viewModel.likeError.observe(viewLifecycleOwner) {
            Snackbar.make(binding.root, R.string.error_local_like, Snackbar.LENGTH_SHORT)
                .setAction(R.string.retry) {
                    viewModel.syncPosts()
                }
                .show()
        }*/

        viewModel.shouldShowAuthDialog.observe(viewLifecycleOwner) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Авторизация")
                .setMessage("Для лайка или создания поста необходимо авторизоваться. Перейти к авторизации?")
                .setPositiveButton("Да") { _, _ ->
                    findNavController().navigate(R.id.action_feedFragment_to_signInFragment)
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        viewModel.postCreated.observe(viewLifecycleOwner) {
            Log.d("FeedFragment", "Пост создан, перезагружаем посты")
            viewModel.loadPosts()
            findNavController().navigateUp()
        }

        binding.addNewPost.setOnClickListener {
            if (viewModel.isAuthenticated.value != true) {
                viewModel.shouldShowAuthDialog.call()
                return@setOnClickListener
            }
            viewModel.cancelEditing()
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        return binding.root
    }
}
