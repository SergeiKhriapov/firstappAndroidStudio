package ru.netology.nmedia.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.netology.nmedia.dao.LocalPostDao
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.entities.LocalPostEntity
import ru.netology.nmedia.entities.PostEntity

@Database(entities = [PostEntity::class, LocalPostEntity::class], version = 1)
abstract class AppDb : RoomDatabase() {
    abstract val postDao: PostDao
    abstract val localPostDao: LocalPostDao

  /*  companion object {
        @Volatile
        private var instance: AppDb? = null

        fun getInstance(context: Context): AppDb {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context)
                    .also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDb {
            return Room.databaseBuilder(
                context, AppDb::class.java, "app.db"
            )
                .build()
        }
    }*/
}
