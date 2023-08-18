package dev.vornao.mysleeptimer

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Context.DEVICE_POLICY_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class SleepWorker (private val ctx: Context, params: WorkerParameters) : Worker(ctx, params){

    override fun doWork(): Result {
        val deviceManager = ctx.getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        try {
            deviceManager.lockNow()
        } catch (e: Exception) {
            Log.e("SleepWorker", "Error locking device: ${e.message}")
            postLockNotification()
            return Result.failure()
        }
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun postLockNotification() {
        // post a notification to the user that the device has been locked
        val newIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                42,
                newIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        val notificationBuilder =
            NotificationCompat.Builder(
                applicationContext,
                "LockNotification"
            )

        notificationBuilder.setContentTitle("Device Locked")
            .setContentText("Device was Locked by MySleepTimer")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, notificationBuilder.build())
    }
}