package dev.vornao.mysleeptimer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var timerValue = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val addTime = findViewById<Button>(R.id.add_time)
        val subTime = findViewById<Button>(R.id.sub_time)
        val timeText = findViewById<TextView>(R.id.time)
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val sleepAdminReceiver = ComponentName(this, SleepAdminReceiver::class.java)

        if (!devicePolicyManager.isAdminActive(sleepAdminReceiver)) {
            requestAdmin(sleepAdminReceiver)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requireNotificationPermission()

        createNotificationChannel()

        addTime.setOnClickListener {
            timerValue += 5
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
            timeText.text = getString(R.string.minutes_tv, timerValue)
        }

        subTime.setOnClickListener {
            timerValue -= 5
            if (timerValue <= 0) {
                timerValue = 0
                timeText.text = getString(R.string.no_timer_set)
            } else {
                timeText.text = getString(R.string.minutes_tv, timerValue)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        scheduleTimer(timerValue)
    }

    override fun onResume() {
        super.onResume()
        timerValue = 0
        val timeText = findViewById<TextView>(R.id.time)
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        val workManager = WorkManager.getInstance(this)

        timeText.text = getString(R.string.no_timer_set)
        notificationManager.cancelAll()

        if (workManager.getWorkInfosByTag("sleep").get().size > 0) {
            workManager.cancelAllWorkByTag("sleep")
            Snackbar.make(
                findViewById(R.id.time),
                getString(R.string.all_timers_cancelled),
                Snackbar.LENGTH_SHORT
            ).show()
        }


    }

    private fun requestAdmin(sleepAdminReceiver: ComponentName) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, sleepAdminReceiver)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            getString(R.string.explaination_admin)
        )

        val launcher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode != RESULT_OK) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.dialog_admin_error_title))
                        .setMessage(getString(R.string.dialog_admin_error_message))
                        .setPositiveButton("Understand") { dialog, _ ->
                            dialog.dismiss()
                            this@MainActivity.finish()
                        }
                        .show()
                }
            }
        launcher.launch(intent)
    }

    @SuppressLint("MissingPermission")
    private fun scheduleTimer(timerValue: Int) {
        if (timerValue == 0) return
        val workManager = WorkManager.getInstance(this)
        val sleepTimer = OneTimeWorkRequestBuilder<SleepWorker>()
            .setInitialDelay(timerValue.toLong(), java.util.concurrent.TimeUnit.MINUTES)
            .addTag("sleep")
            .build()
        workManager.enqueue(sleepTimer)

        val time = System.currentTimeMillis() + timerValue * 60 * 1000
        val timeString = SimpleDateFormat("HH:mm", Locale("IT")).format(Date(time))
        NotificationManagerCompat.from(this).notify(1, buildNotification(timeString))
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requireNotificationPermission() {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (!it) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.dialog_notification_error_title))
                        .setMessage(getString(R.string.dialog_notification_error_message))
                        .show()
                }
            }
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun createNotificationChannel() {
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                getString(R.string.notification_channel_id),
                getString(R.string.notification_channel_name),
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    private fun buildNotification(timeString: String): Notification {

        val dismissIntent = Intent(this, SleepDismissReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, dismissIntent,
            FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        return androidx.core.app.NotificationCompat.Builder(
            this, getString(R.string.notification_channel_id)
        )
            .setSmallIcon(R.drawable.baseline_nights_stay_24)
            .setContentTitle(getString(R.string.sleep_set_notification_title))
            .setContentText(getString(R.string.notification_text, timeString))
            .setChannelId(getString(R.string.notification_channel_id))
            .addAction(R.drawable.ic_launcher_foreground, "Cancel timer", pendingIntent)
            .setOngoing(true)
            .build()
    }
}