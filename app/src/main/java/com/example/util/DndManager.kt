package com.example.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

object DndManager {
    private const val PREFS_NAME = "dnd_prefs"
    private const val KEY_AUTO_DND_ENABLED = "auto_dnd_enabled"
    private const val KEY_PREVIOUS_INTERRUPTION_FILTER = "prev_interruption_filter"

    fun isAutoDndEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_DND_ENABLED, true)
    }

    fun setAutoDndEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_DND_ENABLED, enabled).apply()
    }

    fun hasDndPermission(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }
    }

    fun openDndSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w("DndManager", "Failed to open DND settings: ${e.message}")
            }
        }
    }

    fun enablePriorityDnd(context: Context): Boolean {
        if (!isAutoDndEnabled(context)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        if (!nm.isNotificationPolicyAccessGranted) return false

        try {
            val currentFilter = nm.currentInterruptionFilter
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_PREVIOUS_INTERRUPTION_FILTER, currentFilter).apply()

            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            return true
        } catch (e: Exception) {
            Log.w("DndManager", "Failed to set Priority DND: ${e.message}")
            return false
        }
    }

    fun restoreDnd(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) return

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val prevFilter = prefs.getInt(
                KEY_PREVIOUS_INTERRUPTION_FILTER,
                NotificationManager.INTERRUPTION_FILTER_ALL
            )
            nm.setInterruptionFilter(prevFilter)
        } catch (e: Exception) {
            Log.w("DndManager", "Failed to restore DND: ${e.message}")
        }
    }
}
