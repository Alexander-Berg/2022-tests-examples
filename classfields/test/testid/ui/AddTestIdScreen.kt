package ru.auto.test.testid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import ru.auto.core_ui.compose.components.ButtonDefaults
import ru.auto.core_ui.compose.components.CenterAlignedTopAppBar
import ru.auto.core_ui.compose.components.Scaffold
import ru.auto.core_ui.compose.components.Text
import ru.auto.core_ui.compose.components.TextButton
import ru.auto.core_ui.compose.components.TextField
import ru.auto.core_ui.compose.tea.subscribeAsState
import ru.auto.core_ui.compose.theme.AutoTheme
import ru.auto.core_ui.compose.theme.tokens.DimenTokens
import ru.auto.test.R
import ru.auto.test.common.di.ComponentManager
import ru.auto.test.common.ui.CloseIconButton
import ru.auto.test.common.ui.SelectableTextField
import ru.auto.test.common.ui.provide
import ru.auto.test.testid.presentation.AddTestId
import ru.auto.test.testid.presentation.AddTestIdFeature

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AddTestIdScreen(
    close: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val feature: AddTestIdFeature = provide(ComponentManager::addTestIdFeature)
    val state: AddTestId.State by feature.subscribeAsState { eff ->
        when (eff) {
            is AddTestId.Eff.Close -> {
                keyboardController?.hide()
                close()
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = AutoTheme.colorScheme.surface,
        topBar = {
            AddTestIdTopAppBar(
                onCloseClick = { feature.accept(AddTestId.Msg.OnCloseClick) },
                onSaveClick = { feature.accept(AddTestId.Msg.OnSaveClick) }
            )
        },
        content = { innerPadding ->
            AddTestIdContent(
                modifier = Modifier.padding(innerPadding),
                state = state,
                feature = feature
            )
        }
    )
}

@Composable
private fun AddTestIdTopAppBar(
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = Modifier.statusBarsPadding(),
        title = { Text(stringResource(R.string.add_test_id_title)) },
        navigationIcon = { CloseIconButton(onClick = onCloseClick) },
        actions = {
            TextButton(
                colors = ButtonDefaults.textSecondaryButtonColors(),
                onClick = onSaveClick
            ) {
                Text(text = stringResource(R.string.save))
            }
        }
    )
}

@Composable
private fun AddTestIdContent(
    modifier: Modifier = Modifier,
    state: AddTestId.State,
    feature: AddTestIdFeature,
) {

    val focusRequesters = remember { Array(Field.values().size) { FocusRequester() } }
    var fieldInFocus by rememberSaveable(stateSaver = fieldSaver) { mutableStateOf(Field.TestId) }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(DimenTokens.x4),
        verticalArrangement = Arrangement.spacedBy(DimenTokens.x2)
    ) {

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequesters[Field.TestId.ordinal])
                .onFocusChanged {
                    if (it.isFocused) fieldInFocus = Field.TestId
                },
            value = state.testId,
            isError = state.testIdError != null,
            label = {
                val text = when (state.testIdError) {
                    AddTestId.TestIdError.AlreadyExist -> stringResource(R.string.test_id_alredy_exist)
                    else -> stringResource(R.string.test_id)
                }
                Text(text)
            },
            onValueChange = { feature.accept(AddTestId.Msg.OnTestIdChange(it)) },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Number
            ),
            keyboardActions = KeyboardActions {
                focusRequesters[Field.Name.ordinal].requestFocus()
            },
        )

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequesters[Field.Name.ordinal])
                .onFocusChanged { if (it.isFocused) fieldInFocus = Field.Name },
            value = state.description,
            label = { Text(stringResource(R.string.description)) },
            onValueChange = { feature.accept(AddTestId.Msg.OnDescriptionChange(it)) },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions {
                expanded = true
            },
        )

        SelectableTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.key,
            onValueChange = { feature.accept(AddTestId.Msg.OnKeyChange(it)) },
            label = stringResource(R.string.key),
            options = state.keys,
            expanded = expanded,
            onExpandedChange = { expanded = it }
        )
    }

    DisposableEffect(fieldInFocus) {
        focusRequesters[fieldInFocus.ordinal].requestFocus()
        onDispose {}
    }
}

private enum class Field {
    TestId,
    Name,
}

private val fieldSaver: Saver<Field, Int>
    get() = Saver(
        save = { it.ordinal },
        restore = { Field.values()[it] }
    )
