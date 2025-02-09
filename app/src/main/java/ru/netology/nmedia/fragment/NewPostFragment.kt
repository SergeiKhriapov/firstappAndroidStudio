package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.util.focusAndShowKeyboard
import ru.netology.nmedia.viewmodel.PostViewModel

class NewPostFragment : Fragment() {
    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(inflater, container, false)
        arguments?.textArg?.let { binding.edit.setText(it) }
        binding.edit.post {
            binding.edit.focusAndShowKeyboard()
        }
        val draftContent = viewModel.getDraft()
        arguments?.textArg?.let { binding.edit.setText(it) }
            ?: draftContent?.let { binding.edit.setText(it) }
        binding.edit.post {
            binding.edit.setSelection(binding.edit.text.length)
            binding.edit.focusAndShowKeyboard()
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            viewModel.saveDraft(binding.edit.text.toString())
            findNavController().navigateUp()
        }

        binding.ok.setOnClickListener {
            val text = binding.edit.text.toString()
            if (text.isBlank()) {
                Snackbar.make(binding.root, R.string.error_empty_content, Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomAppBar)
                    .show()
            } else {
                viewModel.saveContent(text)
                findNavController().navigateUp()
            }
        }
        return binding.root
    }

    companion object {
        var Bundle.textArg by StringArg
    }
}
