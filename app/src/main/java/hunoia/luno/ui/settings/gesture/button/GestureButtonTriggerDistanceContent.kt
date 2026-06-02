package hunoia.luno.ui.settings.gesture.button

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import hunoia.luno.R
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxLongPressTriggerDelayMs
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxDoubleTapTriggerDelayMs
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxHoldTriggerDelayMs
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxLongSlideTriggerDistance
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxSlideTriggerDistance
import hunoia.luno.config.defaults.SettingsUiDefaults.MinLongPressTriggerDelayMs
import hunoia.luno.config.defaults.SettingsUiDefaults.MinDoubleTapTriggerDelayMs
import hunoia.luno.config.defaults.SettingsUiDefaults.MinHoldTriggerDelayMs
import hunoia.luno.config.defaults.SettingsUiDefaults.MinLongSlideTriggerDistance
import hunoia.luno.config.defaults.SettingsUiDefaults.MinSlideTriggerDistance
import hunoia.luno.config.model.GestureButton
import hunoia.luno.ui.component.MyColumn
import hunoia.luno.ui.component.input.MyTextSlider

@Composable
fun GestureButtonTriggerDistanceContent(
    button: GestureButton,
    vm: GestureButtonSettingsVM
) {
    val scrollState = rememberScrollState()
    GestureSlideTriggerDistanceContent(
        slideTriggerDistance = button.slideTriggerDistance,
        onSlideTriggerDistanceChange = vm::onSlideTriggerDistanceChange,
        slideTriggerDistanceRange = MinSlideTriggerDistance.toFloat()..MaxSlideTriggerDistance.toFloat(),
        longPressTriggerDelayMs = button.longPressTriggerDelayMs,
        onLongPressTriggerDelayMsChange = vm::onLongPressTriggerDelayMsChange,
        doubleTapTriggerDelayMs = button.doubleTapTriggerDelayMs,
        onDoubleTapTriggerDelayMsChange = vm::onDoubleTapTriggerDelayMsChange,
        longSlideTriggerDistance = button.longSlideTriggerDistance,
        onLongSlideTriggerDistanceChange = vm::onLongSlideTriggerDistanceChange,
        slideHoldTriggerDelayMs = button.slideHoldTriggerDelayMs,
        onSlideHoldTriggerDelayMsChange = vm::onSlideHoldTriggerDelayMsChange,
        longSlideHoldTriggerDelayMs = button.longSlideHoldTriggerDelayMs,
        onLongSlideHoldTriggerDelayMsChange = vm::onLongSlideHoldTriggerDelayMsChange,
        scrollState = scrollState,
    )
}

@Composable
fun GestureSlideTriggerDistanceContent(
    slideTriggerDistance: Int,
    onSlideTriggerDistanceChange: (Float) -> Unit,
    slideTriggerDistanceRange: ClosedFloatingPointRange<Float>,
    longPressTriggerDelayMs: Long? = null,
    onLongPressTriggerDelayMsChange: ((Float) -> Unit)? = null,
    doubleTapTriggerDelayMs: Long? = null,
    onDoubleTapTriggerDelayMsChange: ((Float) -> Unit)? = null,
    longSlideTriggerDistance: Int,
    onLongSlideTriggerDistanceChange: (Float) -> Unit,
    longSlideTriggerDistanceRange: ClosedFloatingPointRange<Float> = MinLongSlideTriggerDistance.toFloat()..MaxLongSlideTriggerDistance.toFloat(),
    slideHoldTriggerDelayMs: Long,
    onSlideHoldTriggerDelayMsChange: (Float) -> Unit,
    longSlideHoldTriggerDelayMs: Long,
    onLongSlideHoldTriggerDelayMsChange: (Float) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState? = null,
) {
    val content: @Composable () -> Unit = {
        MyTextSlider(
            value = slideTriggerDistance.toFloat(),
            onValueChange = onSlideTriggerDistanceChange,
            text = stringResource(R.string.trigger_slide_distance),
            valueDisplay = "${slideTriggerDistance}px",
            valueRange = slideTriggerDistanceRange,
        )
        if (longPressTriggerDelayMs != null && onLongPressTriggerDelayMsChange != null) {
            MyTextSlider(
                value = longPressTriggerDelayMs.toFloat(),
                onValueChange = onLongPressTriggerDelayMsChange,
                text = stringResource(R.string.trigger_long_press_delay),
                valueDisplay = "${longPressTriggerDelayMs}ms",
                valueRange = MinLongPressTriggerDelayMs.toFloat()..MaxLongPressTriggerDelayMs.toFloat(),
            )
        }
        if (doubleTapTriggerDelayMs != null && onDoubleTapTriggerDelayMsChange != null) {
            MyTextSlider(
                value = doubleTapTriggerDelayMs.toFloat(),
                onValueChange = onDoubleTapTriggerDelayMsChange,
                text = stringResource(R.string.trigger_double_tap_delay),
                valueDisplay = "${doubleTapTriggerDelayMs}ms",
                valueRange = MinDoubleTapTriggerDelayMs.toFloat()..MaxDoubleTapTriggerDelayMs.toFloat(),
            )
        }
        MyTextSlider(
            value = longSlideTriggerDistance.toFloat(),
            onValueChange = onLongSlideTriggerDistanceChange,
            text = stringResource(R.string.trigger_long_slide_distance),
            valueDisplay = "${longSlideTriggerDistance}px",
            valueRange = longSlideTriggerDistanceRange,
        )
        MyTextSlider(
            value = slideHoldTriggerDelayMs.toFloat(),
            onValueChange = onSlideHoldTriggerDelayMsChange,
            text = stringResource(R.string.trigger_slide_hold_delay),
            valueDisplay = "${slideHoldTriggerDelayMs}ms",
            valueRange = MinHoldTriggerDelayMs.toFloat()..MaxHoldTriggerDelayMs.toFloat(),
        )
        MyTextSlider(
            value = longSlideHoldTriggerDelayMs.toFloat(),
            onValueChange = onLongSlideHoldTriggerDelayMsChange,
            text = stringResource(R.string.trigger_long_slide_hold_delay),
            valueDisplay = "${longSlideHoldTriggerDelayMs}ms",
            valueRange = MinHoldTriggerDelayMs.toFloat()..MaxHoldTriggerDelayMs.toFloat(),
        )
    }
    if (scrollState != null) {
        MyColumn(scrollState = scrollState) { content() }
    } else {
        content()
    }
}
