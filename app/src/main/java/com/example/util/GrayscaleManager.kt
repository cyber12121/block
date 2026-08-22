package com.example.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

object GrayscaleManager {
    private const val PREFS_NAME = "grayscale_prefs"
    private const val KEY_BEDTIME_GRAYSCALE = "bedtime_grayscale_enabled"
    private const val KEY_MONOCHROME_LAUNCHER = "monochrome_launcher_enabled"

    fun isBedtimeGrayscaleEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BEDTIME_GRAYSCALE, false)
    }

    fun setBedtimeGrayscaleEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BEDTIME_GRAYSCALE, enabled).apply()
    }

    fun isMonochromeLauncherEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MONOCHROME_LAUNCHER, false)
    }

    fun setMonochromeLauncherEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MONOCHROME_LAUNCHER, enabled).apply()
    }

    fun openColorCorrectionSettings(context: Context) {
        try {
            // Opens System Accessibility -> Color Correction / Monochrome settings
            val intent = Intent("android.settings.COLOR_INVERSION_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w("GrayscaleManager", "Failed to open Color Correction settings: ${e.message}")
            }
        }
    }
}
