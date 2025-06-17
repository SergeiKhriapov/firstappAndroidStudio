package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.databinding.FragmentSignUpBinding
import ru.netology.nmedia.viewmodel.SignUpViewModel
import kotlin.getValue

class SignUpFragment : Fragment() {
    private val viewModel: SignUpViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignUpBinding.inflate(inflater, container, false)

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
            viewModel.register(name, login, password)
        }

        lifecycleScope.launch {
            viewModel.registrationSuccess.collectLatest { success ->
                if (success) {
                    findNavController().popBackStack()
                    viewModel.resetRegistrationSuccess()
                    Snackbar.make(binding.root, "Регистрация прошла успешно!", Snackbar.LENGTH_SHORT).show()

                }
            }
        }
        return binding.root
    }
}