package ru.netology.nmedia.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg
import ru.netology.nmedia.databinding.ActivityAppBinding
import ru.netology.nmedia.viewmodel.AuthViewModel

class AppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent?.let {
            if (it.action == Intent.ACTION_SEND) {
                val text = it.getStringExtra(Intent.EXTRA_TEXT)
                if (text.isNullOrBlank()) {
                    Snackbar.make(
                        binding.root,
                        R.string.error_empty_content,
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(android.R.string.ok) {
                            finish()
                        }
                        .show()
                    return@let
                }

                findNavController(R.id.fragment_container).navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        textArg = text
                    }
                )
            }
        }
        requestNotificationsPermission()
        addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.auth_menu, menu)


                    val viewModel by viewModels<AuthViewModel>()
                    viewModel.authData.flowWithLifecycle(lifecycle)
                        .onEach {
                            val isAuthorized = viewModel.isAuthorized
                            menu.setGroupVisible(R.id.unauthorized, isAuthorized.not())
                            menu.setGroupVisible(R.id.authorized, isAuthorized)
                        }
                        .launchIn(lifecycleScope)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.singIn, R.id.singUp -> {
                            findNavController(R.id.fragment_container).navigate(R.id.action_feedFragment_to_signInFragment)
                            true
                            /*AppAuth.getInstance().setAuth(5, "x-token")
                            true*/
                        }

                        R.id.logout -> {
                            AppAuth.getInstance().clearAuth()
                            true
                        }

                        else -> false
                    }
            }
        )
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        requestPermissions(arrayOf(permission), 1)
    }
}