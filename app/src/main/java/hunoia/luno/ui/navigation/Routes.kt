package hunoia.luno.ui.navigation

import androidx.annotation.Keep
import hunoia.luno.config.model.SubGestureDirection
import hunoia.luno.config.model.GestureDirection
import kotlinx.serialization.Serializable



@Keep
@Serializable
data class ActionSelect(
    val gestureButtonId: String,
    val direction: GestureDirection,
    val isLongSlide: Boolean,
    val isTap: Boolean = false,
    val isLongPress: Boolean = false,
)

@Serializable
@Keep
data class GestureButtonSettings(
    val buttonId: String,
)

@Keep
@Serializable
data object Home

@Keep
@Serializable
data object ActionLibrary

@Keep
@Serializable
data object ActionSettings

@Keep
@Serializable
data class SubGestureEditor(
    val subGestureId: String
)

@Keep
@Serializable
data class SubGestureActionSelect(
    val id: String,
    val direction: SubGestureDirection
)

@Keep
@Serializable
data object PointerSettings

@Keep
@Serializable
data object FrozenManage

@Keep
@Serializable
data object AppBlacklist
