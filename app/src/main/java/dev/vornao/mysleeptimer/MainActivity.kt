package dev.vornao.mysleeptimer

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var timerValue = 0
        val addTime = findViewById<Button>(R.id.add_time)
        val subTime = findViewById<Button>(R.id.sub_time)
        val startTimer = findViewById<Button>(R.id.start_timer)
        val timeText = findViewById<TextView>(R.id.time)
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val sleepAdminReceiver = ComponentName(this, SleepAdminReceiver::class.java)
        val workManager = WorkManager.getInstance(this)

        checkNotificationPermission()

        // check if there is a timer running and cancel it
        if (workManager.getWorkInfosByTag("sleep").get().size > 0) {
            workManager.cancelAllWorkByTag("sleep")
            Snackbar.make(startTimer, "All timers cancelled", Snackbar.LENGTH_SHORT).show()
        }

        if (!devicePolicyManager.isAdminActive(sleepAdminReceiver)) {
            requestAdminPermissions(sleepAdminReceiver)
        }

        addTime.setOnClickListener {
            timerValue += 5
            timeText.text = getString(R.string.minutes_tv, timerValue)
        }

        subTime.setOnClickListener {
            timerValue -= 5
            if (timerValue < 0) {
                timerValue = 0
            }
            timeText.text = getString(R.string.minutes_tv, timerValue)
        }

        startTimer.setOnClickListener {
            if (timerValue == 0) {
                Snackbar.make(it, "Timer value cannot be 0", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // create sleep work request with timer value as initial delay and tag sleep
            val sleepWorkRequest = OneTimeWorkRequestBuilder<SleepWorker>()
                .setInitialDelay((timerValue).toLong(), java.util.concurrent.TimeUnit.MINUTES)
                .addTag("sleep")
                .build()

            workManager.enqueue(sleepWorkRequest)

            // add snackbar with button to dismiss the timer
            Snackbar.make(it, "Timer set for $timerValue minutes", Snackbar.LENGTH_LONG)
                .setAction("Dismiss") {
                    workManager.cancelAllWorkByTag("sleep")
                }.show()

            Log.d("MainActivity", "Timer set for $timerValue minutes")

        }
    }

    // add override function that will be called when apps becomes in foreground again.

    override fun onResume() {
        super.onResume()
        val startTimer = findViewById<Button>(R.id.start_timer)
        val workManager = WorkManager.getInstance(this)
        // delete all work with tag sleep if there is any and status is enqueued
        if (workManager.getWorkInfosByTag("sleep").get().size > 0) {
            workManager.cancelAllWorkByTag("sleep")
            Snackbar.make(startTimer, "All timers cancelled", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun requestAdminPermissions(componentName: ComponentName){
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).let {
            it.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            it.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Sleep timer")
            val r = registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    Toast.makeText(
                        this,
                        "Device admin enabled",
                        Toast.LENGTH_SHORT).show()
                        // relaunch app
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                } else {
                    Toast.makeText(
                        this,
                        "Device admin not enabled",
                        Toast.LENGTH_SHORT).show()
                }
            }
            r.launch(it)
        }
    }

    private fun checkNotificationPermission() {
        // RequestPermission

        val rp = ActivityResultContracts.RequestPermission()
        val r = registerForActivityResult(rp) { result ->
            if (result) {
                Toast.makeText(
                    this,
                    "Notification permission granted",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Notification permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    }
}