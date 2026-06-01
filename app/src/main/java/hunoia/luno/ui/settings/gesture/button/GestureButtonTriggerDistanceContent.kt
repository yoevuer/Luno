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
    MyColumn(scrollState = scrollState) {
        MyTextSlider(
            value = button.slideTriggerDistance.toFloat(),
            onValueChange = { vm.onSlideTriggerDistanceChange(it) },
            text = stringResource(R.string.trigger_slide_distance),
            valueDisplay = "${button.slideTriggerDistance}px",
            valueRange = MinSlideTriggerDistance.toFloat()..MaxSlideTriggerDistance.toFloat()
        )
        MyTextSlider(
            value = button.longPressTriggerDelayMs.toFloat(),
            onValueChange = { vm.onLongPressTriggerDelayMsChange(it) },
            text = stringResource(R.string.trigger_long_press_delay),
            valueDisplay = "${button.longPressTriggerDelayMs}ms",
            valueRange = MinLongPressTriggerDelayMs.toFloat()..MaxLongPressTriggerDelayMs.toFloat()
        )
        MyTextSlider(
            value = button.longSlideTriggerDistance.toFloat(),
            onValueChange = { vm.onLongSlideTriggerDistanceChange(it) },
            text = stringResource(R.string.trigger_long_slide_distance),
            valueDisplay = "${button.longSlideTriggerDistance}px",
            valueRange = MinLongSlideTriggerDistance.toFloat()..MaxLongSlideTriggerDistance.toFloat()
        )
        ExpressiveSwitchItem(
            onCheckedChange = { vm.onLongSlideTriggerImmediatelyChange(it) },
            checked = button.longSlideTriggerImmediately,
            title = stringResource(R.string.long_slide_trigger_immediately),
            subtitle = stringResource(R.string.long_slide_trigger_immediately_hint)
        )
        MyTextSlider(
            value = button.longSlideTriggerDelayMs.toFloat(),
            onValueChange = { vm.onLongSlideTriggerDelayMsChange(it) },
            text = stringResource(R.string.trigger_long_slide_delay),
            valueDisplay = "${button.longSlideTriggerDelayMs}ms",
            valueRange = MinLongSlideTriggerDelayMs.toFloat()..MaxLongSlideTriggerDelayMs.toFloat()
        )
    }
}
