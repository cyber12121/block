package com.example.util

import android.accessibilityservice.AccessibilityServiceInfo
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

    /**
     * BlockIT-style hard fallback: force-lock the device screen via Device Admin.
     * Used when the accessibility bounce-back fails to keep the user inside the
     * minimalist strict mode. Requires FocusGuard to be an active device admin.
     */
    fun lockScreen(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val adminComponent = ComponentName(context, FocusDeviceAdminReceiver::class.java)
            if (dpm != null && dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
            }
        } catch (_: Exception) {
            // ignore - best effort only
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

    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                try {
                    val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(appDetailsIntent)
                } catch (_: Exception) {
                    val generalSettings = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(generalSettings)
                }
            }
        }
    }

    fun isUsageAccessGranted(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val generalSettings = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(generalSettings)
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
        val isUsageAccess = isUsageAccessGranted(context)

        return listOf(
            PermissionStatus(
                id = "battery",
                title = "Battery Optimization",
                description = "Exempts app from background battery restrictions",
                isGranted = isBatteryExempt,
                actionLabel = if (isBatteryExempt) "Active" else "Exempt Battery",
                intentAction = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            ),
            PermissionStatus(
                id = "usage",
                title = "Usage Stats Access",
                description = "Monitors app usage duration and status per person",
                isGranted = isUsageAccess,
                actionLabel = if (isUsageAccess) "Active" else "Allow Usage Stats",
                intentAction = Settings.ACTION_USAGE_ACCESS_SETTINGS
            ),
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
                id = "autostart",
                title = "Auto-Start & Background Protection",
                description = "Ensures MIUI, ColorOS and OEM battery savers do not terminate the blocker",
                isGranted = isBatteryExempt,
                actionLabel = "Configure OEM",
                intentAction = "ACTION_OEM_AUTOSTART"
            )
        )
    }

    fun openOemAutostartSettings(context: Context): Boolean {
        val autostartIntents = listOf(
            // Xiaomi / MIUI / HyperOS
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")),
            // Huawei / Honor
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            // Oppo / Realme / ColorOS
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            // Vivo
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            // Samsung
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"))
        )

        for (intent in autostartIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: Exception) {}
        }

        // Fallback to standard app details
        return try {
            val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appDetails)
            true
        } catch (_: Exception) {
            false
        }
    }
}
