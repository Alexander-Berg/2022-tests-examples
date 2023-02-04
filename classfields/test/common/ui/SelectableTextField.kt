package ru.auto.test.common.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.DpOffset
import ru.auto.core_ui.compose.components.DropdownMenuItem
import ru.auto.core_ui.compose.components.ExposedDropdownMenuBox
import ru.auto.core_ui.compose.components.ExposedDropdownMenuDefaults
import ru.auto.core_ui.compose.components.Text
import ru.auto.core_ui.compose.components.TextField
import ru.auto.core_ui.compose.theme.tokens.DimenTokens

@Composable
fun SelectableTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isError: Boolean = false
) {
    @OptIn(ExperimentalMaterial3Api::class)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        TextField(
            modifier = modifier,
            readOnly = true,
            value = value,
            isError = isError,
            onValueChange = { },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            label = { Text(text = label) }
        )
        val focusManager = LocalFocusManager.current
        ExposedDropdownMenu(
            offset = DpOffset(y = DimenTokens.x1, x = DimenTokens.Zero),
            expanded = expanded,
            onDismissRequest = {
                onExpandedChange(false)
                focusManager.clearFocus()
            },
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onValueChange(selectionOption)
                        onExpandedChange(false)
                        focusManager.clearFocus()
                    }
                )
            }
        }
    }
}
