package ru.auto.test.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ru.auto.core_ui.compose.components.Scaffold
import ru.auto.core_ui.compose.components.Switch
import ru.auto.core_ui.compose.components.Text
import ru.auto.core_ui.compose.components.TextButton
import ru.auto.core_ui.compose.components.TextField
import ru.auto.core_ui.compose.components.Tooltip
import ru.auto.core_ui.compose.components.TooltipPlacement
import ru.auto.core_ui.compose.tea.subscribeAsState
import ru.auto.core_ui.compose.theme.AutoTheme
import ru.auto.core_ui.compose.theme.bottom
import ru.auto.core_ui.compose.theme.tokens.DimenTokens
import ru.auto.core_ui.compose.theme.top
import ru.auto.data.util.LoadableData
import ru.auto.data.util.maybeValue
import ru.auto.data.util.takeIfNotBlank
import ru.auto.settings.provider.SettingEntity
import ru.auto.settings.provider.SettingId
import ru.auto.settings.provider.SettingValue
import ru.auto.test.BuildConfig
import ru.auto.test.R
import ru.auto.test.common.di.ComponentManager
import ru.auto.test.common.model.SettingsStore
import ru.auto.test.common.ui.CenterAlignedTopAppBar
import ru.auto.test.common.ui.Empty
import ru.auto.test.common.ui.Loading
import ru.auto.test.common.ui.SelectableTextField
import ru.auto.test.common.ui.provide
import ru.auto.test.settings.presentation.Settings
import ru.auto.core_ui.compose.components.ButtonDefaults as AutoButtonDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    openExperiments: () -> Unit,
) {

    val feature = provide(ComponentManager::settingsFeature)
    val state: Settings.State by feature.subscribeAsState()

    val listState = rememberLazyListState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .imePadding()
            .navigationBarsPadding()
            .fillMaxSize(),
        topBar = {
            TopBar(
                actionsVisible = state.settings.maybeValue?.settings.isNullOrEmpty().not(),
                scrollBehavior = scrollBehavior,
                onExperimentsClick = openExperiments,
                tooltipVisible = state.isHelpTooltipVisible,
                onLogoClick = { feature.accept(Settings.Msg.OnLogoClick) },
                onTooltipDismissRequest = { feature.accept(Settings.Msg.OnHelpTooltipDismissRequest) }
            )
        },
        content = { padding ->
            Content(
                modifier = Modifier.padding(padding),
                listState = listState,
                state = state,
                onSettingChange = { settingId, value ->
                    feature.accept(Settings.Msg.OnSettingValueChanged(settingId, value))
                }
            )
        }
    )
}

@Composable
private fun TopBar(
    actionsVisible: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onExperimentsClick: () -> Unit,
    onLogoClick: () -> Unit,
    onTooltipDismissRequest: () -> Unit,
    tooltipVisible: Boolean,
) {
    CenterAlignedTopAppBar(
        modifier = Modifier.statusBarsPadding(),
        scrollBehavior = scrollBehavior,
        title = {
            Icon(
                modifier = Modifier.clickable(
                    onClick = onLogoClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ),
                painter = painterResource(id = R.drawable.logo_test_app),
                contentDescription = "Logo",
                tint = AutoTheme.colorScheme.secondary
            )
            Tooltip(
                visible = tooltipVisible,
                onDismissRequest = onTooltipDismissRequest,
                modifier = Modifier
                    .padding(horizontal = DimenTokens.x4)
                    .sizeIn(maxWidth = 300.dp),
                placement = TooltipPlacement.Bottom,
                offset = DpOffset(DimenTokens.Zero, DimenTokens.x2)
            ) {
                Text(
                    modifier = Modifier.padding(DimenTokens.x3),
                    text = stringResource(id = R.string.info_text, BuildConfig.BUILD_TYPE)
                )
            }
        },
        actions = {
            if (actionsVisible) {
                ExperimentsIconButton(onExperimentsClick)
            }
        }
    )
}

@Composable
private fun Content(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    state: Settings.State,
    onSettingChange: (setting: SettingEntity, value: SettingValue?) -> Unit,
) {
    when (val settings = state.settings) {
        is LoadableData.Initial, is LoadableData.Loading -> {
            Loading(containerColor = AutoTheme.colorScheme.surface)
        }
        is LoadableData.Failure -> {
            Empty(
                painter = painterResource(id = R.drawable.empty_error),
                text = settings.error.run { message ?: toString() }
            )
        }
        is LoadableData.Success -> {
            if (settings.value.settings.isNotEmpty()) {
                SettingsList(
                    modifier = modifier,
                    state = listState,
                    settingsStore = settings.value,
                    onSettingChange = onSettingChange
                )
            } else {
                Empty(
                    painter = painterResource(id = R.drawable.empty_nothing_found),
                    text = stringResource(R.string.settings_nothing_found)
                )
            }
        }
    }
}

