package com.example.notifapi.classes

import android.Manifest
import android.R
import android.app.PendingIntent
import android.content.Intent
import com.example.notifapi.MainActivity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val channelId = "default_channel"
    private val notificationId = 1

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {
        createNotificationChannel()
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        val builder = setupInitialNotification()
        notificationManager.notify(notificationId, builder.build())

        simulateDownloadProgress(notificationManager, builder)

        showCompletionNotification(notificationManager)

        return Result.success()
    }

    private fun setupInitialNotification(): NotificationCompat.Builder {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargando archivo...")
            .setContentText("Progreso de descarga")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, false)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun simulateDownloadProgress(
        notificationManager: NotificationManagerCompat,
        builder: NotificationCompat.Builder
    ) {
        for (i in 1..100 step 10) {
            try {
                Thread.sleep(500)
                builder.setProgress(100, i, false)
                    .setContentText("Descargando... $i%")
                notificationManager.notify(notificationId, builder.build())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showCompletionNotification(notificationManager: NotificationManagerCompat) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("download_complete", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.stat_sys_download_done)
            .setContentTitle("Descarga completada")
            .setContentText("Toca para abrir la app")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Aumentamos prioridad
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Descargas"
            val descriptionText = "Notificaciones de progreso de descargas"
            val importance = NotificationManager.IMPORTANCE_DEFAULT // Cambiado para que suene al completar
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }

            val notificationManager = applicationContext.getSystemService(
                NotificationManager::class.java
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
