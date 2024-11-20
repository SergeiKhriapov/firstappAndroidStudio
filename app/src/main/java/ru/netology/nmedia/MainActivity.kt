package ru.netology.nmedia

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.viewmodel.PostViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: PostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onLike(post: Post) {
                viewModel.likeById(post.id)
            }

            override fun onShare(post: Post) {
                viewModel.shareById(post.id)
            }

            override fun onView(post: Post) {
                viewModel.viewById(post.id)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onEdit(post: Post) {
                viewModel.edit(post)
                showEditMenu(post.content, binding)
                binding.content.setText(post.content)
            }

            override fun offEdit(post: Post) {
                viewModel.offEdit()
                clearEditMenu(binding)
            }
        })

        binding.container.adapter = adapter

        viewModel.data.observe(this) { posts ->
            val new = adapter.currentList.size < posts.size
            adapter.submitList(posts) {
                if (new) {
                    binding.container.smoothScrollToPosition(posts.size)
                }
            }
        }

        viewModel.edited.observe(this) { editedPost ->
            if (editedPost.id != 0L) {
                showEditMenu(editedPost.content, binding)
                binding.content.setText(editedPost.content)
            } else {
                clearEditMenu(binding)
            }
        }

        binding.save.setOnClickListener {
            val text = binding.content.text.toString()
            if (text.isBlank()) {
                Toast.makeText(this, R.string.error_empty_content, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            viewModel.saveContent(text)
            binding.content.text.clear()
            AndroidUtils.hideKeyboard(it)
        }

        binding.cancellation.setOnClickListener {
            viewModel.offEdit()
            binding.content.text.clear()
            AndroidUtils.hideKeyboard(it)
            clearEditMenu(binding)
        }
    }

    private fun showEditMenu(content: String, binding: ActivityMainBinding) {
        binding.editContent.setText(content)
        binding.editMessageGroup.visibility = View.VISIBLE
    }

    private fun clearEditMenu(binding: ActivityMainBinding) {
        binding.editContent.text.clear()
        binding.editMessageGroup.visibility = View.GONE

    }
}
