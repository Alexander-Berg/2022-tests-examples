package ru.auto.test.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.auto.core_ui.compose.components.AlertDialog
import ru.auto.core_ui.compose.components.ButtonDefaults
import ru.auto.core_ui.compose.components.Text
import ru.auto.core_ui.compose.components.TextButton
import ru.auto.test.R

@Composable
fun ConfirmAlertDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                content = { Text(text = stringResource(id = R.string.ok)) },
                colors = ButtonDefaults.textSecondaryButtonColors(),
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                content = { Text(text = stringResource(id = R.string.cancel)) },
                colors = ButtonDefaults.textSecondaryButtonColors(),
            )
        }
    )
}
