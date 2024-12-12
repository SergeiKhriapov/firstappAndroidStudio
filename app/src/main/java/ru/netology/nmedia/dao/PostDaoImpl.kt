package ru.netology.nmedia.dao

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import ru.netology.nmedia.dto.Post

class PostDaoImpl(private val db: SQLiteDatabase) : PostDao {

    companion object {
        val DDL = """
        CREATE TABLE ${PostColumns.TABLE} (
            ${PostColumns.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${PostColumns.COLUMN_AUTHOR} TEXT NOT NULL,
            ${PostColumns.COLUMN_CONTENT} TEXT NOT NULL,
            ${PostColumns.COLUMN_PUBLISHED} TEXT NOT NULL,
            ${PostColumns.COLUMN_LIKE_BY_ME} BOOLEAN NOT NULL DEFAULT 0,
            ${PostColumns.COLUMN_LIKE_COUNT} INTEGER NOT NULL DEFAULT 0,
            ${PostColumns.COLUMN_SHARE_COUNT} INTEGER NOT NULL DEFAULT 0,
            ${PostColumns.COLUMN_VIEWS_COUNT} INTEGER NOT NULL DEFAULT 0,
            ${PostColumns.COLUMN_VIDEO} TEXT
        );
        """.trimIndent()
    }

    object PostColumns {
        const val TABLE = "posts"
        const val COLUMN_ID = "id"
        const val COLUMN_AUTHOR = "author"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_PUBLISHED = "published"
        const val COLUMN_LIKE_BY_ME = "likedByMe"
        const val COLUMN_LIKE_COUNT = "likes"
        const val COLUMN_SHARE_COUNT = "share"
        const val COLUMN_VIEWS_COUNT = "views"
        const val COLUMN_VIDEO = "video"
        val ALL_COLUMNS = arrayOf(
            COLUMN_ID,
            COLUMN_AUTHOR,
            COLUMN_CONTENT,
            COLUMN_PUBLISHED,
            COLUMN_LIKE_BY_ME,
            COLUMN_LIKE_COUNT,
            COLUMN_SHARE_COUNT,
            COLUMN_VIEWS_COUNT,
            COLUMN_VIDEO
        )
    }

    override fun getAll(): List<Post> {
        val posts = mutableListOf<Post>()
        db.query(
            PostColumns.TABLE,
            PostColumns.ALL_COLUMNS,
            null,
            null,
            null,
            null,
            "${PostColumns.COLUMN_ID} DESC"
        ).use {
            while (it.moveToNext()) {
                posts.add(map(it))
            }
        }
        return posts
    }

    override fun save(post: Post): Post {
        val values = ContentValues().apply {
            put(PostColumns.COLUMN_AUTHOR, "Me")
            put(PostColumns.COLUMN_CONTENT, post.content)
            put(PostColumns.COLUMN_PUBLISHED, "now")
            put(PostColumns.COLUMN_LIKE_BY_ME, if (post.likeByMe) 1 else 0)
            put(PostColumns.COLUMN_LIKE_COUNT, post.likeCount)
            put(PostColumns.COLUMN_SHARE_COUNT, post.shareCount)
            put(PostColumns.COLUMN_VIEWS_COUNT, post.viewsCount)
            put(PostColumns.COLUMN_VIDEO, post.video)
        }
        val id = if (post.id != 0L) {
            db.update(
                PostColumns.TABLE,
                values,
                "${PostColumns.COLUMN_ID} = ?",
                arrayOf(post.id.toString()),
            )
            post.id
        } else {
            db.insert(PostColumns.TABLE, null, values)
        }
        db.query(
            PostColumns.TABLE,
            PostColumns.ALL_COLUMNS,
            "${PostColumns.COLUMN_ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
        ).use {
            it.moveToNext()
            return map(it)
        }
    }

    override fun likeById(id: Long) {
        db.execSQL(
            """
           UPDATE ${PostColumns.TABLE} SET
               ${PostColumns.COLUMN_LIKE_COUNT} = ${PostColumns.COLUMN_LIKE_COUNT} + CASE WHEN ${PostColumns.COLUMN_LIKE_BY_ME} THEN -1 ELSE 1 END,
               ${PostColumns.COLUMN_LIKE_BY_ME} = CASE WHEN ${PostColumns.COLUMN_LIKE_BY_ME} THEN 0 ELSE 1 END
           WHERE id = ?;
        """.trimIndent(), arrayOf(id)
        )
    }

    override fun viewById(id: Long) {
        db.execSQL(
            """
        UPDATE ${PostColumns.TABLE} SET
            ${PostColumns.COLUMN_VIEWS_COUNT} = ${PostColumns.COLUMN_VIEWS_COUNT} + 1
        WHERE id = ?;
        """.trimIndent(), arrayOf(id)
        )
    }

    override fun shareById(id: Long) {
        db.execSQL(
            """
        UPDATE ${PostColumns.TABLE} SET
            ${PostColumns.COLUMN_SHARE_COUNT} = ${PostColumns.COLUMN_SHARE_COUNT} + 1
        WHERE id = ?;
        """.trimIndent(), arrayOf(id)
        )
    }

    override fun removeById(id: Long) {
        db.delete(
            PostColumns.TABLE,
            "${PostColumns.COLUMN_ID} = ?",
            arrayOf(id.toString())
        )
    }

    private fun map(cursor: Cursor): Post {
        with(cursor) {
            return Post(
                id = getLong(getColumnIndexOrThrow(PostColumns.COLUMN_ID)),
                author = getString(getColumnIndexOrThrow(PostColumns.COLUMN_AUTHOR)),
                content = getString(getColumnIndexOrThrow(PostColumns.COLUMN_CONTENT)),
                published = getString(getColumnIndexOrThrow(PostColumns.COLUMN_PUBLISHED)),
                likeByMe = getInt(getColumnIndexOrThrow(PostColumns.COLUMN_LIKE_BY_ME)) != 0,
                likeCount = getInt(getColumnIndexOrThrow(PostColumns.COLUMN_LIKE_COUNT)),
                shareCount = getInt(getColumnIndexOrThrow(PostColumns.COLUMN_SHARE_COUNT)),
                viewsCount = getInt(getColumnIndexOrThrow(PostColumns.COLUMN_VIEWS_COUNT)),
                video = getString(getColumnIndexOrThrow(PostColumns.COLUMN_VIDEO))
            )
        }
    }
}
