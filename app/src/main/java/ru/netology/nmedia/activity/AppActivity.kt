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
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityAppBinding
import ru.netology.nmedia.viewmodel.AuthViewModel

@AndroidEntryPoint
class AppActivity : AppCompatActivity() {
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.fragment_container)

        handleSendIntent(intent, binding)

        requestNotificationsPermission()

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.auth_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                val isFeedFragment = navController.currentDestination?.id == R.id.feedFragment
                val isAuthorized = viewModel.isAuthorized

                menu.setGroupVisible(R.id.unauthorized, isFeedFragment && !isAuthorized)
                menu.setGroupVisible(R.id.authorized, isFeedFragment && isAuthorized)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
                R.id.singIn -> {
                    navController.navigate(R.id.action_feedFragment_to_signInFragment)
                    true
                }

                R.id.singUp -> {
                    navController.navigate(R.id.action_feedFragment_to_signUnFragment)
                    true
                }

                R.id.logout -> {
                    viewModel.logout()
                    true
                }

                else -> false
            }
        }, this)

        navController.addOnDestinationChangedListener { _, _, _ ->
            invalidateOptionsMenu()
        }

        viewModel.authData
            .flowWithLifecycle(lifecycle)
            .onEach {
                invalidateOptionsMenu()
            }
            .launchIn(lifecycleScope)
    }

    private fun handleSendIntent(intent: Intent?, binding: ActivityAppBinding) {
        intent?.let {
            if (it.action == Intent.ACTION_SEND) {
                val text = it.getStringExtra(Intent.EXTRA_TEXT)
                if (text.isNullOrBlank()) {
                    Snackbar.make(
                        binding.root,
                        R.string.error_empty_content,
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(android.R.string.ok) { finish() }
                        .show()
                    return@let
                }

                navController.navigate(R.id.action_feedFragment_to_newPostFragment)
            }
        }
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 1)
        }
    }
}
