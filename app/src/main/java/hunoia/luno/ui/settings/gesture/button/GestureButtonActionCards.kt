package hunoia.luno.ui.settings.gesture.button

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import hunoia.luno.R
import hunoia.luno.config.model.ActionPanelStyles
import hunoia.luno.config.model.GestureButton
import hunoia.luno.config.model.GestureDirection
import hunoia.luno.config.model.GestureTriggerType
import hunoia.luno.gesture.GestureFacade
import hunoia.luno.ui.component.ExpressiveCard
import hunoia.luno.ui.component.ExpressiveRow
import hunoia.luno.ui.component.actionTextCompose
import hunoia.luno.ui.navigation.ActionSelect
import hunoia.luno.ui.settings.gesture.style.MySideGestureSettings
import hunoia.luno.ui.settings.gesture.style.StyleTrailingButton

val actionCardDirections = listOf(
    GestureDirection.Left,
    GestureDirection.UpLeft,
    GestureDirection.Up,
    GestureDirection.UpRight,
    GestureDirection.Right,
    GestureDirection.DownRight,
    GestureDirection.Down,
    GestureDirection.DownLeft,
)

@Composable
fun GestureButtonSlideActionsCard(
    gestureButton: GestureButton,
    onNavToActionSelect: (ActionSelect) -> Unit,
) {
    ExpressiveCard(
        icon = Icons.Default.Swipe,
        title = stringResource(id = R.string.slide_action),
        subtitle = stringResource(id = R.string.slide_actions_subtitle),
        onClick = {},
    ) {
        SlideActionRows(
            styleGestureButton = gestureButton,
            actionsText = { direction -> gestureButton.slideActions.actionsBy(direction).actionTextCompose() },
            onDirectionClick = { direction ->
                onNavToActionSelect(
                    ActionSelect(
                        gestureButtonId = gestureButton.id,
                        direction = direction,
                        triggerType = GestureTriggerType.Slide,
                    )
                )
            },
        )
    }
}

@Composable
fun GestureButtonLongSlideActionsCard(
    gestureButton: GestureButton,
    onNavToActionSelect: (ActionSelect) -> Unit,
    onStyleSelect: (GestureDirection) -> Unit,
) {
    ExpressiveCard(
        icon = Icons.Default.Gesture,
        title = stringResource(id = R.string.long_slide_action),
        subtitle = stringResource(id = R.string.long_slide_subtitle),
        onClick = {},
    ) {
        LongSlideActionRows(
            styleGestureButton = gestureButton,
            actionsText = { direction -> gestureButton.longSlideActions.actionsBy(direction).actionTextCompose() },
            currentStyle = { direction -> GestureFacade.styleBy(gestureButton.longSlideActionPanelStyles, direction) },
            onDirectionClick = { direction ->
                onNavToActionSelect(
                    ActionSelect(
                        gestureButtonId = gestureButton.id,
                        direction = direction,
                        triggerType = GestureTriggerType.LongSlide,
                    )
                )
            },
            onStyleSelect = onStyleSelect,
        )
    }
}

@Composable
fun SlideActionRows(
    styleGestureButton: GestureButton,
    actionsText: @Composable (GestureDirection) -> String,
    onDirectionClick: (GestureDirection) -> Unit,
) {
    actionCardDirections.forEach { direction ->
        MySideGestureSettings(
            onClick = { onDirectionClick(direction) },
            gestureButton = styleGestureButton,
            direction = direction,
            isLongSlide = false,
            secondaryText = actionsText(direction),
        )
    }
}

@Composable
fun LongSlideActionRows(
    styleGestureButton: GestureButton,
    actionsText: @Composable (GestureDirection) -> String,
    currentStyle: (GestureDirection) -> ActionPanelStyles,
    onDirectionClick: (GestureDirection) -> Unit,
    onStyleSelect: (GestureDirection) -> Unit,
) {
    actionCardDirections.forEach { direction ->
        MySideGestureSettings(
            onClick = { onDirectionClick(direction) },
            gestureButton = styleGestureButton,
            direction = direction,
            isLongSlide = true,
            secondaryText = actionsText(direction),
            trailing = {
                StyleTrailingButton(
                    currentStyle = currentStyle(direction),
                    onClick = { onStyleSelect(direction) }
                )
            }
        )
    }
}

