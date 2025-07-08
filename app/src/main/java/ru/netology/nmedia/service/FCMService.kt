package ru.netology.nmedia.service

import android.Manifest
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
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    private val keyContent = "content"
    private val channelId = "channelId"
    private val gson = Gson()

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreate() {
        super.onCreate()
        registerChannel()
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", token)
        appAuth.sendPushTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        message.data[keyContent]?.let {
            val push = gson.fromJson(it, Push::class.java)
            val myId = appAuth.data.value?.id

            when {
                push.recipientId == null -> showPushNotification(push.content)
                push.recipientId == myId -> showPushNotification(push.content)
                push.recipientId == 0L && myId == null -> showPushNotification(push.content)
                push.recipientId != myId -> appAuth.sendPushTokenToServer()
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

    private fun notify(notification: Notification) {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this)
                .notify(Random.nextInt(100_000), notification)
        }
    }
}

// Дополнительные классы и enum

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
