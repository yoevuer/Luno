package hunoia.luno.gesture

import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import hunoia.luno.config.model.Action
import hunoia.luno.config.model.AdvancedSettings
import hunoia.luno.config.model.GestureButton
import hunoia.luno.config.model.GestureDirection
import hunoia.luno.config.model.GestureSettings
import hunoia.luno.config.model.GestureTriggerType
import hunoia.luno.core.AppContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.hypot

data class GestureResolvedActions(
    val button: GestureButton,
    val direction: GestureDirection,
    val actionDirection: GestureDirection,
    val actions: List<Action>,
    val triggerType: GestureTriggerType,
    val touchPosition: Offset,
)

class SideGestureState(
    private val coroutineScope: CoroutineScope,
    private val buttons: List<GestureButton>,
    private val advancedSettings: AdvancedSettings = AdvancedSettings(),
    private val gestureSettings: GestureSettings = GestureSettings()
) {

    var isCanceled: Boolean by mutableStateOf(false)
        private set

    var button: GestureButton? by mutableStateOf(null)
        private set
    var effectiveButton: GestureButton? by mutableStateOf(null)
        private set
    var triggerDirection: GestureDirection by mutableStateOf(GestureDirection.Right)
        private set
    var actionDirection: GestureDirection by mutableStateOf(GestureDirection.Right)
        private set

    var origin = Offset.Unspecified
        private set
    var finger = Offset.Unspecified
        private set

    data class AnimState(
        val originX: Float = Float.NaN,
        val originY: Float = Float.NaN,
        val fingerX: Float = Float.NaN,
        val fingerY: Float = Float.NaN,
    )

    private var animState by mutableStateOf(AnimState())

    val originXAnimVal: Float get() = animState.originX
    val originYAnimVal: Float get() = animState.originY
    val fingerXAnimVal: Float get() = animState.fingerX
    val fingerYAnimVal: Float get() = animState.fingerY

    var onResolved: (GestureResolvedActions) -> Unit = {}

    private var longSlideFirstTriggerMs = 0L
    private var slideHoldFirstTriggerMs = 0L
    private var longSlideHoldFirstTriggerMs = 0L
    private var calcLongPressJob: Job? = null
    private var pendingTapJob: Job? = null
    private var pendingTapButtonId: String? = null
    private var directTriggered = false

    private val curStickySlideValue: Float
        get() = stickySlideValue()

    private var isOhoGestureEverCanTriggered = false

    private var slideVibrationFlags = false
    private var isMirrorTouchTarget = false

    private val viewConfiguration = ViewConfiguration.get(AppContext.get())

    fun onDragStart(offset: Offset, imePadding: Int) {
        isCanceled = false
        origin = offset
        finger = offset
        val touchTarget = buttons.findTouchTarget(offset, imePadding)
        button = touchTarget?.sourceButton
        effectiveButton = touchTarget?.effectiveButton
        isMirrorTouchTarget = touchTarget?.isMirror == true

        val button = button ?: run {
            effectiveButton = null
            animState = AnimState()
            return
        }
        if (pendingTapJob?.isActive == true && pendingTapButtonId == button.id) {
            pendingTapJob?.cancel()
        }
        if (button.longPressActions.hasMeaningfulActions()) {
            calcLongPressJob = coroutineScope.launch {
                delay(button.longPressTriggerDelayMs)
                if (directTriggered) return@launch
                directTriggered = true
                button.tryVibrateForLongPress()
                onResolved(
                    GestureResolvedActions(
                        button = button,
                        direction = GestureDirection.Right,
                        actionDirection = GestureDirection.Right,
                        actions = button.longPressActions,
                        triggerType = GestureTriggerType.LongPress,
                        touchPosition = finger,
                    )
                )
            }
        }

        animState = AnimState(originX = offset.x, originY = offset.y, fingerX = offset.x, fingerY = offset.y)
    }

    fun onDrag(dragAmount: Offset): GestureResolvedActions? {
        finger += dragAmount

        val touchSlop = viewConfiguration.scaledTouchSlop
        val minus = finger - origin
        if (calcLongPressJob?.isActive == true &&
            (minus.x.absoluteValue > touchSlop ||
            minus.y.absoluteValue > touchSlop)
        ) {
            calcLongPressJob?.cancel()
        }
        if (pendingTapJob?.isActive == true &&
            (minus.x.absoluteValue > touchSlop || minus.y.absoluteValue > touchSlop)
        ) {
            cancelPendingTap()
        }

        val button = button ?: return null
        val resolvedEffectiveButton = this.effectiveButton ?: button
        val physicalDirection = calcDirection(button, origin, finger) ?: return null
        val mappedActionDirection = calcDirection(button, origin, finger, mirrorHorizontal = isMirrorTouchTarget)
            ?: physicalDirection
        triggerDirection = physicalDirection
        actionDirection = mappedActionDirection

        val prev = animState
        animState = prev.copy(fingerX = prev.fingerX + dragAmount.x, fingerY = prev.fingerY + dragAmount.y)

        val canTriggerSlide = canDistanceTriggered(resolvedEffectiveButton, origin, finger, actionDirection, false, curStickySlideValue, judgeAction = false, configButton = button)
        val canTriggerLong = canDistanceTriggered(resolvedEffectiveButton, origin, finger, actionDirection, true, curStickySlideValue, judgeAction = false, configButton = button)
        if (canTriggerLong) {
            val holdDelayMs = button.longSlideHoldTriggerDelayMs
            val timeMs = SystemClock.uptimeMillis()
            if (longSlideHoldFirstTriggerMs == 0L) {
                longSlideHoldFirstTriggerMs = timeMs
            } else if (!directTriggered && timeMs - longSlideHoldFirstTriggerMs >= holdDelayMs) {
                val actions = button.longSlideHoldActions.actionsBy(actionDirection)
                if (actions.hasMeaningfulActions()) {
                    directTriggered = true
                    button.tryVibrateForLongSlide()
                    return resolved(button, actionDirection, GestureTriggerType.LongSlideHold, actions)
                }
            }
        } else {
            longSlideHoldFirstTriggerMs = 0L
        }

        if (canTriggerSlide && !canTriggerLong) {
            val holdDelayMs = button.slideHoldTriggerDelayMs
            val timeMs = SystemClock.uptimeMillis()
            if (slideHoldFirstTriggerMs == 0L) {
                slideHoldFirstTriggerMs = timeMs
            } else if (!directTriggered && timeMs - slideHoldFirstTriggerMs >= holdDelayMs) {
                val actions = button.slideHoldActions.actionsBy(actionDirection)
                if (actions.hasMeaningfulActions()) {
                    directTriggered = true
                    if (!slideVibrationFlags) button.tryVibrateForSlide()
                    return resolved(button, actionDirection, GestureTriggerType.SlideHold, actions)
                }
            }
        } else {
            slideHoldFirstTriggerMs = 0L
        }

        if (button.vibrateImmediately &&
            !slideVibrationFlags && canTriggerSlide
        ) {
            slideVibrationFlags = true
            button.tryVibrateForSlide()
        }

        return null
    }

    fun onDragEnd(): GestureResolvedActions? {
        calcLongPressJob?.cancel()
        val button = button ?: return null
        if (directTriggered) {
            reset()
            return null
        }
        val actionDirection = actionDirection
        var result: GestureResolvedActions? = null
        val resolvedEffectiveButton = this.effectiveButton ?: button
        if (canDistanceTriggered(resolvedEffectiveButton, origin, finger, actionDirection, true, curStickySlideValue, configButton = button)) {
            val actions = button.longSlideActions.actionsBy(actionDirection)
            if (actions.hasMeaningfulActions()) {
                button.tryVibrateForLongSlide()
                result = resolved(button, actionDirection, GestureTriggerType.LongSlide, actions)
            }
        } else if (canDistanceTriggered(resolvedEffectiveButton, origin, finger, actionDirection, false, curStickySlideValue, configButton = button)) {
            val actions = button.slideActions.actionsBy(actionDirection)
            if (actions.hasMeaningfulActions()) {
                if (!slideVibrationFlags) {
                    button.tryVibrateForSlide()
                }
                result = resolved(button, actionDirection, GestureTriggerType.Slide, actions)
            }
        }

        if (result == null) {
            val distance = hypot(finger.x - origin.x, finger.y - origin.y)
            if (distance <= viewConfiguration.scaledTouchSlop) {
                if (pendingTapButtonId == button.id && button.doubleTapActions.hasMeaningfulActions()) {
                    pendingTapJob?.cancel()
                    pendingTapJob = null
                    pendingTapButtonId = null
                    button.tryVibrateForTap()
                    result = resolved(button, GestureDirection.Right, GestureTriggerType.DoubleTap, button.doubleTapActions)
                } else if (button.tapActions.hasMeaningfulActions() || button.doubleTapActions.hasMeaningfulActions()) {
                    val tapResult = resolved(button, GestureDirection.Right, GestureTriggerType.Tap, button.tapActions)
                    if (button.doubleTapActions.hasMeaningfulActions()) {
                        pendingTapButtonId = button.id
                        pendingTapJob?.cancel()
                        pendingTapJob = coroutineScope.launch {
                            delay(button.doubleTapTriggerDelayMs)
                            pendingTapButtonId = null
                            if (button.tapActions.hasMeaningfulActions()) {
                                button.tryVibrateForTap()
                                onResolved(tapResult)
                            }
                        }
                    } else {
                        if (!slideVibrationFlags) button.tryVibrateForTap()
                        result = tapResult
                    }
                }
            }
        }

        reset()
        return result
    }

    fun cancel() {
        if (isCanceled) return
        reset()
        isCanceled = true
    }

    fun onDragCancel() {
        reset()
    }

    fun reset() {
        calcLongPressJob?.cancel()
        calcLongPressJob = null
        isCanceled = false
        origin = Offset.Unspecified
        finger = Offset.Unspecified
        button = null
        effectiveButton = null
        isMirrorTouchTarget = false
        animState = AnimState()
        longSlideFirstTriggerMs = 0L
        slideHoldFirstTriggerMs = 0L
        longSlideHoldFirstTriggerMs = 0L
        isOhoGestureEverCanTriggered = false
        slideVibrationFlags = false
        directTriggered = false
        actionDirection = GestureDirection.Right
    }

    fun canDistanceTriggered(button: GestureButton, isLongSlide: Boolean, judgeAction: Boolean = true): Boolean {
        return canDistanceTriggered(button, origin, finger, triggerDirection, isLongSlide, curStickySlideValue, judgeAction)
    }

    fun cancelPendingTap() {
        pendingTapJob?.cancel()
        pendingTapJob = null
        pendingTapButtonId = null
    }

    private fun resolved(
        button: GestureButton,
        actionDirection: GestureDirection,
        triggerType: GestureTriggerType,
        actions: List<Action>,
    ): GestureResolvedActions {
        return GestureResolvedActions(
            button = button,
            direction = triggerDirection,
            actionDirection = actionDirection,
            actions = actions,
            triggerType = triggerType,
            touchPosition = finger,
        )
    }

    private fun List<Action>.hasMeaningfulActions(): Boolean = any { it != Action.NONE }
}
