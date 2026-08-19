package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.FocusGuardApp
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var loopJob: Job? = null
    private var lastNotificationText = ""
    private var lastNotificationUpdateTime = 0L

    companion object {
        const val CHANNEL_ID = "focus_guard_persistent_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_FOREGROUND"
        const val ACTION_STOP = "ACTION_STOP_FOREGROUND"

        fun startService(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("FocusGuard Protection Active", "Distraction shielding and scheduled focus active"))
        startBackgroundMonitorLoop()

        return START_STICKY
    }

    private fun startBackgroundMonitorLoop() {
        loopJob?.cancel()
        loopJob = serviceScope.launch {
            val app = application as? FocusGuardApp ?: return@launch
            val sessionManager = app.sessionManager
            val repository = app.repository

            sessionManager.refreshBlockedTargetsCache(repository)

            var scheduleCheckCounter = 0
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            while (isActive) {
                sessionManager.updateTick()
                scheduleCheckCounter++
                if (scheduleCheckCounter >= 15) { // check schedules every 15s to save CPU
                    scheduleCheckCounter = 0
                    sessionManager.checkAutomaticSchedules(repository)
                }

                val sessionState = sessionManager.sessionState.value
                val now = System.currentTimeMillis()

                if (sessionState.isActive) {
                    val minutes = sessionState.remainingSeconds / 60
                    val seconds = sessionState.remainingSeconds % 60
                    val mode = if (sessionState.isStrictMode) "Strict Lock" else if (sessionState.isPomodoro) "Pomodoro" else "Focus Active"
                    val content = "${sessionState.title} • $mode • ${String.format("%02d:%02d", minutes, seconds)} remaining"

                    // Only update notification if remaining time changes by minutes, or in last 30 seconds, or every 10s
                    if (content != lastNotificationText && (now - lastNotificationUpdateTime > 10000 || sessionState.remainingSeconds <= 30)) {
                        lastNotificationText = content
                        lastNotificationUpdateTime = now
                        notificationManager.notify(NOTIFICATION_ID, buildNotification("FocusGuard Enforced", content))
                    }
                } else {
                    val idleText = "Shield standby • 0 distractions"
                    if (idleText != lastNotificationText) {
                        lastNotificationText = idleText
                        lastNotificationUpdateTime = now
                        notificationManager.notify(NOTIFICATION_ID, buildNotification("FocusGuard Protection Active", idleText))
                    }
                }

                delay(1000)
            }
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FocusGuard Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps FocusGuard active in background to block distracting apps and websites"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartIntent = Intent(applicationContext, FocusForegroundService::class.java).apply {
            action = ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(restartIntent)
        } else {
            applicationContext.startService(restartIntent)
        }
    }

    override fun onDestroy() {
        loopJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
