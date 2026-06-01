package hunoia.luno.gesture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import hunoia.luno.action.api.ActionFacade
import hunoia.luno.action.payload.SubGestureActionData
import hunoia.luno.config.model.Action
import hunoia.luno.config.model.SubGesture
import hunoia.luno.config.model.SubGestureSettings
import hunoia.luno.core.JsonSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_SUB_GESTURE_DEPTH = 3

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
        subGestureDepth += 1
        scheduleTimeout()
        onModeChanged(true)
        return true
    }

    fun onDragStart(): Boolean {
        if (!isActive) return false
        subGestureAccum = Offset.Zero
        restartTimeout()
        return true
    }

    fun onDrag(dragAmount: Offset): Action? {
        if (!isActive) return null
        subGestureAccum += dragAmount
        val sg = activeSubGesture!!
        if (kotlin.math.hypot(subGestureAccum.x, subGestureAccum.y) >= sg.triggerDistance) {
            val direction = sg.angle.directionOf(subGestureAccum)
            val action = sg.actionFor(direction)
            lastResolvedActionSubGesture = sg
            activeSubGesture = null
            subGestureAccum = Offset.Zero
            sg.tryVibrate()
            return action
        }
        return null
    }

    fun onDragEnd() {
        if (!isActive) return
        subGestureAccum = Offset.Zero
    }

    fun onDragCancel() {
        if (!isActive) return
        clear()
    }

    fun clear(notifyService: Boolean = true) {
        activeSubGesture = null
        lastResolvedActionSubGesture = null
        subGestureAccum = Offset.Zero
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
}
