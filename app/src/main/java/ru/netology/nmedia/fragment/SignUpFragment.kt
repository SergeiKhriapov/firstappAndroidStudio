package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import ru.netology.nmedia.R
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.databinding.FragmentSignUpBinding
import ru.netology.nmedia.viewmodel.SignUpViewModel
import kotlin.getValue

class SignUpFragment : Fragment() {

    private val viewModel: SignUpViewModel by activityViewModels()
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private val photoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(requireContext(), "Ошибка выбора фото", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            val uri = it.data?.data ?: return@registerForActivityResult
            viewModel.changePhoto(uri, uri.toFile())
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)

        // Кнопка Выбора фото
        binding.pickPhotoButton.setOnClickListener {
            ImagePicker.Builder(this)
                .crop()
                .galleryOnly()
                .maxResultSize(2048, 2048)
                .createIntent { intent ->
                    photoLauncher.launch(intent)
                }
        }

        // Кнопка удалить фото
        binding.removePhotoButton?.setOnClickListener {
            viewModel.removePhoto()
        }

        // Подписка на обновление фото
        viewModel.photo.observe(viewLifecycleOwner) { photo ->
            if (photo != null) {
                binding.photoPreview.setImageURI(photo.uri)
                binding.photoPreview.isVisible = true
                binding.removePhotoButton?.isVisible = true
            } else {
                binding.photoPreview.setImageResource(R.drawable.image_preview_background)
                binding.removePhotoButton?.isVisible = false
            }
        }

        // Кнопка регистрации
        binding.signUpButton.setOnClickListener {
            val name = binding.nameEditText.text.toString()
            val login = binding.loginEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (name.isBlank() || login.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                Snackbar.make(binding.root, "Все поля обязательны!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Snackbar.make(binding.root, "Пароли не совпадают!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val photo = viewModel.photo.value
            if (photo != null) {
                viewModel.registerWithPhoto(name, login, password, photo.uri, requireContext())
            } else {
                viewModel.register(name, login, password)
            }
        }

        lifecycleScope.launch {
            viewModel.registrationSuccess.collectLatest { success ->
                if (success) {
                    findNavController().popBackStack()
                    viewModel.resetRegistrationSuccess()
                    Snackbar.make(
                        binding.root,
                        "Регистрация прошла успешно!",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    viewModel.removePhoto()
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
