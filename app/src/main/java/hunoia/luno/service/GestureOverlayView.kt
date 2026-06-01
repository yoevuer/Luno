package hunoia.luno.service

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hunoia.luno.bridge.WallpaperChangedEvent
import hunoia.luno.config.model.ActionSettings
import hunoia.luno.config.model.AdvancedSettings
import hunoia.luno.config.model.GestureSettings
import hunoia.luno.config.model.GestureButtonActionSettingsOverride
import hunoia.luno.config.model.SubGestureSettings
import hunoia.luno.config.ConfigProvider
import hunoia.luno.ui.component.container.SideGestureContainer
import hunoia.luno.core.Events
import hunoia.luno.core.Events.SubscribeEvent
import hunoia.luno.ui.theme.SideGestureTheme

@Composable
fun GestureOverlayView(
    screenshotService: SideGestureService,
    onSubGestureModeChanged: (Boolean) -> Unit,
    onAction: (hunoia.luno.config.model.Action, hunoia.luno.config.model.GestureButton?, GestureButtonActionSettingsOverride?) -> Unit,
    onPointerStart: (GestureSettings.Pointer) -> Boolean,
    onPointerEnd: () -> Unit,
    onPointerActionAtPosition: (Int, Int, Boolean) -> Unit,
    windowController: SideGestureWindowController,
) {
    var lastWallpaperChangeMs by remember { mutableStateOf(0L) }
    SubscribeEvent(eventClass = WallpaperChangedEvent::class) {
        val now = System.currentTimeMillis()
        if (now - lastWallpaperChangeMs < 500L) return@SubscribeEvent
        lastWallpaperChangeMs = now
    }
    val themeKey = lastWallpaperChangeMs.toString()
    SideGestureTheme(wallpaperChangeTrigger = themeKey) {
        Box(modifier = Modifier.fillMaxSize()) {
            val gestureButtons by ConfigProvider
                .gestureButtons
                .collectAsStateWithLifecycle(initialValue = emptyList())
            val advancedSettings by ConfigProvider
                .advancedSettings
                .collectAsStateWithLifecycle(initialValue = AdvancedSettings())
            val gestureSettings by ConfigProvider
                .gestureSettings
                .collectAsStateWithLifecycle(initialValue = GestureSettings())
            val actionSettings by ConfigProvider
                .actionSettings
                .collectAsStateWithLifecycle(initialValue = ActionSettings())
            val subGestureSettings by ConfigProvider
                .subGestureSettings
                .collectAsStateWithLifecycle(initialValue = SubGestureSettings())
            SideGestureContainer(
                modifier = Modifier.matchParentSize(),
                buttons = gestureButtons,
                onSubGestureModeChanged = onSubGestureModeChanged,
                onAction = onAction,
                onPointerStart = onPointerStart,
                onPointerEnd = onPointerEnd,
                onPointerActionAtPosition = onPointerActionAtPosition,
                actionSettings = actionSettings,
                advancedSettings = advancedSettings,
                gestureSettings = gestureSettings,
                subGestureSettings = subGestureSettings,
            )
        }
    }
}
