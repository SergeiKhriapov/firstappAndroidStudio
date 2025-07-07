package ru.netology.nmedia.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentEditPostBinding
import ru.netology.nmedia.di.DependencyContainer
import ru.netology.nmedia.util.focusAndShowKeyboard
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.viewmodel.ViewModelFactory

class EditPostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels(
        factoryProducer = {
            ViewModelFactory(
                DependencyContainer.getInstance().appAuth,
                DependencyContainer.getInstance().repository,
                DependencyContainer.getInstance().localRepository,
                ru.netology.nmedia.repository.FileRepositoryImpl(requireContext())
            )
        }
    )

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentEditPostBinding.inflate(inflater, container, false)

        val post = viewModel.edited.value

        binding.originalText.setText(post?.content)
        binding.originalText.isEnabled = false
        binding.originalText.setTextColor(resources.getColor(android.R.color.black, null))

        binding.editPost.setText(post?.content)

        binding.editPost.focusAndShowKeyboard()

        binding.ok.setOnClickListener {
            val updatedContent = binding.editPost.text.toString()
            if (updatedContent.isNotEmpty()) {
                viewModel.saveContent(binding.editPost.text.toString())
                findNavController().popBackStack()
            } else {
                Snackbar.make(binding.root, R.string.error_empty_content, Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomAppBar)
                    .show()
            }
        }

        binding.exitEdit.setOnClickListener {
            viewModel.cancelEditing()
            findNavController().popBackStack()
        }
        return binding.root
    }
}
