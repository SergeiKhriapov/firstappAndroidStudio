package ru.netology.nmedia.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.netology.nmedia.dao.LocalPostDao
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.entities.LocalPostEntity
import ru.netology.nmedia.entities.PostEntity

@Database(entities = [PostEntity::class, LocalPostEntity::class], version = 1)
abstract class AppDb : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun localPostDao(): LocalPostDao
}
