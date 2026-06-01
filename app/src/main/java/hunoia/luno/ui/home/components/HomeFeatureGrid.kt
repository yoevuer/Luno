package hunoia.luno.ui.home

import hunoia.luno.ui.theme.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HomeFeatureGrid(
    uiState: UiState,
    onExcludeClick: () -> Unit,
    onActionSettingsClick: () -> Unit,
    onActionLibraryClick: () -> Unit,
    onPointerClick: () -> Unit,
    onFrozenClick: () -> Unit,
    onFreezeClick: () -> Unit,
    onUnfreezeClick: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onResetToggle: () -> Unit,
    showResetConfirm: Boolean,
    onResetConfirm: () -> Unit,
    onResetDismiss: () -> Unit,
    onCardAreaPosition: (Int) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useTwoColumns = maxWidth >= HomeWideBreakpoint
        if (useTwoColumns) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing12),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing12),
                ) {
                    HomePointerCard(onPointerClick)
                    HomeActionSettingsCard(onActionSettingsClick)
                    HomeActionLibraryCard(onActionLibraryClick)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing12),
                ) {
                    HomeFrozenCard(uiState, onFrozenClick, onFreezeClick, onUnfreezeClick)
                    HomeExcludeCard(uiState, onExcludeClick)
                    HomeToolsCard(
                        onBackupClick, onRestoreClick,
                        onResetToggle, showResetConfirm, onResetConfirm, onResetDismiss,
                        onCardAreaPosition,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing12),
            ) {
                HomePointerCard(onPointerClick)
                HomeActionSettingsCard(onActionSettingsClick)
                HomeActionLibraryCard(onActionLibraryClick)
                HomeFrozenCard(uiState, onFrozenClick, onFreezeClick, onUnfreezeClick)
                HomeExcludeCard(uiState, onExcludeClick)
                HomeToolsCard(
                    onBackupClick, onRestoreClick,
                    onResetToggle, showResetConfirm, onResetConfirm, onResetDismiss,
                    onCardAreaPosition,
                )
            }
        }
    }
}