@Composable
fun GestureButtonTapActionsCard(
    gestureButton: GestureButton,
    onNavToActionSelect: (ActionSelect) -> Unit,
) {
    ExpressiveCard(
        icon = Icons.Default.Adjust,
        title = stringResource(id = R.string.tap_and_long_press_action),
        subtitle = stringResource(id = R.string.tap_long_press_subtitle),
        onClick = {},
    ) {
        ExpressiveRow(
            onClick = {
                onNavToActionSelect(
                    ActionSelect(
                        gestureButtonId = gestureButton.id,
                        direction = GestureDirection.Right,
                        triggerType = GestureTriggerType.Tap,
                    )
                )
            },
            text = stringResource(id = R.string.tap_action),
            secondaryText = gestureButton.tapActions.actionTextCompose(),
            secondaryTextColor = MaterialTheme.colorScheme.primary,
            icon = { Icon(Icons.Default.Adjust, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        ExpressiveRow(
            onClick = {
                onNavToActionSelect(
                    ActionSelect(
                        gestureButtonId = gestureButton.id,
                        direction = GestureDirection.Right,
                        triggerType = GestureTriggerType.DoubleTap,
                    )
                )
            },
            text = stringResource(id = R.string.double_tap_action),
            secondaryText = gestureButton.doubleTapActions.actionTextCompose(),
            secondaryTextColor = MaterialTheme.colorScheme.primary,
            icon = { Icon(Icons.Default.Adjust, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        ExpressiveRow(
            onClick = {
                onNavToActionSelect(
                    ActionSelect(
                        gestureButtonId = gestureButton.id,
                        direction = GestureDirection.Right,
                        triggerType = GestureTriggerType.LongPress,
                    )
                )
            },
            text = stringResource(id = R.string.long_press),
            secondaryText = gestureButton.longPressActions.actionTextCompose(),
            secondaryTextColor = MaterialTheme.colorScheme.primary,
            icon = { Icon(Icons.Default.Adjust, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
    }
}

@Composable
fun GestureButtonSlideHoldActionsCard(
    gestureButton: GestureButton,
    onNavToActionSelect: (ActionSelect) -> Unit,
) {
    ExpressiveCard(
        icon = Icons.Default.Swipe,
        title = stringResource(id = R.string.slide_hold_action),
        subtitle = stringResource(id = R.string.slide_hold_subtitle),
        onClick = {},
    ) {
        SlideActionRows(
            styleGestureButton = gestureButton,
            actionsText = { direction -> gestureButton.slideHoldActions.actionsBy(direction).actionTextCompose() },
            onDirectionClick = { direction ->
                onNavToActionSelect(
                    ActionSelect(
                        gestureButtonId = gestureButton.id,
                        direction = direction,
                        triggerType = GestureTriggerType.SlideHold,
                    )
                )
            },
        )
    }
}

@Composable
fun GestureButtonLongSlideHoldActionsCard(
    gestureButton: GestureButton,
    onNavToActionSelect: (ActionSelect) -> Unit,
    onStyleSelect: (GestureDirection) -> Unit,
) {
    ExpressiveCard(
        icon = Icons.Default.Gesture,
        title = stringResource(id = R.string.long_slide_hold_action),
        subtitle = stringResource(id = R.string.long_slide_hold_subtitle),
        onClick = {},
    ) {
        LongSlideActionRows(
            styleGestureButton = gestureButton,
            actionsText = { direction -> gestureButton.longSlideHoldActions.actionsBy(direction).actionTextCompose() },
            currentStyle = { direction -> GestureFacade.styleBy(gestureButton.longSlideActionPanelStyles, direction) },
            onDirectionClick = { direction ->
                onNavToActionSelect(
                    ActionSelect(
                        gestureButtonId = gestureButton.id,
                        direction = direction,
                        triggerType = GestureTriggerType.LongSlideHold,
                    )
                )
            },
            onStyleSelect = onStyleSelect,
        )
    }
}
