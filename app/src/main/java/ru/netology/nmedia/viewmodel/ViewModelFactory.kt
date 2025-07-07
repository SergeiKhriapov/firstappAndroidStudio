package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.repository.FileRepository
import ru.netology.nmedia.repository.LocalPostRepositoryImpl
import ru.netology.nmedia.repository.PostRepository

class PostViewModelFactory(
    private val appAuth: AppAuth,
    private val repository: PostRepository,
    private val localRepository: LocalPostRepositoryImpl,
    private val fileRepository: FileRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when {

            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(appAuth) as T
            }

            modelClass.isAssignableFrom(PostViewModel::class.java) -> {
                PostViewModel(appAuth, repository, localRepository, fileRepository) as T
            }

            modelClass.isAssignableFrom(SignInViewModel::class.java) -> {
                SignInViewModel(appAuth) as T
            }

            modelClass.isAssignableFrom(SignUpViewModel::class.java) -> {
                SignUpViewModel(appAuth) as T
            }

            else -> error("Unknown class $modelClass")
        }

}