package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentFocusOnAttachmentBinding
import ru.netology.nmedia.dto.Post

class FocusOnAttachmentFragment : Fragment() {

    private var _binding: FragmentFocusOnAttachmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFocusOnAttachmentBinding.inflate(inflater, container, false)

        // Получаем объект Post из аргументов
        val post = arguments?.getParcelable<Post>("post")

        if (post?.attachment != null) {
            binding.focusAttachment.visibility = View.VISIBLE

            // Формируем полный URL к изображению (замени адрес, если нужно)
            val imageUrl = "http://10.0.2.2:9999/media/${post.attachment.url}"

            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.hourglass_24_ic)
                .error(R.drawable.error_ic)
                .timeout(10_000)
                .into(binding.focusAttachment)
        } else {
            binding.focusAttachment.visibility = View.GONE
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}