@Composable
private fun SettingsList(
    state: LazyListState,
    modifier: Modifier = Modifier,
    settingsStore: SettingsStore,
    onSettingChange: (setting: SettingEntity, value: SettingValue?) -> Unit,
) {
    LazyColumn(
        state = state,
        modifier = modifier.fillMaxSize(),
    ) {
        settingsStore.settings.entries.forEach { (groupId, settingsGroup) ->
            settingsGroupTitle(groupId)
            settingsGroupItems(
                settings = settingsGroup.values,
                onSettingChange = onSettingChange,
                settingsValues = settingsStore.settingsValues
            )
        }
        item { Spacer(modifier = Modifier.height(DimenTokens.x2)) }
    }
}

private fun LazyListScope.settingsGroupTitle(text: String) {
    item(key = text) {
        Text(
            modifier = Modifier
                .padding(top = DimenTokens.x2)
                .fillMaxWidth()
                .clip(AutoTheme.shapes.large.top)
                .background(AutoTheme.colorScheme.surface)
                .padding(
                    start = DimenTokens.x5,
                    top = DimenTokens.x4,
                    end = DimenTokens.x3,
                    bottom = DimenTokens.x2
                ),
            text = text,
            style = AutoTheme.typography.headline5Bold
        )
    }
}

private fun LazyListScope.settingsGroupItems(
    settings: Collection<SettingEntity>,
    onSettingChange: (setting: SettingEntity, value: SettingValue?) -> Unit,
    settingsValues: Map<SettingId, SettingValue>,
) {
    var prevSetting: SettingEntity? = null
    settings.forEach { setting ->
        val fieldTopPadding = if (prevSetting !is SettingEntity.BooleanSetting) DimenTokens.x2 else DimenTokens.Zero
        when (setting) {
            is SettingEntity.BooleanSetting -> {
                item(key = setting.id) {
                    BooleanSettingItem(
                        text = setting.label,
                        checked = (settingsValues[setting.id] as? SettingValue.BooleanValue)?.value
                            ?: setting.defaultValue.value,
                        onValueChange = { onSettingChange(setting, SettingValue.BooleanValue(it)) }
                    )
                }
            }
            is SettingEntity.OptionsSetting -> {
                item(key = setting.id) {
                    OptionsSettingItem(
                        topPadding = fieldTopPadding,
                        label = setting.label,
                        value = (settingsValues[setting.id] as? SettingValue.StringValue)?.value
                            ?: setting.defaultValue?.value.orEmpty(),
                        onValueChange = { onSettingChange(setting, SettingValue.StringValue(it)) },
                        options = setting.options,
                    )
                }
            }
            is SettingEntity.StringSetting -> {
                item(key = setting.id) {
                    StringSettingItem(
                        topPadding = fieldTopPadding,
                        label = setting.label,
                        value = (settingsValues[setting.id] as? SettingValue.StringValue)?.value
                            ?: setting.defaultValue?.value.orEmpty(),
                        onValueChange = { onSettingChange(setting, it.takeIfNotBlank()?.let(SettingValue::StringValue)) },
                    )
                }
            }
        }
        prevSetting = setting
    }
    item {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(DimenTokens.x4)
            .clip(AutoTheme.shapes.large.bottom)
            .background(AutoTheme.colorScheme.surface)
        )
    }
}

@Composable
private fun OptionsSettingItem(
    topPadding: Dp,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
) {
    val (expanded, onExpandedChange) = remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AutoTheme.colorScheme.surface)
            .padding(
                start = DimenTokens.x3,
                end = DimenTokens.x3,
                top = topPadding
            )
    ) {
        SelectableTextField(
            modifier = Modifier.fillMaxWidth(),
            label = label,
            value = value,
            onValueChange = onValueChange,
            options = options,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
        )
    }
}

@Composable
private fun StringSettingItem(
    topPadding: Dp,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var localValue by remember(value) { mutableStateOf(value) }
    val focusManager = LocalFocusManager.current
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .background(AutoTheme.colorScheme.surface)
            .padding(
                start = DimenTokens.x3,
                end = DimenTokens.x3,
                top = topPadding
            ),
        label = { Text(text = label, maxLines = 1) },
        value = localValue,
        onValueChange = { localValue = it },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions {
            onValueChange(localValue)
            focusManager.clearFocus()
        }
    )
}

@Composable
private fun BooleanSettingItem(
    text: String,
    checked: Boolean,
    onValueChange: ((Boolean) -> Unit),
) {
    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .background(AutoTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            start = DimenTokens.x5,
            end = DimenTokens.x4
        ),
        shape = RectangleShape,
        colors = AutoButtonDefaults.textButtonColors(contentColor = AutoTheme.colorScheme.onSurface),
        onClick = { onValueChange(!checked) }
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = DimenTokens.x4,
                    bottom = DimenTokens.x4,
                    end = DimenTokens.x4
                ),
            text = text,
            style = AutoTheme.typography.subtitle
        )
        Switch(
            modifier = Modifier
                .align(Alignment.Top)
                .padding(
                    top = DimenTokens.x4,
                    bottom = DimenTokens.x4
                ),
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun ExperimentsIconButton(onClick: () -> Unit) {
    IconButton(
        modifier = Modifier
            .padding(
                top = DimenTokens.x1,
                end = DimenTokens.x2
            ),
        onClick = onClick
    ) {
        Image(painter = painterResource(id = R.drawable.ic_ab), contentDescription = "experiments")
    }
}
