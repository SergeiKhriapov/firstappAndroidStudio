package ru.netology.nmedia.repository

import android.content.Context
import java.io.File

class FileRepositoryImpl(private val context: Context) : FileRepository {
    override fun saveToInternalStorage(file: File): File {
        val internalDir = context.filesDir
        val internalFile = File(internalDir, file.name)
        file.copyTo(internalFile, overwrite = true)
        return internalFile
    }
}