package ru.netology.nmedia.repository.file

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileRepository {
    override fun saveToInternalStorage(file: File): File {
        val internalDir = context.filesDir
        val internalFile = File(internalDir, file.name)
        file.copyTo(internalFile, overwrite = true)
        return internalFile
    }
}