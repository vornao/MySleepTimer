package dev.vornao.mysleeptimer

class SleepAdminReceiver : android.app.admin.DeviceAdminReceiver() {
    override fun onEnabled(context: android.content.Context, intent: android.content.Intent) {
      super.onEnabled(context, intent)
      android.widget.Toast.makeText(context,
          context.getString(R.string.device_admin_enabled), android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: android.content.Context, intent: android.content.Intent) {
        super.onDisabled(context, intent)
        android.widget.Toast.makeText(context,
            context.getString(R.string.device_admin_disabled), android.widget.Toast.LENGTH_SHORT).show()
    }
}