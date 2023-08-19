package dev.vornao.mysleeptimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager

class SleepDismissReceiver :BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("SleepDismissReceiver", "onReceive: ")
        val workManager = WorkManager.getInstance(context!!)
        workManager.cancelAllWorkByTag("sleep")
        // cancel notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(1)
        //show toast
        android.widget.Toast.makeText(context, "Sleep timer dismissed", android.widget.Toast.LENGTH_SHORT).show()
    }
}