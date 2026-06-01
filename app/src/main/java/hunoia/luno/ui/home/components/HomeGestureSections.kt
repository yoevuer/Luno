package hunoia.luno.ui.home

import hunoia.luno.ui.theme.*
import hunoia.luno.R
import hunoia.luno.config.model.GestureButton
import hunoia.luno.config.model.SubGesture

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun HomeGestureSections(
    uiState: UiState,
    onGestureHeaderClick: () -> Unit,
    onSubHeaderClick: () -> Unit,
    onGestureButtonClick: (GestureButton) -> Unit,
    onSubGestureClick: (String) -> Unit,
    onGestureCheckedChange: (GestureButton, Boolean) -> Unit,
    onSubCheckedChange: (SubGesture, Boolean) -> Unit,
    onAddGesture: () -> Unit,
    onAddSub: () -> Unit,
    onMarkColorClick: (Any) -> Unit,
    onGestureButtonRename: (GestureButton) -> Unit = {},
    onSubGestureRename: (SubGesture) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column {
            GestureEntryCard(
                title = stringResource(id = R.string.gesture_button),
                subtitle = "${uiState.gestureButtons.count { it.enabled }} / ${uiState.gestureButtons.size} 个已启用",
                icon = Icons.Default.TouchApp,
                expanded = uiState.isGestureButtonListExpanded,
                onClick = onGestureHeaderClick,
                accent = MaterialTheme.colorScheme.secondaryContainer,
                onAccent = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            GestureButtonList(
                visible = uiState.isGestureButtonListExpanded,
                buttons = uiState.gestureButtons,
                onItemClick = onGestureButtonClick,
                onCheckedChange = onGestureCheckedChange,
                onAddClick = onAddGesture,
                onMarkColorClick = onMarkColorClick,
                onRenameClick = onGestureButtonRename,
            )
        }
        Spacer(Modifier.height(Spacing12))
        Column {
            GestureEntryCard(
                title = stringResource(id = R.string.sub_gesture_list),
                subtitle = "${uiState.subGestures.count { it.enabled }} / ${uiState.subGestures.size} 个已启用",
                icon = Icons.Default.AllInclusive,
                expanded = uiState.isSubGestureListExpanded,
                onClick = onSubHeaderClick,
                accent = MaterialTheme.colorScheme.tertiaryContainer,
                onAccent = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            SubGestureList(
                visible = uiState.isSubGestureListExpanded,
                gestures = uiState.subGestures,
                onItemClick = onSubGestureClick,
                onCheckedChange = onSubCheckedChange,
                onAddClick = onAddSub,
                onMarkColorClick = onMarkColorClick,
                onRenameClick = onSubGestureRename,
            )
        }
    }
}
