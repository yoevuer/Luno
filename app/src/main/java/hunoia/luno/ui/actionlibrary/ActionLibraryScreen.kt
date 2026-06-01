package hunoia.luno.ui.actionlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import hunoia.luno.R
import hunoia.luno.config.model.Action
import hunoia.luno.config.model.ActionLibraryEntry
import hunoia.luno.config.model.ActionLibraryType
import hunoia.luno.config.model.OpenAppOrUrlData
import hunoia.luno.config.model.ShellCommandData
import hunoia.luno.core.JsonSerializer
import hunoia.luno.ui.component.AppSearchBar
import hunoia.luno.ui.component.EmptyState
import hunoia.luno.ui.component.TopBar
import hunoia.luno.ui.settings.ActivitySettingsContent
import hunoia.luno.ui.settings.ShellCommandSettingsContent
import hunoia.luno.ui.settings.UrlSettingsContent
import hunoia.luno.ui.theme.ContentPaddingHorizontal
import hunoia.luno.ui.theme.ScrollBottomPadding
import hunoia.luno.ui.theme.Spacing12
import hunoia.luno.ui.theme.Spacing4
import hunoia.luno.ui.theme.Spacing8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionLibraryScreen(
    onBack: () -> Unit,
    vm: ActionLibraryVM = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ActionLibraryEntry?>(null) }
    var deleting by remember { mutableStateOf<ActionLibraryEntry?>(null) }
    Scaffold(
        topBar = { TopBar(onBack = onBack, title = stringResource(R.string.action_library)) },
        floatingActionButton = {
            FloatingActionButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    ActionLibraryType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(type.titleRes)) },
                            onClick = {
                                menuExpanded = false
                                editing = ActionLibraryEntry.create(type, defaultName(type, uiState.entries))
                            },
                        )
                    }
                }
            }
        }
    ) { padding ->
        val filtered = uiState.entries
            .filter { it.matchesQuery(query) }
            .sortedWith(compareBy<ActionLibraryEntry> { it.type.sortIndex() }.thenBy { it.createdAt })
        LazyColumn(contentPadding = PaddingValues(bottom = ScrollBottomPadding), modifier = Modifier.padding(padding)) {
            item {
                AppSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = stringResource(R.string.action_library_search_hint),
                    modifier = Modifier.padding(horizontal = ContentPaddingHorizontal * 2, vertical = Spacing8),
                )
            }
            if (filtered.isEmpty()) {
                item { EmptyState(stringResource(R.string.action_library_empty)) }
            } else {
                filtered.groupBy { it.type }.forEach { (type, entries) ->
                    item(key = "header_${type.name}") {
                        Text(
                            text = stringResource(type.titleRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = ContentPaddingHorizontal * 2, vertical = Spacing8),
                        )
                    }
                    items(entries, key = { it.id }) { entry ->
                        ActionLibraryItem(
                            entry = entry,
                            referenceCount = uiState.referenceCounts[entry.id] ?: 0,
                            onClick = { editing = entry },
                            onDelete = { deleting = entry },
                        )
                    }
                }
            }
        }
    }
    editing?.let { entry ->
        ActionLibraryEditDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { vm.save(it); editing = null },
        )
    }
    deleting?.let { entry ->
        val count = uiState.referenceCounts[entry.id] ?: 0
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.action_library_delete_title)) },
            text = { Text(stringResource(R.string.action_library_delete_desc, count)) },
            confirmButton = { TextButton(onClick = { vm.remove(entry); deleting = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ActionLibraryItem(entry: ActionLibraryEntry, referenceCount: Int, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing12, vertical = Spacing4),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(Spacing12), horizontalArrangement = Arrangement.spacedBy(Spacing12)) {
            Icon(entry.type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(entry.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.action_library_reference_count, referenceCount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null) }
        }
    }
}

@Composable
private fun ActionLibraryEditDialog(entry: ActionLibraryEntry, onDismiss: () -> Unit, onSave: (ActionLibraryEntry) -> Unit) {
    var name by remember(entry.id) { mutableStateOf(entry.name) }
    val action = remember(entry) { entry.toConfigAction() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_library_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing12)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.action_library_entry_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                when (entry.type) {
                    ActionLibraryType.Shell -> ShellCommandSettingsContent(action) { data ->
                        val shell = JsonSerializer.decodeFromString<ShellCommandData>(data)
                        onSave(entry.copy(name = name.ifBlank { entry.name }, shellCommand = shell))
                    }
                    ActionLibraryType.Url -> UrlSettingsContent(action) { data ->
                        onSave(entry.copy(name = name.ifBlank { entry.name }, openAppOrUrl = JsonSerializer.decodeFromString<OpenAppOrUrlData>(data)))
                    }
                    ActionLibraryType.Activity -> ActivitySettingsContent(action) { data ->
                        onSave(entry.copy(name = name.ifBlank { entry.name }, openAppOrUrl = JsonSerializer.decodeFromString<OpenAppOrUrlData>(data)))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private val ActionLibraryType.titleRes: Int get() = when (this) {
    ActionLibraryType.Shell -> R.string.action_library_shell
    ActionLibraryType.Url -> R.string.action_library_url
    ActionLibraryType.Activity -> R.string.action_library_activity
}

private val ActionLibraryType.icon: ImageVector get() = when (this) {
    ActionLibraryType.Shell -> Icons.Default.Terminal
    ActionLibraryType.Url -> Icons.AutoMirrored.Filled.OpenInNew
    ActionLibraryType.Activity -> Icons.Default.Settings
}

private val ActionLibraryEntry.summary: String get() = when (type) {
    ActionLibraryType.Shell -> shellCommand.command.ifBlank { "Shell" }
    ActionLibraryType.Url -> openAppOrUrl.url.ifBlank { "URL" }
    ActionLibraryType.Activity -> listOf(openAppOrUrl.packageName, openAppOrUrl.activityClassName).filter { it.isNotBlank() }.joinToString("/")
}

private fun ActionLibraryEntry.toConfigAction(): Action = when (type) {
    ActionLibraryType.Shell -> Action(data = JsonSerializer.encodeToString(shellCommand))
    ActionLibraryType.Url,
    ActionLibraryType.Activity -> Action(data = JsonSerializer.encodeToString(openAppOrUrl))
}

private fun defaultName(type: ActionLibraryType, entries: List<ActionLibraryEntry>): String {
    val count = entries.count { it.type == type } + 1
    val res = when (type) {
        ActionLibraryType.Shell -> R.string.action_library_default_shell
        ActionLibraryType.Url -> R.string.action_library_default_url
        ActionLibraryType.Activity -> R.string.action_library_default_activity
    }
    return hunoia.luno.core.AppContext.get().getString(res, count)
}
