package ru.netology.nmedia.service

import android.Manifest
import android.R.id.content
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import kotlin.random.Random

class FCMService : FirebaseMessagingService() {
    private val keyAction = "action"
    private val keyContent = "content"
    private val channelId = "channelId"
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        registerChannel()

    }

    override fun onNewToken(token: String) {
        Log.d("FCM", token)
        AppAuth.getInstance().sendPushTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        message.data[keyContent]?.let {
            val push = gson.fromJson(it, Push::class.java)
            val myId = AppAuth.getInstance().data.value?.id

            when {
                push.recipientId == null -> {
                    showPushNotification(push.content)
                }

                push.recipientId == myId -> {
                    showPushNotification(push.content)
                }

                push.recipientId == 0L && myId != null -> {
                    AppAuth.getInstance().sendPushTokenToServer()
                }

                push.recipientId != myId -> {
                    AppAuth.getInstance().sendPushTokenToServer()
                }
            }
        }
    }
    private fun showPushNotification(content: String) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Push-сообщение")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notify(notification)
    }


    private fun registerChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_remote_name)
            val descriptionText = getString(R.string.channel_remote_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun handleNewPost(content: NewPost) {
        val notificationText = getString(
            R.string.notification_user_posted_new_post,
            content.userName,
            content.content?.take(100)
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    content.content
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notify(notification)
    }

    private fun handleLike(content: Like) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(
                    R.string.notification_user_liked,
                    content.userName,
                    content.postAuthor,
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notify(notification)
    }

    private fun handleUnknownAction(action: String) {
        Log.e("FCMService", "Unknown action: $action")
    }

    private fun notify(notification: Notification) {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this)
                .notify(Random.nextInt(100_000), notification)
        }
    }
}

enum class Action {
    Like,
    NewPost
}

data class Like(
    val userId: Long,
    val userName: String?,
    val postId: Long,
    val postAuthor: String?
)

data class NewPost(
    val userId: Long,
    val userName: String?,
    val postId: Long,
    val content: String?
)

data class Push(
    val recipientId: Long?,
    val content: String
)