package hunoia.luno.ui.settings

import hunoia.luno.ui.theme.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hunoia.luno.R
import hunoia.luno.config.model.Action
import hunoia.luno.config.model.OpenAppOrUrlData
import hunoia.luno.core.JsonSerializer

@Composable
fun UrlSettingsContent(
    action: Action,
    onConfirm: (String) -> Unit
) {
    val existingData = remember {
        runCatching { JsonSerializer.decodeFromString<OpenAppOrUrlData>(action.data) }.getOrNull()
    }
    var urlInput by remember { mutableStateOf(existingData?.url ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ItemPadding),
        verticalArrangement = Arrangement.spacedBy(ItemPadding)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text(stringResource(R.string.url_link)) },
            singleLine = true
        )
        TextButton(
            modifier = Modifier.align(Alignment.End),
            enabled = urlInput.isNotBlank(),
            onClick = {
                onConfirm(
                    JsonSerializer.encodeToString(
                        OpenAppOrUrlData(
                            type = OpenAppOrUrlData.TYPE_URL,
                            url = urlInput.trim()
                        )
                    )
                )
            }
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
            Text(text = stringResource(id = R.string.confirm))
        }
    }
}
