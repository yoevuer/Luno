package hunoia.luno.service

import hunoia.luno.config.model.GestureButton
import hunoia.luno.config.ConfigProvider
import hunoia.luno.config.model.ActionSettings
import hunoia.luno.config.model.AdvancedSettings
import hunoia.luno.config.model.GestureSettings
import hunoia.luno.config.model.InitialSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class SideGestureSettingsObserver(
    private val scope: CoroutineScope,
    private val onInitialSettings: (InitialSettings) -> Unit,
    private val onAdvancedSettings: (AdvancedSettings) -> Unit,
    private val onGestureSettings: (GestureSettings) -> Unit,
    private val onActionSettings: (ActionSettings) -> Unit,
    private val onGestureButtons: (Collection<GestureButton>) -> Unit,
    private val onRefreshGestureButtons: () -> Unit
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch(Dispatchers.Main.immediate) {
            observeLatestSettings()
            observeGestureButtonChanges()
            observeGestureVisibilityChanges()
        }
    }

    private fun CoroutineScope.observeLatestSettings() {
        launch { ConfigProvider.initialSettings.collectLatest(onInitialSettings) }
        launch { ConfigProvider.advancedSettings.collectLatest(onAdvancedSettings) }
        launch { ConfigProvider.gestureSettings.collectLatest(onGestureSettings) }
        launch { ConfigProvider.actionSettings.collectLatest(onActionSettings) }
    }

    private fun CoroutineScope.observeGestureButtonChanges() {
        launch {
            ConfigProvider
                .gestureButtons
                .collectLatest(onGestureButtons)
        }
    }

    private fun CoroutineScope.observeGestureVisibilityChanges() {
        launch {
            ConfigProvider
                .initialSettings
                .distinctUntilChangedBy { it.gestureEnabled }
                .collectLatest { onRefreshGestureButtons() }
        }
    }

}
