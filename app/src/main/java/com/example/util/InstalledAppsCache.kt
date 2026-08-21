package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val iconBitmap: Bitmap?,
    val category: String,
    val isSystemApp: Boolean
)

object InstalledAppsCache {
    @Volatile
    private var cachedList: List<InstalledAppItem> = emptyList()
    @Volatile
    private var isLoaded: Boolean = false

    fun getCachedApps(): List<InstalledAppItem> = cachedList

    fun isReady(): Boolean = isLoaded && cachedList.isNotEmpty()

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    suspend fun loadApps(context: Context): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        if (isLoaded && cachedList.isNotEmpty()) {
            return@withContext cachedList
        }

        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        val apps = mutableListOf<InstalledAppItem>()
        val seenPackages = mutableSetOf<String>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val name = info.loadLabel(pm).toString()
            val iconDrawable = try {
                info.loadIcon(pm)
            } catch (_: Exception) {
                null
            }
            val bitmap = iconDrawable?.let { drawableToBitmap(it) }

            var category = "Other"
            val pkgLower = pkg.lowercase()
            val nameLower = name.lowercase()

            if (pkgLower.contains("instagram") || pkgLower.contains("facebook") || pkgLower.contains("twitter") ||
                pkgLower.contains("tiktok") || pkgLower.contains("snapchat") || pkgLower.contains("reddit") ||
                pkgLower.contains("discord") || pkgLower.contains("telegram") || pkgLower.contains("whatsapp") ||
                nameLower.contains("social") || nameLower.contains("chat")
            ) {
                category = "Social"
            } else if (pkgLower.contains("youtube") || pkgLower.contains("netflix") || pkgLower.contains("spotify") ||
                pkgLower.contains("twitch") || pkgLower.contains("disney") || pkgLower.contains("hulu") ||
                pkgLower.contains("primevideo") || nameLower.contains("stream") || nameLower.contains("video")
            ) {
                category = "Video"
            } else if (pkgLower.contains("game") || nameLower.contains("game") ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info.activityInfo.applicationInfo.category == ApplicationInfo.CATEGORY_GAME)
            ) {
                category = "Games"
            } else if (pkgLower.contains("amazon") || pkgLower.contains("ebay") || pkgLower.contains("shopping") ||
                pkgLower.contains("shein") || pkgLower.contains("temu") || pkgLower.contains("aliexpress")
            ) {
                category = "Shopping"
            } else if (pkgLower.contains("chrome") || pkgLower.contains("browser") || pkgLower.contains("firefox") ||
                pkgLower.contains("opera") || pkgLower.contains("edge")
            ) {
                category = "Browsers"
            }

            val isSys = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            apps.add(InstalledAppItem(name, pkg, bitmap, category, isSys))
        }

        apps.sortBy { it.appName.lowercase() }
        cachedList = apps
        isLoaded = true
        return@withContext apps
    }
}
