package ru.netology.nmedia.repository.file

import java.io.File

interface FileRepository {
    fun saveToInternalStorage(file: File): File
}