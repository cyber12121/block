package com.example.service

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.FocusGuardApp
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class FocusTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val app = application as? FocusGuardApp ?: return
        val sessionManager = app.sessionManager
        val currentState = sessionManager.sessionStateFlow.value

        if (currentState.isActive) {
            // End session if not strict
            if (!currentState.isStrictMode) {
                sessionManager.stopSession(earlyUnlocked = false)
            }
        } else {
            // Start 25-min Quick Focus block
            scope.launch {
                val enabledLists = app.repository.getActiveLists()
                val listNames = enabledLists.map { it.name }
                sessionManager.startSession(
                    title = "Quick Focus (Tile)",
                    durationMinutes = 25,
                    isStrictMode = false,
                    activeListNames = listNames,
                    isAutoScheduled = false
                )
                updateTileState()
            }
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val app = application as? FocusGuardApp ?: return
        val sessionState = app.sessionManager.sessionStateFlow.value

        if (sessionState.isActive) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Focus: ${sessionState.remainingSeconds / 60}m"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (sessionState.isStrictMode) "Strict Shield" else "Active Focus"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "FocusGuard"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Tap for 25m Focus"
            }
        }
        tile.updateTile()
    }

    companion object {
        fun requestTileUpdate(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    TileService.requestListeningState(
                        context.applicationContext,
                        ComponentName(context.applicationContext, FocusTileService::class.java)
                    )
                } catch (_: Exception) {}
            }
        }
    }
}
