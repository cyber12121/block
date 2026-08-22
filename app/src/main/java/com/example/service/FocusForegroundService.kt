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

            // Warm the blocking cache immediately on start
            sessionManager.refreshBlockedTargetsCache(repository)

            // One-time schedule check on startup
            sessionManager.checkAutomaticSchedules(repository)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            var sessionCompletionJob: Job? = null

            // Combine and react to sessionState and activeSchedulesState
            launch {
                sessionManager.sessionState.collect { sessionState ->
                    val activeSchedulesState = sessionManager.activeSchedulesState.value
                    updateNotification(notificationManager, sessionState, activeSchedulesState)

                    // Zero-wakeup battery optimization:
                    // Instead of polling every 1 second, schedule a single completion trigger at endTimeMillis
                    sessionCompletionJob?.cancel()
                    if (sessionState.isActive) {
                        val remainingMs = sessionState.endTimeMillis - System.currentTimeMillis()
                        if (remainingMs > 0) {
                            sessionCompletionJob = launch {
                                delay(remainingMs + 200L) // Wait until exact completion time
                                if (sessionManager.sessionState.value.isActive) {
                                    sessionManager.updateTick()
                                }
                            }
                        } else {
                            sessionManager.updateTick()
                        }
                    } else {
                        sessionCompletionJob = null
                    }
                }
            }

            launch {
                sessionManager.activeSchedulesState.collect { activeSchedulesState ->
                    val sessionState = sessionManager.sessionState.value
                    updateNotification(notificationManager, sessionState, activeSchedulesState)
                }
            }
        }
    }

    private fun updateNotification(
        notificationManager: NotificationManager,
        sessionState: ActiveSessionState,
        activeSchedulesState: ActiveSchedulesState
    ) {
        if (sessionState.isActive) {
            val mode = if (sessionState.isUltraStrict) "Strict Lock 🔒" else if (sessionState.isStrictMode) "Normal Lock" else if (sessionState.isPomodoro) "Pomodoro" else "Focus Active"
            val title = "${sessionState.title} • $mode"
            val content = "Protection active • Countdown in progress"

            if (title != lastNotificationText) {
                lastNotificationText = title
                val notif = buildNotification(
                    title = title,
                    text = content,
                    chronometerTargetMillis = sessionState.endTimeMillis
                )
                notificationManager.notify(NOTIFICATION_ID, notif)
            }
        } else if (activeSchedulesState.isActive) {
            val names = activeSchedulesState.activeSchedules.joinToString(", ") { it.name }
            val mode = if (activeSchedulesState.isUltraStrict) "Strict Schedule 🔒" else if (activeSchedulesState.isStrictMode) "Schedule Shield" else "Auto Schedule"
            val title = "FocusGuard Schedule Running"
            val content = "$names • $mode Active"

            if (content != lastNotificationText) {
                lastNotificationText = content
                val notif = buildNotification(
                    title = title,
                    text = content,
                    chronometerTargetMillis = if (activeSchedulesState.endTimeMillis > 0) activeSchedulesState.endTimeMillis else null
                )
                notificationManager.notify(NOTIFICATION_ID, notif)
            }
        } else {
            val idleText = "Shield standby • 0 distractions"
            if (idleText != lastNotificationText) {
                lastNotificationText = idleText
                val notif = buildNotification(
                    title = "FocusGuard Protection Active",
                    text = idleText
                )
                notificationManager.notify(NOTIFICATION_ID, notif)
            }
        }
    }

    private fun buildNotification(
        title: String,
        text: String,
        chronometerTargetMillis: Long? = null
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (chronometerTargetMillis != null && chronometerTargetMillis > System.currentTimeMillis()) {
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setWhen(chronometerTargetMillis)
        }

        return builder.build()
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
