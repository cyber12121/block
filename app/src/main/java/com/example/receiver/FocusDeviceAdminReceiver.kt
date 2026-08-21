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
        val sessionManager = app?.sessionManager

        val isStrictActive = sessionManager?.let { it.isStrictActive() || it.isUltraStrictActive() } ?: false
        if (isStrictActive) {
            val isUltra = sessionManager?.isUltraStrictActive() == true
            val modeName = if (isUltra) "STRICT BLOCKER" else "NORMAL BLOCKER"
            return "$modeName LOCK: FocusGuard uninstallation and deactivation is strictly prohibited while focus enforcement is active."
        }
        return null
    }
}
