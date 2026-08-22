package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.util.InstalledAppsCache

/**
 * BroadcastReceiver listening for package additions, removals, and updates.
 * Invalidates the InstalledAppsCache so the app drawer and blocking lists
 * immediately reflect newly installed or uninstalled applications.
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        InstalledAppsCache.invalidate()
    }
}
