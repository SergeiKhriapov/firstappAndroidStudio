package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentFocusOnAttachmentBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class FocusOnAttachmentFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFocusOnAttachmentBinding.inflate(inflater, container, false)

        viewModel.data.observe(viewLifecycleOwner) { feedModel ->
            val posts = feedModel.posts
            val currentPost =
                posts.find { it.id == arguments?.getLong("idFocusPost") } ?: return@observe

            if (currentPost.attachment != null) {
                binding.focusAttachment.visibility = View.VISIBLE
                val imageUrl = "http://10.0.2.2:9999/media/${currentPost.attachment.url}"

                Glide.with(binding.focusAttachment)
                    .load(imageUrl)
                    .placeholder(R.drawable.hourglass_24_ic)
                    .error(R.drawable.error_ic)
                    .timeout(10_000)
                    .into(binding.focusAttachment)
            } else {
                binding.focusAttachment.visibility = View.GONE
            }
        }

        return binding.root
    }
}
