package hunoia.luno.config.model

import android.graphics.Color
import androidx.annotation.Keep
import hunoia.luno.bridge.DensityProvider
import hunoia.luno.config.defaults.GestureSettingsDefaults.SubGestureTimeoutMs
import hunoia.luno.config.model.SubGestureDirection
import hunoia.luno.bridge.vibration.VibrationEffects
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class SubGesture(
    val id: String,
    val name: String = "",
    val angle: SubGestureAngle = SubGestureAngle(),
    val upAction: Action? = null,
    val downAction: Action? = null,
    val leftAction: Action? = null,
    val rightAction: Action? = null,
    val upRightAction: Action? = null,
    val downRightAction: Action? = null,
    val downLeftAction: Action? = null,
    val upLeftAction: Action? = null,
    val enabled: Boolean = true,
    val color: Int = Color.TRANSPARENT,
    val vibrate: Boolean = true,
    val vibrateImmediately: Boolean = false,
    val vibrationEffect: VibrationEffects = VibrationEffects.Click,
    val customVibrationMs: Long = 50L,
    val actionSettingsOverride: GestureButtonActionSettingsOverride = GestureButtonActionSettingsOverride(),
    val timeoutMs: Long = SubGestureTimeoutMs,
    val triggerDistance: Int = DensityProvider.dp2px(30f),
) {
    fun actionFor(direction: SubGestureDirection): Action? = when (direction) {
        SubGestureDirection.Up -> upAction
        SubGestureDirection.Down -> downAction
        SubGestureDirection.Left -> leftAction
        SubGestureDirection.Right -> rightAction
        SubGestureDirection.UpRight -> upRightAction
        SubGestureDirection.DownRight -> downRightAction
        SubGestureDirection.DownLeft -> downLeftAction
        SubGestureDirection.UpLeft -> upLeftAction
    }

    fun withAction(direction: SubGestureDirection, action: Action?): SubGesture = when (direction) {
        SubGestureDirection.Up -> copy(upAction = action)
        SubGestureDirection.Down -> copy(downAction = action)
        SubGestureDirection.Left -> copy(leftAction = action)
        SubGestureDirection.Right -> copy(rightAction = action)
        SubGestureDirection.UpRight -> copy(upRightAction = action)
        SubGestureDirection.DownRight -> copy(downRightAction = action)
        SubGestureDirection.DownLeft -> copy(downLeftAction = action)
        SubGestureDirection.UpLeft -> copy(upLeftAction = action)
    }
}
