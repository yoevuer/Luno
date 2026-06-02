package hunoia.luno.gesture

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import hunoia.luno.action.api.ActionFacade
import hunoia.luno.action.payload.SubGestureActionData
import hunoia.luno.config.model.Action
import hunoia.luno.config.model.GestureDirection
import hunoia.luno.config.model.SubGesture
import hunoia.luno.config.model.SubGestureSettings
import hunoia.luno.core.JsonSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_SUB_GESTURE_DEPTH = 3

data class SubGestureResolvedActions(
    val subGesture: SubGesture,
    val direction: GestureDirection,
    val actions: List<Action>,
    val isLongSlide: Boolean,
)

class SubGestureState(
    private val scope: CoroutineScope,
    private val subGestureSettings: SubGestureSettings,
    private val onModeChanged: (Boolean) -> Unit,
) {
    var activeSubGesture by mutableStateOf<SubGesture?>(null)
        private set
    var subGestureAccum by mutableStateOf(Offset.Zero)
        private set
    var subGestureDepth by mutableIntStateOf(0)
        private set
    var lastResolvedActionSubGesture by mutableStateOf<SubGesture?>(null)
        private set
    private var timeoutJob by mutableStateOf<Job?>(null)
    private var longSlideFirstTriggerMs = 0L
    private var slideVibrationFlags = false

    val isActive: Boolean get() = activeSubGesture != null

    fun tryEnterSubGesture(action: Action): Boolean {
        if (action.value != ActionFacade.SUB_GESTURE) return false
        val id = try {
            JsonSerializer.decodeFromString<SubGestureActionData>(action.data).id
        } catch (_: Exception) { return false }
        val target = subGestureSettings.subGestures.firstOrNull { it.id == id && it.enabled }
            ?: return false
        if (subGestureDepth >= MAX_SUB_GESTURE_DEPTH) return false
        activeSubGesture = target
        subGestureAccum = Offset.Zero
        longSlideFirstTriggerMs = 0L
        slideVibrationFlags = false
        subGestureDepth += 1
        scheduleTimeout()
        onModeChanged(true)
        return true
    }

    fun onDragStart(): Boolean {
        if (!isActive) return false
        subGestureAccum = Offset.Zero
        longSlideFirstTriggerMs = 0L
        slideVibrationFlags = false
        restartTimeout()
        return true
    }

    fun onDrag(dragAmount: Offset): SubGestureResolvedActions? {
        if (!isActive) return null
        subGestureAccum += dragAmount
        val sg = activeSubGesture!!
        val distance = kotlin.math.hypot(subGestureAccum.x, subGestureAccum.y)
        if (distance >= sg.longSlideTriggerDistance) {
            if (longSlideFirstTriggerMs == 0L) {
                longSlideFirstTriggerMs = SystemClock.uptimeMillis()
            } else if (sg.longSlideTriggerImmediately && SystemClock.uptimeMillis() - longSlideFirstTriggerMs >= sg.longSlideTriggerDelayMs) {
                val direction = sg.angle.directionOf(subGestureAccum)
                return resolve(sg, direction, isLongSlide = true)
            }
        } else {
            longSlideFirstTriggerMs = 0L
        }
        if (sg.vibrateImmediately && !slideVibrationFlags && distance >= sg.triggerDistance) {
            slideVibrationFlags = true
            sg.tryVibrateForSlide()
        }
        return null
    }

    fun onDragEnd(): SubGestureResolvedActions? {
        if (!isActive) return null
        val sg = activeSubGesture!!
        val distance = kotlin.math.hypot(subGestureAccum.x, subGestureAccum.y)
        val direction = sg.angle.directionOf(subGestureAccum)
        val canLongSlide = !sg.longSlideTriggerImmediately &&
            distance >= sg.longSlideTriggerDistance &&
            longSlideFirstTriggerMs != 0L &&
            SystemClock.uptimeMillis() - longSlideFirstTriggerMs >= sg.longSlideTriggerDelayMs
        return when {
            canLongSlide -> resolve(sg, direction, isLongSlide = true)
            distance >= sg.triggerDistance -> resolve(sg, direction, isLongSlide = false)
            else -> {
                subGestureAccum = Offset.Zero
                null
            }
        }
    }

    fun onDragCancel() {
        if (!isActive) return
        clear()
    }

    fun clear(notifyService: Boolean = true) {
        activeSubGesture = null
        lastResolvedActionSubGesture = null
        subGestureAccum = Offset.Zero
        longSlideFirstTriggerMs = 0L
        slideVibrationFlags = false
        subGestureDepth = 0
        timeoutJob?.cancel()
        timeoutJob = null
        if (notifyService) onModeChanged(false)
    }

    private fun scheduleTimeout() {
        timeoutJob?.cancel()
        val ms = activeSubGesture?.timeoutMs ?: return
        timeoutJob = scope.launch {
            delay(ms)
            clear()
        }
    }

    private fun restartTimeout() {
        scheduleTimeout()
    }

    private fun resolve(
        subGesture: SubGesture,
        direction: GestureDirection,
        isLongSlide: Boolean,
    ): SubGestureResolvedActions {
        val actions = if (isLongSlide) {
            subGesture.longSlideActionsFor(direction)
        } else {
            subGesture.slideActionsFor(direction)
        }
        lastResolvedActionSubGesture = subGesture
        activeSubGesture = null
        subGestureAccum = Offset.Zero
        longSlideFirstTriggerMs = 0L
        if (isLongSlide) {
            subGesture.tryVibrateForLongSlide()
        } else if (!slideVibrationFlags) {
            subGesture.tryVibrateForSlide()
        }
        slideVibrationFlags = false
        return SubGestureResolvedActions(subGesture, direction, actions, isLongSlide)
    }
}
