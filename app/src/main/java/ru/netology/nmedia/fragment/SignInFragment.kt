package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ru.netology.nmedia.databinding.FragmentSignInBinding
import ru.netology.nmedia.viewmodel.SignInViewModel

class SignInFragment : Fragment() {

    private val viewModel: SignInViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignInBinding.inflate(inflater, container, false)

        binding.signInButton.setOnClickListener {
            val login = binding.loginEditText.text.toString()
            val pass = binding.passwordEditText.text.toString()

            if (login.isBlank() || pass.isBlank()) {
                Toast.makeText(requireContext(), "Пожалуйста, введите логин и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(login, pass)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.signInButton.isEnabled = !isLoading
        }

        viewModel.authSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                requireActivity().onBackPressed()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        return binding.root
    }
}

