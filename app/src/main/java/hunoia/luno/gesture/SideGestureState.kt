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
import hunoia.luno.core.AppContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.hypot

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

    var onLongPress: (Action) -> Unit = {}

    private var longSlideFirstTriggerMs = 0L
    private var calcLongPressJob: Job? = null

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
        val longPressAction = button.longPressActions.firstOrNull()
        if (longPressAction != null && longPressAction != Action.NONE) {
            calcLongPressJob = coroutineScope.launch {
                delay(button.longPressTriggerDelayMs)
                button.tryVibrateForLongPress()
                onLongPress(longPressAction)
            }
        }

        animState = AnimState(originX = offset.x, originY = offset.y, fingerX = offset.x, fingerY = offset.y)
    }

    fun onDrag(dragAmount: Offset): List<Action>? {
        finger += dragAmount

        val touchSlop = viewConfiguration.scaledTouchSlop
        val minus = finger - origin
        if (calcLongPressJob?.isActive == true &&
            (minus.x.absoluteValue > touchSlop ||
            minus.y.absoluteValue > touchSlop)
        ) {
            calcLongPressJob?.cancel()
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

        val canTriggerLong = canDistanceTriggered(resolvedEffectiveButton, origin, finger, actionDirection, true, curStickySlideValue, configButton = button)
        if (canTriggerLong) {
            val longSlideDelayMs = button.longSlideTriggerDelayMs
            val timeMs = SystemClock.uptimeMillis()
            if (longSlideFirstTriggerMs == 0L) {
                longSlideFirstTriggerMs = timeMs
            } else if (timeMs - longSlideFirstTriggerMs >= longSlideDelayMs) {
                val actions = button.longSlideActions.actionsBy(actionDirection)
                if (button.longSlideTriggerImmediately) {
                    button.tryVibrateForLongSlide()
                    return actions
                }
            }
        } else {
            longSlideFirstTriggerMs = 0L
        }

        if (button.vibrateImmediately &&
            !slideVibrationFlags && canDistanceTriggered(resolvedEffectiveButton, origin, finger, actionDirection, false, curStickySlideValue, configButton = button)
        ) {
            slideVibrationFlags = true
            button.tryVibrateForSlide()
        }

        return emptyList()
    }

    fun onDragEnd(): Action {
        calcLongPressJob?.cancel()
        val button = button ?: return Action.NONE
        val actionDirection = actionDirection
        val longSlideDelayMs = button.longSlideTriggerDelayMs
        var returnAction = Action.NONE
        val resolvedEffectiveButton = this.effectiveButton ?: button
        if (!button.longSlideTriggerImmediately &&
            canDistanceTriggered(resolvedEffectiveButton, origin, finger, actionDirection, true, curStickySlideValue, configButton = button) &&
            SystemClock.uptimeMillis() - longSlideFirstTriggerMs >= longSlideDelayMs
        ) {
            val actions = button.longSlideActions.actionsBy(actionDirection)
            val action = actions.firstOrNull()
            if (action != null && action != Action.NONE) {
                button.tryVibrateForLongSlide()
                returnAction = action
            }
        } else if (canDistanceTriggered(resolvedEffectiveButton, origin, finger, actionDirection, false, curStickySlideValue, configButton = button)) {
            val actions = button.slideActions.actionsBy(actionDirection)
            val action = actions.firstOrNull()
            if (action != null && action != Action.NONE) {
                if (!slideVibrationFlags) {
                    button.tryVibrateForSlide()
                }
                returnAction = action
            }
        }

        if (returnAction == Action.NONE) {
            val distance = hypot(finger.x - origin.x, finger.y - origin.y)
            if (distance <= viewConfiguration.scaledTouchSlop) {
                val tapAction = button.tapActions.firstOrNull()
                if (tapAction != null && tapAction != Action.NONE) {
                    if (!slideVibrationFlags) {
                        button.tryVibrateForTap()
                    }
                    returnAction = tapAction
                }
            }
        }

        reset()
        return returnAction
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
        isOhoGestureEverCanTriggered = false
        slideVibrationFlags = false
        actionDirection = GestureDirection.Right
    }

    fun canDistanceTriggered(button: GestureButton, isLongSlide: Boolean, judgeAction: Boolean = true): Boolean {
        return canDistanceTriggered(button, origin, finger, triggerDirection, isLongSlide, curStickySlideValue, judgeAction)
    }
}
