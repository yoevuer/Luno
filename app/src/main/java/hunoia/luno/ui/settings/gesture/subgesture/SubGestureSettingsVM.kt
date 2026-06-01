package hunoia.luno.ui.settings.gesture.subgesture

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aaron.compose.base.BaseComposeVM
import hunoia.luno.R
import hunoia.luno.config.SubGestureCleaner
import hunoia.luno.config.model.Action
import hunoia.luno.core.AppContext
import hunoia.luno.action.api.ActionFacade
import hunoia.luno.action.payload.SubGestureActionData
import hunoia.luno.core.JsonSerializer
import hunoia.luno.config.model.SubGestureDirection
import hunoia.luno.ui.navigation.SubGestureEditor
import hunoia.luno.config.ConfigProvider
import hunoia.luno.config.model.SubGesture
import hunoia.luno.config.model.SubGestureAngle
import hunoia.luno.config.model.SubGestureSettings
import hunoia.luno.bridge.vibration.VibrationEffects
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SubGestureSettingsUiState(
    val subGesture: SubGesture? = null,
    val allSubGestures: List<SubGesture>? = null,
    val showDeleteWarningDialog: Boolean = false,
    val showMirrorCopyDialog: Boolean = false,
)

sealed interface SubGestureSettingsUiEvent

class SubGestureSettingsVM(savedStateHandle: SavedStateHandle) : BaseComposeVM<SubGestureSettingsUiState, SubGestureSettingsUiEvent>() {

    private val subGestureEditor = savedStateHandle.toRoute<SubGestureEditor>()

    override val initialState: SubGestureSettingsUiState = SubGestureSettingsUiState()

    init {
        loadData()
    }

    fun showDeleteWarningDialog(show: Boolean) {
        updateUiState { it.copy(showDeleteWarningDialog = show) }
    }

    fun showMirrorCopyDialog(show: Boolean) {
        updateUiState { it.copy(showMirrorCopyDialog = show) }
    }

    fun createMirroredCopy() {
        viewModelScope.launch {
            val original = uiState.subGesture ?: return@launch
            val newId = java.util.UUID.randomUUID().toString()
            val mirrored = original.copy(
                id = newId,
                name = AppContext.get().getString(
                    R.string.mirror_sub_gesture_name,
                    original.name.ifEmpty { AppContext.get().getString(R.string.sub_gesture) }
                ),
                leftAction = original.rightAction,
                rightAction = original.leftAction,
                upRightAction = original.upLeftAction,
                upLeftAction = original.upRightAction,
                downRightAction = original.downLeftAction,
                downLeftAction = original.downRightAction,
                angle = original.angle.copy(
                    boundaries = original.angle.boundaries.let { b ->
                        listOf(3, 2, 1, 0, 7, 6, 5, 4).map { i -> ((0.5f - b[i]) + 1f) % 1f }
                    }
                ),
            )
            ConfigProvider.updateSubGestureSettings { settings ->
                settings.copy(subGestures = settings.subGestures + mirrored)
            }
        }
    }

    fun deleteSubGesture() {
        viewModelScope.launch {
            ConfigProvider.updateSubGestureSettings { settings ->
                settings.copy(
                    subGestures = settings.subGestures.filter { it.id != subGestureEditor.subGestureId }
                )
            }
            cleanReferences(subGestureEditor.subGestureId)
        }.invokeOnCompletion {
            finish()
        }
    }

    private suspend fun cleanReferences(deletedId: String) {
        SubGestureCleaner.cleanSubGestureReferences(
            deletedId = deletedId,
            shouldRemove = { SubGestureCleaner.matchesDeletedSubGesture(it, deletedId) }
        )
    }

    fun updateAngle(angle: SubGestureAngle) {
        viewModelScope.launch {
            ConfigProvider.updateSubGestureSettings { settings ->
                settings.copy(
                    subGestures = settings.subGestures.map { gesture ->
                        if (gesture.id == subGestureEditor.subGestureId) gesture.copy(angle = angle)
                        else gesture
                    }
                )
            }
        }
    }

    fun updateColor(color: Int) {
        viewModelScope.launch {
            ConfigProvider.updateSubGestureSettings { settings ->
                settings.copy(
                    subGestures = settings.subGestures.map { gesture ->
                        if (gesture.id == subGestureEditor.subGestureId) gesture.copy(color = color)
                        else gesture
                    }
                )
            }
        }
    }

    private fun updateSubGesture(fieldUpdate: SubGesture.() -> SubGesture) {
        viewModelScope.launch {
            ConfigProvider.updateSubGestureSettings { settings ->
                settings.copy(
                    subGestures = settings.subGestures.map { gesture ->
                        if (gesture.id == subGestureEditor.subGestureId) gesture.fieldUpdate()
                        else gesture
                    }
                )
            }
        }
    }

    fun onSubVibrateChange(value: Boolean) = updateSubGesture { copy(vibrate = value) }
    fun onSubVibrateImmediatelyChange(value: Boolean) = updateSubGesture { copy(vibrateImmediately = value) }
    fun onSubVibrationEffectChange(value: VibrationEffects) = updateSubGesture { copy(vibrationEffect = value) }
    fun onSubCustomVibrationMsChange(value: Float) = updateSubGesture { copy(customVibrationMs = value.toLong()) }
    fun onSubTriggerDistanceChange(value: Float) = updateSubGesture { copy(triggerDistance = value.toInt()) }
    fun onSubTimeoutMsChange(value: Float) = updateSubGesture { copy(timeoutMs = value.toLong()) }

    private fun loadData() {
        viewModelScope.launch {
            ConfigProvider.subGestureSettings.collectLatest { settings ->
                val gesture = settings.subGestures.find { it.id == subGestureEditor.subGestureId }
                updateUiState {
                    it.copy(
                        subGesture = gesture,
                        allSubGestures = settings.subGestures,
                    )
                }
            }
        }
    }

    private object App {
        lateinit var context: android.content.Context
        fun init(ctx: android.content.Context) { context = ctx }
    }
}
