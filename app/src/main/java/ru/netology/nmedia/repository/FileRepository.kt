package ru.netology.nmedia.repository

import java.io.File

interface FileRepository {
    fun saveToInternalStorage(file: File): File
}
