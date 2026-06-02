package hunoia.luno.ui.settings.gesture.button

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import hunoia.luno.R
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxLongPressTriggerDelayMs
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxLongSlideTriggerDelayMs
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxLongSlideTriggerDistance
import hunoia.luno.config.defaults.SettingsUiDefaults.MaxSlideTriggerDistance
import hunoia.luno.config.defaults.SettingsUiDefaults.MinLongPressTriggerDelayMs
import hunoia.luno.config.defaults.SettingsUiDefaults.MinLongSlideTriggerDelayMs
import hunoia.luno.config.defaults.SettingsUiDefaults.MinLongSlideTriggerDistance
import hunoia.luno.config.defaults.SettingsUiDefaults.MinSlideTriggerDistance
import hunoia.luno.config.model.GestureButton
import hunoia.luno.ui.component.ExpressiveSwitchItem
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
        longSlideTriggerDistance = button.longSlideTriggerDistance,
        onLongSlideTriggerDistanceChange = vm::onLongSlideTriggerDistanceChange,
        longSlideTriggerImmediately = button.longSlideTriggerImmediately,
        onLongSlideTriggerImmediatelyChange = vm::onLongSlideTriggerImmediatelyChange,
        longSlideTriggerDelayMs = button.longSlideTriggerDelayMs,
        onLongSlideTriggerDelayMsChange = vm::onLongSlideTriggerDelayMsChange,
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
    longSlideTriggerDistance: Int,
    onLongSlideTriggerDistanceChange: (Float) -> Unit,
    longSlideTriggerDistanceRange: ClosedFloatingPointRange<Float> = MinLongSlideTriggerDistance.toFloat()..MaxLongSlideTriggerDistance.toFloat(),
    longSlideTriggerImmediately: Boolean,
    onLongSlideTriggerImmediatelyChange: (Boolean) -> Unit,
    longSlideTriggerDelayMs: Long,
    onLongSlideTriggerDelayMsChange: (Float) -> Unit,
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
        MyTextSlider(
            value = longSlideTriggerDistance.toFloat(),
            onValueChange = onLongSlideTriggerDistanceChange,
            text = stringResource(R.string.trigger_long_slide_distance),
            valueDisplay = "${longSlideTriggerDistance}px",
            valueRange = longSlideTriggerDistanceRange,
        )
        ExpressiveSwitchItem(
            onCheckedChange = onLongSlideTriggerImmediatelyChange,
            checked = longSlideTriggerImmediately,
            title = stringResource(R.string.long_slide_trigger_immediately),
            subtitle = stringResource(R.string.long_slide_trigger_immediately_hint),
        )
        MyTextSlider(
            value = longSlideTriggerDelayMs.toFloat(),
            onValueChange = onLongSlideTriggerDelayMsChange,
            text = stringResource(R.string.trigger_long_slide_delay),
            valueDisplay = "${longSlideTriggerDelayMs}ms",
            valueRange = MinLongSlideTriggerDelayMs.toFloat()..MaxLongSlideTriggerDelayMs.toFloat(),
        )
    }
    if (scrollState != null) {
        MyColumn(scrollState = scrollState) { content() }
    } else {
        content()
    }
}
