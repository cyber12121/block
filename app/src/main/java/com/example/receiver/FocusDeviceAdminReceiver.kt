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

        val isStrictActive = sessionState?.isActive == true && (sessionState.isStrictMode || sessionState.isUltraStrict)
        if (isStrictActive) {
            val remainingMins = (sessionState.remainingSeconds / 60).coerceAtLeast(1)
            val modeName = if (sessionState.isUltraStrict) "STRICT BLOCKER" else "NORMAL BLOCKER"
            return "$modeName LOCK: FocusGuard uninstallation and deactivation is strictly prohibited until the active focus timer concludes ($remainingMins min remaining)."
        }
        return null
    }
}
