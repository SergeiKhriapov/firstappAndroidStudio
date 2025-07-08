package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.util.focusAndShowKeyboard
import ru.netology.nmedia.viewmodel.PostViewModel
import kotlin.getValue

class NewPostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private var isPosting = false  // !! флаг

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(inflater, container, false)

        val text = arguments?.textArg ?: viewModel.getDraft()
        text?.let { binding.edit.setText(it) }

        binding.edit.post {
            binding.edit.setSelection(binding.edit.text.length)
            binding.edit.focusAndShowKeyboard()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.saveDraft(binding.edit.text.toString())
            findNavController().navigateUp()
        }

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.new_post, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    if (isPosting) return true

                    val textSave = binding.edit.text.toString()
                    if (textSave.isBlank()) {
                        Snackbar.make(
                            binding.root,
                            R.string.error_empty_content,
                            Snackbar.LENGTH_SHORT
                        ).setAnchorView(binding.bottomAppBar).show()
                    } else {
                        isPosting = true
                        viewModel.saveContent(textSave)
                    }
                    AndroidUtils.hideKeyboard(requireView())
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
        val photoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == ImagePicker.RESULT_ERROR) {
                    Toast.makeText(
                        requireContext(),
                        R.string.photo_pick_error, Toast.LENGTH_SHORT
                    ).show()
                    return@registerForActivityResult
                }
                val result = it.data?.data ?: return@registerForActivityResult
                viewModel.changePhoto(result, result.toFile())
            }
        viewModel.photo.observe(viewLifecycleOwner) { photo ->
            if (photo == null) {
                binding.previewContainer.isGone = true
                return@observe
            }
            binding.previewContainer.isVisible = true
            binding.preview.setImageURI(photo.uri)
        }

        binding.removePhoto.setOnClickListener {
            viewModel.removePhoto()
        }
        binding.takePhoto.setOnClickListener {
            ImagePicker.Builder(this)
                .crop()
                .cameraOnly()
                .maxResultSize(MAX_PHOTO_SIZE_PIX, MAX_PHOTO_SIZE_PIX)
                .createIntent {
                    photoLauncher.launch(it)
                }
        }

        binding.pickPhoto.setOnClickListener {
            ImagePicker.Builder(this)
                .crop()
                .galleryOnly()
                .maxResultSize(MAX_PHOTO_SIZE_PIX, MAX_PHOTO_SIZE_PIX)
                .createIntent {
                    photoLauncher.launch(it)
                }

        }

        viewModel.postCreated.observe(viewLifecycleOwner) {
            isPosting = false // !!!!!!!!!!!!!!!!! сбрасываем флаг
            findNavController().navigateUp()
        }

        return binding.root
    }

    companion object {
        var Bundle.textArg by StringArg
        private const val MAX_PHOTO_SIZE_PIX = 2048
    }
}

