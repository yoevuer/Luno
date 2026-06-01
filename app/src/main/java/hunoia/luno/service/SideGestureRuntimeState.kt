package hunoia.luno.service

import hunoia.luno.config.model.GestureButton
import hunoia.luno.config.model.AdvancedSettings
import hunoia.luno.config.model.InitialSettings

interface SideGestureRuntime {
    fun nowInLauncher(): Boolean
    fun requestEnableFrozenPackage(packageName: String, onResult: (Boolean) -> Unit)
}

internal data class SideGestureRuntimeState(
    val currentPackageName: String,
    val isNowInLockScreenPage: Boolean,
    val isLandscape: Boolean,
    val isInLauncher: Boolean,
    val isKeyboardInputActive: Boolean,
    val hiddenGestureButtons: Map<String, Long>,
    val isMouseMode: Boolean,
    val nowMs: Long,
)

internal data class GestureButtonRefreshState(
    val initialSettings: InitialSettings,
    val advancedSettings: AdvancedSettings,
    val runtimeState: SideGestureRuntimeState,
) {
    fun shouldShow(button: GestureButton): Boolean {
        return initialSettings.gestureEnabled &&
            (runtimeState.hiddenGestureButtons[button.hiddenKey()] ?: 0L) <= runtimeState.nowMs &&
            !runtimeState.isMouseMode &&
            !(button.fitSoftKeyboard && runtimeState.isKeyboardInputActive) &&
            !(button.hideLandscape && runtimeState.isLandscape) &&
            !(button.hideHomeScreen && runtimeState.isInLauncher) &&
            !(button.hideScreenLock && runtimeState.isNowInLockScreenPage) &&
            runtimeState.currentPackageName !in advancedSettings.excludeApps &&
            button.enabled
    }
}

internal fun GestureButton.hiddenKey(): String = id
