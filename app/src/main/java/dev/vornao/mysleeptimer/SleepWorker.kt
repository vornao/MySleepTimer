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
            // cancel notification
            val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(1)
        } catch (e: Exception) {
            Log.e("SleepWorker", "Error locking device: ${e.message}")
            return Result.failure()
        }
        return Result.success()
    }
}