package hunoia.luno.service

import android.view.View
import android.view.WindowManager
import hunoia.luno.config.model.GestureButton
import hunoia.luno.config.model.AdvancedSettings
import hunoia.luno.config.model.InitialSettings
import hunoia.luno.bridge.window.setFlags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class SideGestureButtonRefreshCoordinator(
    private val host: SideGestureService,
    private val scopeProvider: () -> CoroutineScope,
    private val initialSettingsProvider: suspend () -> InitialSettings,
    private val advancedSettingsProvider: () -> AdvancedSettings?,
    private val buttonViewsProvider: () -> List<View>?,
    private val runtimeStateProvider: () -> SideGestureRuntimeState,
) {

    fun refresh() {
        scopeProvider().launch {
            val initialSettings = initialSettingsProvider()
            val advancedSettings = advancedSettingsProvider() ?: return@launch
            val refreshState = GestureButtonRefreshState(
                initialSettings = initialSettings,
                advancedSettings = advancedSettings,
                runtimeState = runtimeStateProvider(),
            )
            buttonViewsProvider()?.forEach { view ->
                val target = view.tag as? GestureButtonWindowTarget ?: return@forEach
                val lp = (view.layoutParams as WindowManager.LayoutParams).apply {
                    updateGestureButtonState(this, target, refreshState)
                }
                host.updateWindowLayout(view, lp)
            }
        }
    }

    private fun updateGestureButtonState(
        lp: WindowManager.LayoutParams,
        target: GestureButtonWindowTarget,
        state: GestureButtonRefreshState,
    ) {
        lp.updateGestureButton(target.windowButton)
        lp.setFlags(state.shouldShow(target.sourceButton))
    }
}
