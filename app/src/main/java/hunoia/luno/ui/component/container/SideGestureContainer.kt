package hunoia.luno.ui.component.container

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import hunoia.luno.action.api.ActionFacade
import hunoia.luno.config.model.Action
import hunoia.luno.config.model.ActionPanelStyle
import hunoia.luno.config.model.ActionSettings
import hunoia.luno.config.model.AdvancedSettings
import hunoia.luno.config.model.ArcStyle
import hunoia.luno.config.model.GestureSettings
import hunoia.luno.config.model.GestureButton
import hunoia.luno.config.model.GestureButtonActionSettingsOverride
import hunoia.luno.config.model.SubGestureSettings
import hunoia.luno.config.model.effectiveFor
import hunoia.luno.gesture.DragGestureHandler
import hunoia.luno.gesture.GestureFacade
import hunoia.luno.ui.component.panel.ActionPanel
import hunoia.luno.ui.component.panel.rememberActionPanelState
import hunoia.luno.gesture.SideGestureState
import hunoia.luno.gesture.SubGestureState
import hunoia.luno.gesture.VolumeScrubState
import hunoia.luno.pointer.rememberPointerHandle
import kotlin.math.roundToInt

@Composable
fun SideGestureContainer(
    onAction: (Action, GestureButton?, GestureButtonActionSettingsOverride?) -> Unit,
    buttons: List<GestureButton>,
    modifier: Modifier = Modifier,
    imePadding: Int = 0,
    actionPanelStyle: ActionPanelStyle = ArcStyle(),
    actionSettings: ActionSettings = ActionSettings(),
    advancedSettings: AdvancedSettings = AdvancedSettings(),
    gestureSettings: GestureSettings = GestureSettings(),
    onPointerStart: (GestureSettings.Pointer) -> Boolean = { false },
    onPointerEnd: () -> Unit = {},
    onPointerActionAtPosition: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    subGestureSettings: SubGestureSettings = SubGestureSettings(),
    onSubGestureModeChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val curOnAction by rememberUpdatedState(newValue = onAction)
    val curOnSubGestureModeChanged by rememberUpdatedState(newValue = onSubGestureModeChanged)
    val coroutineScope = rememberCoroutineScope()
    val sideGestureState = rememberSideGestureState(buttons, advancedSettings, gestureSettings)
    val actionPanelState = rememberActionPanelState()
    var pointerStartedFromSubGesture by remember { mutableStateOf(false) }
    val pointerHandle = rememberPointerHandle(
        gestureSettings = gestureSettings,
        onPointerStart = onPointerStart,
        shouldPreservePointerCancel = {
            pointerStartedFromSubGesture
        },
        onPointerActionAtPosition = onPointerActionAtPosition,
        onPointerEnd = onPointerEnd,
    )
    val subGestureState = remember(subGestureSettings, coroutineScope) {
        SubGestureState(coroutineScope, subGestureSettings, curOnSubGestureModeChanged)
    }
    val volumeScrubState = remember(context) {
        VolumeScrubState(context, curOnSubGestureModeChanged)
    }

    fun effectiveActionSettings(override: GestureButtonActionSettingsOverride?): ActionSettings = actionSettings.effectiveFor(override)

    fun effectiveActionSettings(button: GestureButton?): ActionSettings = effectiveActionSettings(button?.actionSettingsOverride)

    fun effectivePointerSettings(override: GestureButtonActionSettingsOverride?): GestureSettings.Pointer {
        return gestureSettings.effectiveFor(override).pointer
    }

    fun effectivePointerSettings(button: GestureButton?): GestureSettings.Pointer {
        return effectivePointerSettings(button?.actionSettingsOverride)
    }

    fun handleResolvedAction(
        action: Action,
        sourceButton: GestureButton?,
        touchPosition: Offset,
        sourceOverride: GestureButtonActionSettingsOverride? = sourceButton?.actionSettingsOverride,
    ) {
        if (subGestureState.tryEnterSubGesture(action)) return
        val resolvedAction = if (!touchPosition.x.isFinite() || !touchPosition.y.isFinite()) action
        else Action(value = action.value, data = action.data, extra = listOf(touchPosition.x.roundToInt(), touchPosition.y.roundToInt()), longPressAction = action.longPressAction)
        curOnAction(resolvedAction, sourceButton, sourceOverride)
    }

    SideEffect {
        sideGestureState.onLongPress = { action ->
            handleResolvedAction(action, sideGestureState.button, sideGestureState.finger)
            sideGestureState.cancel()
        }
    }

    DragGestureHandler(
        onDragStart = onDragStart@{ offset ->
            if (subGestureState.isActive) {
                subGestureState.onDragStart()
                return@onDragStart
            }
            sideGestureState.onDragStart(offset, imePadding)
        },
        onDrag = onDrag@{ dragAmount ->
            if (subGestureState.isActive) {
                val resolvedAction = subGestureState.onDrag(dragAmount)
                if (resolvedAction != null) {
                    val sourceOverride = subGestureState.lastResolvedActionSubGesture?.actionSettingsOverride
                    when (resolvedAction.value) {
                        ActionFacade.VOLUME_SCRUB -> {
                            volumeScrubState.activate(effectiveActionSettings(sourceOverride))
                            sideGestureState.cancel()
                            subGestureState.clear(notifyService = false)
                        }
                        ActionFacade.POINTER -> {
                            val started = pointerHandle.start(effectivePointerSettings(sourceOverride), sideGestureState.finger)
                            sideGestureState.cancel()
                            if (started) {
                                pointerStartedFromSubGesture = true
                                subGestureState.clear(notifyService = false)
                            } else {
                                pointerStartedFromSubGesture = false
                                subGestureState.clear()
                            }
                        }
                        ActionFacade.SUB_GESTURE -> {
                            handleResolvedAction(resolvedAction, sideGestureState.button, sideGestureState.finger, sourceOverride)
                        }
                        ActionFacade.NONE -> {
                            sideGestureState.cancel()
                            subGestureState.clear()
                        }
                        else -> {
                            handleResolvedAction(resolvedAction, sideGestureState.button, sideGestureState.finger, sourceOverride)
                            sideGestureState.cancel()
                            subGestureState.clear()
                        }
                    }
                }
                if (resolvedAction == null && !subGestureState.isActive) {
                    sideGestureState.cancel()
                    subGestureState.clear()
                }
                return@onDrag
            }
            if (pointerHandle.isActive) {
                if (!pointerHandle.onDrag(dragAmount)) return@onDrag
                return@onDrag
            }
            if (volumeScrubState.isActive) {
                volumeScrubState.onDrag(dragAmount)
                return@onDrag
            }
            if (actionPanelState.visible) {
                actionPanelState.onDrag(dragAmount)
                return@onDrag
            }
            if (!sideGestureState.isCanceled) {
                val actions = sideGestureState.onDrag(dragAmount)
                val button = sideGestureState.button
                if (button != null && actions != null) {
                    if (actions.size > 1) {
                        actionPanelState.onDragStart(sideGestureState.finger)
                        actionPanelState.ready(
                            sideGestureState.triggerDirection,
                            actions,
                            button.longSlideActionPanelStyles.let { GestureFacade.styleBy(it, sideGestureState.actionDirection) }.value,
                            button.color
                        )
                        sideGestureState.cancel()
                    } else if (actions.isNotEmpty()) {
                        val action = actions.first()
                        when (action.value) {
                            ActionFacade.VOLUME_SCRUB -> {
                                volumeScrubState.activate(effectiveActionSettings(button))
                                sideGestureState.cancel()
                            }
                            ActionFacade.POINTER -> {
                                pointerHandle.start(effectivePointerSettings(button), sideGestureState.finger)
                                sideGestureState.cancel()
                            }
                            else -> {
                                handleResolvedAction(action, button, sideGestureState.finger)
                                sideGestureState.cancel()
                            }
                        }
                    }
                } else {
                    sideGestureState.cancel()
                }
            }
        },
        onDragEnd = onDragEnd@{
            if (subGestureState.isActive) {
                subGestureState.onDragEnd()
                return@onDragEnd
            }
            if (pointerStartedFromSubGesture && !pointerHandle.isActive) {
                pointerStartedFromSubGesture = false
                curOnSubGestureModeChanged(false)
                return@onDragEnd
            }
            if (pointerHandle.isActive) {
                val fromSubGesture = pointerStartedFromSubGesture
                pointerHandle.onDragEnd()
                if (fromSubGesture) {
                    pointerStartedFromSubGesture = false
                    curOnSubGestureModeChanged(false)
                } else {
                    curOnSubGestureModeChanged(false)
                }
                return@onDragEnd
            }
            if (volumeScrubState.isActive) {
                volumeScrubState.onDragEnd()
                return@onDragEnd
            }
            if (actionPanelState.visible) {
                val touchPosition = actionPanelState.finger
                val action = actionPanelState.done()
                actionPanelState.onDragEnd()
                handleResolvedAction(action, sideGestureState.button, touchPosition)
            }
            if (!sideGestureState.isCanceled) {
                val touchPosition = sideGestureState.finger
                val sourceButton = sideGestureState.button
                val action = sideGestureState.onDragEnd()
                handleResolvedAction(action, sourceButton, touchPosition)
            }
        },
        onDragCancel = onDragCancel@{
            if (subGestureState.isActive) {
                subGestureState.onDragCancel()
                return@onDragCancel
            }
            if (pointerHandle.isActive) {
                if (pointerStartedFromSubGesture) return@onDragCancel
                pointerStartedFromSubGesture = false
                curOnSubGestureModeChanged(false)
                pointerHandle.onDragCancel()
                return@onDragCancel
            }
            if (volumeScrubState.isActive) {
                volumeScrubState.onDragCancel()
                return@onDragCancel
            }
            if (actionPanelState.visible) {
                actionPanelState.onDragCancel()
            }
            sideGestureState.onDragCancel()
        }
    )
    Box(modifier = modifier) {
        ActionPanel(
            actionPanelStyle = actionPanelStyle,
            actionPanelState = actionPanelState,
            modifier = Modifier.matchParentSize(),
            gestureSettings = gestureSettings
        )
    }
}

@Composable
internal fun rememberSideGestureState(
    buttons: List<GestureButton>,
    advancedSettings: AdvancedSettings = AdvancedSettings(),
    gestureSettings: GestureSettings = GestureSettings()
): SideGestureState {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope, buttons, advancedSettings, gestureSettings) {
        SideGestureState(coroutineScope, buttons, advancedSettings, gestureSettings)
    }
}

abstract class LongSlideState {

    var origin: Offset by mutableStateOf(Offset.Unspecified)
        protected set
    var finger: Offset by mutableStateOf(Offset.Unspecified)
        protected set

    open fun onDragStart(offset: Offset) {
        origin = offset
        finger = offset
    }

    open fun onDrag(dragAmount: Offset) {
        finger += dragAmount
    }

    open fun onDragEnd() {
        reset()
    }

    open fun onDragCancel() {
        reset()
    }

    protected open fun reset() {
        origin = Offset.Unspecified
        finger = Offset.Unspecified
    }
}
