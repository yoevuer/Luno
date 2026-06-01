package hunoia.luno.service.runtime

import android.os.SystemClock
import hunoia.luno.config.model.GestureButton
import hunoia.luno.service.hiddenKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GestureButtonHideRuntime(
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit,
) {
    private val hiddenButtons = mutableMapOf<String, Long>()

    fun hideTemporarily(button: GestureButton, delayMs: Long) {
        val key = button.hiddenKey()
        hiddenButtons[key] = SystemClock.uptimeMillis() + delayMs
        onStateChanged()
        scope.launch {
            delay(delayMs)
            if ((hiddenButtons[key] ?: 0L) <= SystemClock.uptimeMillis()) {
                hiddenButtons.remove(key)
                onStateChanged()
            }
        }
    }

    fun getSnapshot(): Map<String, Long> = hiddenButtons.toMap()
}
