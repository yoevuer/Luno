package hunoia.luno.ui.component

import androidx.annotation.StringRes
import hunoia.luno.R
import hunoia.luno.config.model.SubGestureDirection

@get:StringRes
val SubGestureDirection.displayNameRes: Int
    get() = when (this) {
        SubGestureDirection.Up -> R.string.top
        SubGestureDirection.Down -> R.string.bottom
        SubGestureDirection.Left -> R.string.left
        SubGestureDirection.Right -> R.string.right
        SubGestureDirection.UpLeft -> R.string.direction_up_left
        SubGestureDirection.UpRight -> R.string.direction_up_right
        SubGestureDirection.DownLeft -> R.string.direction_down_left
        SubGestureDirection.DownRight -> R.string.direction_down_right
    }
