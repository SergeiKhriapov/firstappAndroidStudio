package ru.netology.nmedia

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.formatting.formatCount
import ru.netology.nmedia.viewmodel.PostViewModel

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel: PostViewModel by viewModels()
        viewModel.post.observe(this) { post ->
            binding.content?.text = post.content
            binding.author?.text = post.author
            binding.published?.text = post.published
            binding.like?.setImageResource(
                if (post.likeByMe) R.drawable.ic_baseline_favorite_24
                else R.drawable.ic_baseline_favorite_border_24
            )
            binding.likeCount?.text = formatCount(post.likeCount)
            binding.shareCount?.text = formatCount(post.shareCount)
            binding.viewsCount?.text = formatCount(post.viewsCount)

            binding.like?.setOnClickListener {
                viewModel.like(post.id)
            }
            binding.share?.setOnClickListener {
                viewModel.share(post.id)
            }
            binding.views?.setOnClickListener {
                viewModel.view(post.id)
            }
        }
    }
}
