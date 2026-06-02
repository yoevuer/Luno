package hunoia.luno.runtime.settings

import hunoia.luno.config.ConfigProvider
import hunoia.luno.config.model.ActionSettings
import hunoia.luno.config.model.AdvancedSettings
import hunoia.luno.config.model.GestureButton
import hunoia.luno.config.model.GestureSettings
import hunoia.luno.config.model.InitialSettings
import hunoia.luno.config.model.SubGestureSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsStore(
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch(Dispatchers.Main.immediate) {
            combine(
                ConfigProvider.gestureButtons,
                ConfigProvider.advancedSettings,
                ConfigProvider.gestureSettings,
                ConfigProvider.actionSettings,
                ConfigProvider.initialSettings,
                ConfigProvider.subGestureSettings,
            ) { values ->
                SettingsState(
                    gestureButtons = values[0] as List<GestureButton>,
                    advancedSettings = values[1] as AdvancedSettings,
                    gestureSettings = values[2] as GestureSettings,
                    actionSettings = values[3] as ActionSettings,
                    initialSettings = values[4] as InitialSettings,
                    subGestureSettings = values[5] as SubGestureSettings,
                )
            }.collectLatest { _state.value = it }
        }
    }

    fun snapshot(): SettingsState = _state.value
}
