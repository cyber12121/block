package com.example.util

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityServiceInfo
import androidx.core.app.NotificationManagerCompat
import com.example.receiver.FocusDeviceAdminReceiver
import com.example.service.FocusAccessibilityService

data class PermissionStatus(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val actionLabel: String,
    val intentAction: String
)

object PermissionUtils {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        // Primary check: AccessibilityManager service list (works on all OEMs including Samsung OneUI)
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            val enabledServices = am?.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            ) ?: emptyList()
            val isRunningViaManager = enabledServices.any {
                it.resolveInfo.serviceInfo.packageName == context.packageName
            }
            if (isRunningViaManager) return true

            // Fallback: parse Settings.Secure string (stock Android / AOSP)
            val enabledString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledString)
            val myService = ComponentName(context, FocusAccessibilityService::class.java).flattenToString()
            val shortService = "${context.packageName}/.service.FocusAccessibilityService"

            while (colonSplitter.hasNext()) {
                val service = colonSplitter.next()
                if (service.equals(myService, ignoreCase = true) ||
                    service.equals(shortService, ignoreCase = true) ||
                    (service.contains(context.packageName) && service.contains("FocusAccessibilityService"))
                ) {
                    return true
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val adminComponent = ComponentName(context, FocusDeviceAdminReceiver::class.java)
            dpm?.isAdminActive(adminComponent) == true
        } catch (_: Exception) {
            false
        }
    }

    fun isNotificationGranted(context: Context): Boolean {
        return try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (_: Exception) {
            true
        }
    }

    fun isOverlayGranted(context: Context): Boolean {
        return try {
            Settings.canDrawOverlays(context)
        } catch (_: Exception) {
            false
        }
    }

    fun isBatteryOptimizationExempt(context: Context): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                ?: return false
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } catch (_: Exception) {
            false
        }
    }

    fun getBatteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getAllPermissions(context: Context): List<PermissionStatus> {
        val isA11y = isAccessibilityServiceEnabled(context)
        val isAdmin = isDeviceAdminActive(context)
        val isNotif = isNotificationGranted(context)
        val isOverlay = isOverlayGranted(context)
        val isBatteryExempt = isBatteryOptimizationExempt(context)

        return listOf(
            PermissionStatus(
                id = "a11y",
                title = "Accessibility Shield Service",
                description = "Enforces real-time blocking for distracting apps, keywords and websites",
                isGranted = isA11y,
                actionLabel = if (isA11y) "Configured" else "Enable Service",
                intentAction = Settings.ACTION_ACCESSIBILITY_SETTINGS
            ),
            PermissionStatus(
                id = "admin",
                title = "Device Admin Uninstall Lock",
                description = "Prevents uninstalling or force-stopping FocusGuard during Strict Mode",
                isGranted = isAdmin,
                actionLabel = if (isAdmin) "Protected" else "Activate Lock",
                intentAction = DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
            ),
            PermissionStatus(
                id = "overlay",
                title = "Draw Over Other Apps",
                description = "Displays the focus shield barrier when restricted content is accessed",
                isGranted = isOverlay,
                actionLabel = if (isOverlay) "Allowed" else "Allow Overlay",
                intentAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            ),
            PermissionStatus(
                id = "notif",
                title = "Notification & Strict Alerts",
                description = "Shows live countdown timer in notification bar and intervention reminders",
                isGranted = isNotif,
                actionLabel = if (isNotif) "Enabled" else "Allow Alerts",
                intentAction = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ),
            PermissionStatus(
                id = "battery",
                title = "Battery Optimization Exempt",
                description = "Keeps FocusGuard alive on Xiaomi, Samsung & Realme when screen is off",
                isGranted = isBatteryExempt,
                actionLabel = if (isBatteryExempt) "Exempt" else "Exempt App",
                intentAction = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            )
        )
    }
}
