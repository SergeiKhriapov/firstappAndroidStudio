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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel
import kotlin.getValue

class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentFeedBinding = FragmentFeedBinding.inflate(inflater, container, false)

        // Создаем адаптер для списка постов
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
                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
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

            override fun focusOnAttachment(post: Post) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_focusOnAttachmentFragment,
                    bundleOf("idFocusPost" to post.id)
                )
            }
            override fun showError(message: String) {
                Log.e("FeedFragment", "Ошибка: $message")
            }
        })


        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.newerCount.collect { value ->
                    Log.d("FeedFragment", "Наблюдаем за newerCount: $value")
                    println(value)
                }
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d("FeedFragment", "Обновляем посты...")
            viewModel.refreshPosts()
        }

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            Log.d(
                "FeedFragment",
                "Состояние данных: Загрузка = ${state.loading}, Ошибка = ${state.error}"
            )
            binding.progressLoadPosts.isVisible = state.loading
            binding.swipeRefreshLayout.isRefreshing = state.loading

            if (state.error) {
                Snackbar.make(binding.root, R.string.error_text, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.retry) {
                        Log.d("FeedFragment", "Повторная попытка загрузить посты после ошибки...")
                        viewModel.loadPosts()
                    }
                    .show()
            }
        }

        viewModel.syncError.observe(viewLifecycleOwner) { hasError ->
            if (hasError) {
                Log.d("FeedFragment", "ошибка синхронизации")
                Snackbar.make(binding.root, R.string.error_synchronization, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.retry) {
                        Log.d("FeedFragment", "Повторная попытка синхронизации постов...")
                        viewModel.syncPosts()
                    }
                    .show()
            }
        }

        viewModel.likeError.observe(viewLifecycleOwner) {
            Log.d("FeedFragment", "Произошла ошибка при лайке")
            Snackbar.make(binding.root, R.string.error_local_like, Snackbar.LENGTH_SHORT)
                .setAction(R.string.retry) {
                    Log.d(
                        "FeedFragment",
                        "Повторная попытка синхронизации постов после ошибки лайка..."
                    )
                    viewModel.syncPosts()
                }
                .show()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collect { feedModel ->
                    Log.d("FeedFragment", "Получено количество постов: ${feedModel.posts.size}")
                    adapter.submitList(feedModel.posts) {
                        /*binding.list.scheduleLayoutAnimation()*/

                        binding.list.scrollToPosition(0) // простой, надёжный способ
                        binding.list.scheduleLayoutAnimation()
                    }
                    binding.empty.isVisible = feedModel.empty
                }
            }
        }
        binding.addNewPost.setOnClickListener {
            if (viewModel.isAuthenticated.value != true) {
                viewModel.shouldShowAuthDialog.call()
                return@setOnClickListener
            }

            Log.d("FeedFragment", "Переход на экран создания нового поста")
            viewModel.cancelEditing()
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

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

        viewModel.hiddenSyncedCount.observe(viewLifecycleOwner) { count ->
            binding.fabCounter.text = count.toString()
            val visibility = if (count > 0) View.VISIBLE else View.GONE
            binding.fabCounter.visibility = visibility
            binding.hiddenVisible.visibility = visibility
            binding.hiddenVisible.setOnClickListener {
                viewModel.unhideAllSyncedPosts()
            }
        }
        viewModel.postCreated.observe(viewLifecycleOwner) {
            Log.d("FeedFragment", "Пост создан, перезагружаем посты")
            viewModel.loadPosts()
            binding.list.post {
            }
            findNavController().navigateUp()
        }
        return binding.root
    }
}
