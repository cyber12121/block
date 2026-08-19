package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.FocusGuardApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as? FocusGuardApp ?: return
            val sessionManager = FocusSessionManager.getInstance(context)

            try {
                FocusForegroundService.startService(context)
            } catch (_: Exception) {}

            CoroutineScope(Dispatchers.IO).launch {
                // Refresh cached targets to restore live blocking enforcement immediately after boot
                sessionManager.refreshBlockedTargetsCache(app.repository)
            }
        }
    }
}
