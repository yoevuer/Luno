package hunoia.luno.ui.settings.gesture.subgesture

import hunoia.luno.ui.settings.gesture.subgesture.SubGestureActionSelectUiEvent
import hunoia.luno.ui.settings.gesture.subgesture.SubGestureActionSelectUiState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hunoia.luno.ui.component.displayNameRes
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import hunoia.luno.R
import hunoia.luno.config.model.Action
import hunoia.luno.action.api.ActionFacade
import hunoia.luno.config.model.SubGestureDirection
import hunoia.luno.ui.settings.ActionSettingsDialogContent
import hunoia.luno.ui.component.TopBar
import hunoia.luno.ui.actionselect.ActionPage
import hunoia.luno.ui.actionselect.UiState.SelectedRecord
import hunoia.luno.ui.theme.ScrollBottomPadding
import hunoia.luno.ui.permission.rememberGetInstalledAppsPermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubGestureActionSelectContent(
    subGestureId: String,
    direction: SubGestureDirection,
    onDismiss: () -> Unit,
    vm: SubGestureActionSelectVM = viewModel(
        key = "sub_gesture_action_select_${subGestureId}_${direction.name}",
        factory = SubGestureActionSelectVM.Factory(subGestureId, direction)
    )
) {
    val snackbarHostState = remember { SnackbarHostState() }

    UDFComponent(
        component = vm.udfComponent,
        onEvent = { },
        onBaseEvent = { baseEvent ->
            when (baseEvent) {
                is com.aaron.compose.component.UiBaseEvent.Finish -> { onDismiss(); true }
                else -> false
            }
        }
    ) { uiState ->
        val permissionState = rememberGetInstalledAppsPermissionState { granted ->
            if (granted) {
                vm.updateAppInfos()
                vm.updateShortcutInfos()
            }
        }
        LaunchedEffect(Unit) {
            if (permissionState.isGranted) {
                vm.updateAppInfos()
                vm.updateShortcutInfos()
            }
        }

        if (uiState.actionSettingsDialog.show) {
            ActionSettingsDialogContent(
                onDismissRequest = { vm.actionSettingsDialog.show(false) },
                action = uiState.actionSettingsDialog.action,
                onActionDataChanged = { vm.select(uiState.actionSettingsDialog.action.copy(data = it), true) }
            )
        }

        Scaffold(topBar = {
            TopBar(
                onBack = onDismiss,
                title = stringResource(id = direction.displayNameRes)
            )
        }) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                ActionPage(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = ScrollBottomPadding),
                    subGestures = uiState.subGestures,
                    actions = uiState.actions,
                    actionLibraryEntries = uiState.actionLibraryEntries,
                    appInfos = uiState.apps,
                    createShortcuts = uiState.createShortcuts,
                    launchShortcuts = uiState.launchShortcuts,
                    selectedRecord = uiState.selectedRecord,
                    maxSelectCount = 1,
                    longPressTargetIndex = null,
                    selectSingle = true,
                    snackbarHostState = snackbarHostState,
                    permissionState = permissionState,
                    onSelect = { action, selected -> vm.select(action, selected) },
                    onSelectLibraryEntry = { entry, selected -> vm.select(entry, selected) },
                    onSettingsClick = { action -> vm.showActionSettingsDialogHelper(action) },
                    onSelectApp = { appInfo, selected -> vm.select(appInfo, selected) },
                    onSelectShortcut = { shortcutInfo, selected -> vm.select(shortcutInfo, selected) },
                    onAppLongClick = { appInfo -> vm.toggleMiniWindow(appInfo) }
                )
            }
        }
    }
}

