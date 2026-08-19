package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.FocusGuardApp

class FocusDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "FocusGuard Anti-Uninstall Protection Activated", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "FocusGuard Anti-Uninstall Protection Deactivated", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        val app = context.applicationContext as? FocusGuardApp
        val sessionState = app?.sessionManager?.sessionStateFlow?.value

        if (sessionState?.isActive == true && sessionState.isStrictMode) {
            val remainingMins = (sessionState.remainingSeconds / 60).coerceAtLeast(1)
            return "STRICT MODE LOCK: FocusGuard uninstallation and deactivation is strictly prohibited until the active focus timer concludes ($remainingMins min remaining)."
        }
        return null
    }
}
