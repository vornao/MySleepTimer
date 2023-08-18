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
import androidx.appcompat.app.AppCompatDelegate
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
        val timeText = findViewById<TextView>(R.id.time)
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val sleepAdminReceiver = ComponentName(this, SleepAdminReceiver::class.java)
        val workManager = WorkManager.getInstance(this)

        if (!devicePolicyManager.isAdminActive(sleepAdminReceiver)) {
            requestAdmin(sleepAdminReceiver, devicePolicyManager)
        }

        // check if there is a timer running and cancel it
        if (workManager.getWorkInfosByTag("sleep").get().size > 0) {
            workManager.cancelAllWorkByTag("sleep")
            Snackbar.make(addTime, "All timers cancelled", Snackbar.LENGTH_SHORT).show()
        }

        addTime.setOnClickListener {
            timerValue += 5
            timeText.text = getString(R.string.minutes_tv, timerValue)
            rescheduleTimer(timerValue)
        }

        subTime.setOnClickListener {
            timerValue -= 5
            if (timerValue <= 0) {
                timerValue = 0
                timeText.text = getString(R.string.no_timer_set)
            }else{
                timeText.text = getString(R.string.minutes_tv, timerValue)

            }
            rescheduleTimer(timerValue)
        }
    }

    // add override function that will be called when apps becomes in foreground again.

    override fun onResume() {
        super.onResume()
        val workManager = WorkManager.getInstance(this)
        if (workManager.getWorkInfosByTag("sleep").get().size > 0) {
            workManager.cancelAllWorkByTag("sleep")
        }
    }

    private fun requestAdmin(sleepAdminReceiver: ComponentName, devicePolicyManager: DevicePolicyManager) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, sleepAdminReceiver)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "This app needs admin permission to lock the device"
        )
        Log.d("REQSR", "Requesting admin permission")
        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (devicePolicyManager.isAdminActive(sleepAdminReceiver)) {
                Toast.makeText(this, "Admin permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Admin permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        launcher.launch(intent)
    }

    private fun rescheduleTimer(timerValue: Int) {
        val workManager = WorkManager.getInstance(this)
        // delete all work with tag sleep if there is any and status is enqueued
        if (workManager.getWorkInfosByTag("sleep").get().size > 0) {
            workManager.cancelAllWorkByTag("sleep")
        }

        if (timerValue == 0) {
            return
        }

        val sleepTimer = OneTimeWorkRequestBuilder<SleepWorker>()
            .setInitialDelay(timerValue.toLong(), java.util.concurrent.TimeUnit.MINUTES)
            .addTag("sleep")
            .build()
        workManager.enqueue(sleepTimer)
    }
}