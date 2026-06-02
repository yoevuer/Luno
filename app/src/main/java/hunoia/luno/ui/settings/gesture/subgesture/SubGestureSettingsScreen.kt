package hunoia.luno.ui.settings.gesture.subgesture
import hunoia.luno.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import hunoia.luno.R
import hunoia.luno.ui.component.actionTextCompose
import hunoia.luno.config.model.GestureButton
import hunoia.luno.config.model.GestureDirection
import hunoia.luno.config.defaults.SettingsUiDefaults.GestureButtonColorAlpha
import hunoia.luno.config.model.SubGesture
import hunoia.luno.config.model.GestureTriggerType
import hunoia.luno.gesture.GestureFacade
import hunoia.luno.ui.navigation.ActionSelect
import hunoia.luno.ui.component.MyAlertDialog
import hunoia.luno.ui.component.OptimizedBottomSheet
import hunoia.luno.ui.component.ExpressiveCard
import hunoia.luno.ui.component.ExpressiveRow
import hunoia.luno.ui.component.ExpressiveSwitchItem
import hunoia.luno.ui.component.MyColumn
import hunoia.luno.ui.component.input.MyTextSlider
import hunoia.luno.ui.component.TopBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import com.aaron.compose.ktx.onSingleClick
import hunoia.luno.config.defaults.SettingsUiDefaults.getPredefinedVibrationEffectText
import hunoia.luno.config.defaults.SettingsUiDefaults.MinSubGestureTriggerDistance
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxSubGestureTriggerDistance
import hunoia.luno.config.defaults.SettingsUiDefaults.MinSubGestureLongSlideTriggerDistance
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxSubGestureLongSlideTriggerDistance
import hunoia.luno.config.defaults.SettingsUiDefaults.MinSubGestureTimeoutMs
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxSubGestureTimeoutMs
import hunoia.luno.bridge.vibration.MaxCustomVibrationMs
import hunoia.luno.bridge.vibration.MinCustomVibrationMs
import hunoia.luno.bridge.vibration.VibrationEffects
import hunoia.luno.ui.settings.gesture.subgesture.SubGestureSettingsUiEvent
import hunoia.luno.ui.settings.gesture.subgesture.SubGestureSettingsUiState
import hunoia.luno.ui.settings.gesture.button.LongSlideActionRows
import hunoia.luno.ui.settings.gesture.button.GestureSlideTriggerDistanceContent
import hunoia.luno.ui.settings.gesture.button.SlideActionRows
import hunoia.luno.ui.settings.gesture.button.VibrationEffectSelector
import hunoia.luno.ui.settings.gesture.style.ActionPanelStyleConfigContent
import hunoia.luno.ui.settings.gesture.style.ActionPanelStyleSelectContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubGestureSettingsScreen(
    onBack: () -> Unit,
    onNavToActionSelect: (ActionSelect) -> Unit = {},
    vm: SubGestureSettingsVM = viewModel()
) {
    var showGestureAngles by remember { mutableStateOf(false) }
    var showSubVibrationSettings by remember { mutableStateOf(false) }
    var showSubTriggerDistanceSettings by remember { mutableStateOf(false) }
    var showStyleSelectFor by remember { mutableStateOf<GestureDirection?>(null) }
    var showStyleConfigFor by remember { mutableStateOf<GestureDirection?>(null) }
    UDFComponent<SubGestureSettingsUiState, SubGestureSettingsUiEvent>(component = vm.udfComponent, onEvent = { }) { uiState ->
        if (uiState.showDeleteWarningDialog) {
            MyAlertDialog(
                onDismissRequest = { vm.showDeleteWarningDialog(false) },
                title = stringResource(id = R.string.delete_sub_gesture_warning),
                text = stringResource(id = R.string.delete_sub_gesture_warning_desc),
                onConfirmClick = { vm.deleteSubGesture() }
            )
        }
        if (uiState.showMirrorCopyDialog) {
            MyAlertDialog(
                onDismissRequest = { vm.showMirrorCopyDialog(false) },
                title = stringResource(id = R.string.mirror_sub_gesture),
                text = stringResource(id = R.string.mirror_sub_gesture_desc),
                onConfirmClick = { vm.createMirroredCopy() }
            )
        }

        val gesture = uiState.subGesture ?: return@UDFComponent

        if (showGestureAngles) {
            OptimizedBottomSheet(
                onDismissRequest = { showGestureAngles = false }
            ) {
                SubGestureAngleContent(
                    angle = gesture.angle,
                    onDismiss = { showGestureAngles = false },
                    onSave = { newAngle ->
                        vm.updateAngle(newAngle)
                        showGestureAngles = false
                    },
                    color = Color(gesture.color)
                )
            }
        }

        if (showSubVibrationSettings) {
            OptimizedBottomSheet(
                onDismissRequest = { showSubVibrationSettings = false }
            ) {
                SubGestureVibrationContent(
                    gesture = gesture,
                    vm = vm
                )
            }
        }

        if (showSubTriggerDistanceSettings) {
            OptimizedBottomSheet(
                onDismissRequest = { showSubTriggerDistanceSettings = false }
            ) {
                SubGestureTriggerDistanceContent(
                    gesture = gesture,
                    vm = vm
                )
            }
        }

        showStyleSelectFor?.let { direction ->
            val currentStyle = GestureFacade.styleBy(gesture.longSlideActionPanelStyles, direction)
            OptimizedBottomSheet(
                onDismissRequest = { showStyleSelectFor = null }
            ) {
                ActionPanelStyleSelectContent(
                    currentStyle = currentStyle,
                    onStyleSelected = { style ->
                        vm.updateLongSlideActionPanelStyle(direction, style)
                    },
                    onConfigRequest = { _ ->
                        showStyleSelectFor = null
                        showStyleConfigFor = direction
                    }
                )
            }
        }

        showStyleConfigFor?.let { direction ->
            val currentStyle = GestureFacade.styleBy(gesture.longSlideActionPanelStyles, direction)
            OptimizedBottomSheet(
                onDismissRequest = { showStyleConfigFor = null }
            ) {
                ActionPanelStyleConfigContent(
                    currentStyle = currentStyle,
                    onStyleChanged = { style ->
                        vm.updateLongSlideActionPanelStyle(direction, style)
                    }
                )
            }
        }

        Scaffold(topBar = {
            TopBar(
                onBack = onBack,
                title = uiState.subGesture.name.ifEmpty { stringResource(id = R.string.sub_gesture) },
                postfixTitle = {
                    Box(
                        modifier = Modifier
                            .padding(start = IconTextPadding)
                            .size(MarkColorSize)
                            .background(
                                color = Color(gesture.color).copy(alpha = GestureButtonColorAlpha),
                                shape = CircleShape
                            )
                    )
                },
                actions = {
                    IconButton(onClick = { vm.showMirrorCopyDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = { vm.showDeleteWarningDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )
                    }
                }
            )
        }) { innerPadding ->
            MyColumn(
                modifier = Modifier.padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing12)
            ) {
                val styleGestureButton = remember(gesture.id, gesture.color, gesture.longSlideActionPanelStyles) {
                    GestureButton(
                        id = gesture.id,
                        color = gesture.color,
                        longSlideActionPanelStyles = gesture.longSlideActionPanelStyles,
                    )
                }
                ExpressiveCard(
                    icon = Icons.Default.Swipe,
                    title = stringResource(id = R.string.slide_action),
                    subtitle = stringResource(id = R.string.slide_actions_subtitle),
                    onClick = {},
                ) {
                    SlideActionRows(
                        styleGestureButton = styleGestureButton,
                        actionsText = { direction ->
                            gesture.slideActionsFor(direction).actionTextCompose()
                        },
                        onDirectionClick = { direction ->
                                onNavToActionSelect(
                                    ActionSelect(
                                        gestureButtonId = "",
                                        direction = direction,
                                        triggerType = GestureTriggerType.Slide,
                                        subGestureId = gesture.id,
                                    )
                                )
                        },
                    )
                }

                ExpressiveCard(
                    icon = Icons.Default.Swipe,
                    title = stringResource(id = R.string.slide_hold_action),
                    subtitle = stringResource(id = R.string.slide_hold_subtitle),
                    onClick = {},
                ) {
                    SlideActionRows(
                        styleGestureButton = styleGestureButton,
                        actionsText = { direction ->
                            gesture.slideHoldActionsFor(direction).actionTextCompose()
                        },
                        onDirectionClick = { direction ->
                                onNavToActionSelect(
                                    ActionSelect(
                                        gestureButtonId = "",
                                        direction = direction,
                                        triggerType = GestureTriggerType.SlideHold,
                                        subGestureId = gesture.id,
                                    )
                                )
                        },
                    )
                }

                ExpressiveCard(
                    icon = Icons.Default.Gesture,
                    title = stringResource(id = R.string.long_slide_action),
                    subtitle = stringResource(id = R.string.long_slide_subtitle),
                    onClick = {},
                ) {
                    LongSlideActionRows(
                        styleGestureButton = styleGestureButton,
                        actionsText = { direction ->
                            gesture.longSlideActionsFor(direction).actionTextCompose()
                        },
                        currentStyle = { direction ->
                            GestureFacade.styleBy(gesture.longSlideActionPanelStyles, direction)
                        },
                        onDirectionClick = { direction ->
                                onNavToActionSelect(
                                    ActionSelect(
                                        gestureButtonId = "",
                                        direction = direction,
                                        triggerType = GestureTriggerType.LongSlide,
                                        subGestureId = gesture.id,
                                    )
                                )
                        },
                        onStyleSelect = { direction -> showStyleSelectFor = direction },
                    )
                }

                ExpressiveCard(
                    icon = Icons.Default.Gesture,
                    title = stringResource(id = R.string.long_slide_hold_action),
                    subtitle = stringResource(id = R.string.long_slide_hold_subtitle),
                    onClick = {},
                ) {
                    LongSlideActionRows(
                        styleGestureButton = styleGestureButton,
                        actionsText = { direction ->
                            gesture.longSlideHoldActionsFor(direction).actionTextCompose()
                        },
                        currentStyle = { direction ->
                            GestureFacade.styleBy(gesture.longSlideActionPanelStyles, direction)
                        },
                        onDirectionClick = { direction ->
                                onNavToActionSelect(
                                    ActionSelect(
                                        gestureButtonId = "",
                                        direction = direction,
                                        triggerType = GestureTriggerType.LongSlideHold,
                                        subGestureId = gesture.id,
                                    )
                                )
                        },
                        onStyleSelect = { direction -> showStyleSelectFor = direction },
                    )
                }

                ExpressiveCard(
                    icon = Icons.Default.Tune,
                    title = stringResource(id = R.string.physical_params),
                    subtitle = "角度、震动与触发距离",
                    onClick = {},
                ) {
                    ExpressiveRow(
                        onClick = { showGestureAngles = true },
                        text = stringResource(id = R.string.sub_gesture_angles),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                    ExpressiveRow(
                        onClick = { showSubVibrationSettings = true },
                        text = stringResource(id = R.string.gesture_button_vibration),
                        secondaryText = stringResource(id = R.string.vibration_hint),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                    ExpressiveRow(
                        onClick = { showSubTriggerDistanceSettings = true },
                        text = stringResource(id = R.string.gesture_button_trigger_distance),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                }

            }
        }
    }
}

@Composable
private fun SubGestureVibrationContent(
    gesture: SubGesture,
    vm: SubGestureSettingsVM
) {
    MyColumn(verticalArrangement = Arrangement.spacedBy(Spacing8)) {
        ExpressiveSwitchItem(
            onCheckedChange = { vm.onSubSlideVibrateChange(it) },
            checked = gesture.slideVibrate,
            title = stringResource(R.string.vibration_slide)
        )
        ExpressiveSwitchItem(
            onCheckedChange = { vm.onSubLongSlideVibrateChange(it) },
            checked = gesture.longSlideVibrate,
            title = stringResource(R.string.vibration_long_slide)
        )
        ExpressiveSwitchItem(
            onCheckedChange = { vm.onSubVibrateImmediatelyChange(it) },
            checked = gesture.vibrateImmediately,
            title = stringResource(R.string.vibrate_immediately),
            subtitle = stringResource(R.string.vibrate_immediately_hint)
        )
        VibrationEffectSelector(
            effect = gesture.vibrationEffect,
            onEffectChange = { vm.onSubVibrationEffectChange(it) }
        )
        MyTextSlider(
            enabled = gesture.vibrationEffect == VibrationEffects.None,
            value = gesture.customVibrationMs.toFloat(),
            onValueChange = { vm.onSubCustomVibrationMsChange(it) },
            text = stringResource(R.string.vibration_strength),
            valueDisplay = "${gesture.customVibrationMs}ms",
            valueRange = MinCustomVibrationMs.toFloat()..MaxCustomVibrationMs.toFloat()
        )
    }
}

@Composable
private fun SubGestureTriggerDistanceContent(
    gesture: SubGesture,
    vm: SubGestureSettingsVM
) {
    MyColumn {
        GestureSlideTriggerDistanceContent(
            slideTriggerDistance = gesture.triggerDistance,
            onSlideTriggerDistanceChange = vm::onSubTriggerDistanceChange,
            slideTriggerDistanceRange = MinSubGestureTriggerDistance.toFloat()..MaxSubGestureTriggerDistance.toFloat(),
            longSlideTriggerDistance = gesture.longSlideTriggerDistance,
            onLongSlideTriggerDistanceChange = vm::onSubLongSlideTriggerDistanceChange,
            longSlideTriggerDistanceRange = MinSubGestureLongSlideTriggerDistance.toFloat()..MaxSubGestureLongSlideTriggerDistance.toFloat(),
            slideHoldTriggerDelayMs = gesture.slideHoldTriggerDelayMs,
            onSlideHoldTriggerDelayMsChange = vm::onSubSlideHoldTriggerDelayMsChange,
            longSlideHoldTriggerDelayMs = gesture.longSlideHoldTriggerDelayMs,
            onLongSlideHoldTriggerDelayMsChange = vm::onSubLongSlideHoldTriggerDelayMsChange,
        )
        MyTextSlider(
            value = gesture.timeoutMs.toFloat(),
            onValueChange = { vm.onSubTimeoutMsChange(it) },
            text = stringResource(R.string.sub_gesture_timeout_label),
            valueDisplay = stringResource(R.string.sub_gesture_timeout_value, gesture.timeoutMs / 1000),
            valueRange = MinSubGestureTimeoutMs.toFloat()..MaxSubGestureTimeoutMs.toFloat()
        )
    }
}
