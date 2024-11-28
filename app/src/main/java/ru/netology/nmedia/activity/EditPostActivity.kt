package ru.netology.nmedia.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityEditPostBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class EditPostActivity : AppCompatActivity() {

    private val viewModel: PostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val content = intent.getStringExtra("content") ?: ""
        binding.originalText.setText(content)
        binding.originalText.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            setCursorVisible(false)
        }
        binding.editPost.setText(content)
        binding.editPost.requestFocus()
        binding.ok.setOnClickListener {
            val updatedContent = binding.editPost.text.toString()
            if (updatedContent.isNotEmpty()) {
                viewModel.saveContent(updatedContent)
                setResult(RESULT_OK, Intent().putExtra("text", updatedContent))
                finish()
            } else {
                Snackbar.make(binding.root, R.string.error_empty_content, Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomAppBar)
                    .show()
            }
        }
        binding.exitEdit.setOnClickListener {
            finish()
        }
    }
}

object EditPostContract : ActivityResultContract<String, String?>() {
    override fun createIntent(context: Context, input: String) =
        Intent(context, EditPostActivity::class.java).apply {
            putExtra("content", input)
        }

    override fun parseResult(resultCode: Int, intent: Intent?) =
        intent?.getStringExtra("text")
}
