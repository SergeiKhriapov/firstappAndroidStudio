package ru.netology.nmedia

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.formatting.formatCount


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val post = Post(
            id = 1,
            author = "Нетология. Университет интернет-профессий",
            published = "06 ноября в 21:20",
            content = "Привет, это новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия - помочь встать на путь роста и начать цепочку перемен - http://netolo.gy/lyb",
            likeCount = 0,
            shareCount = 0,
            viewsCount = 0
        )
        binding.content?.text = post.content
        binding.author?.text = post.author
        binding.published?.text = post.published

        binding.like?.setOnClickListener {
            post.likeByMe = !post.likeByMe
            if (post.likeByMe) {
                binding.like.setImageResource(R.drawable.ic_baseline_favorite_24)
                post.likeCount++
            } else {
                binding.like.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                post.likeCount--
            }
            binding.likeCount?.text = formatCount(post.likeCount)
        }

        binding.share?.setOnClickListener {
            post.shareCount++
            binding.shareCount?.text = formatCount(post.shareCount)
        }
        binding.views?.setOnClickListener {
            post.viewsCount++
            binding.viewsCount?.text = formatCount(post.viewsCount)
        }
    }
}