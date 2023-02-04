package ru.auto.test.experiments.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import ru.auto.core_ui.compose.components.Checkbox
import ru.auto.core_ui.compose.components.Divider
import ru.auto.core_ui.compose.components.RadioButton
import ru.auto.core_ui.compose.components.Scaffold
import ru.auto.core_ui.compose.components.Text
import ru.auto.core_ui.compose.tea.subscribeAsState
import ru.auto.core_ui.compose.theme.AutoTheme
import ru.auto.core_ui.compose.theme.bottom
import ru.auto.core_ui.compose.theme.contentEmphasisLow
import ru.auto.core_ui.compose.theme.contentEmphasisMedium
import ru.auto.core_ui.compose.theme.tokens.DimenTokens
import ru.auto.core_ui.compose.theme.top
import ru.auto.data.util.LoadableData
import ru.auto.data.util.isNotNullOrEmpty
import ru.auto.settings.provider.ExperimentEntity
import ru.auto.settings.provider.TestIdEntity
import ru.auto.test.R
import ru.auto.test.common.di.ComponentManager
import ru.auto.test.common.model.ExperimentsStore
import ru.auto.test.common.ui.AddIconButton
import ru.auto.test.common.ui.BackIconButton
import ru.auto.test.common.ui.CenterAlignedTopAppBar
import ru.auto.test.common.ui.ConfirmAlertDialog
import ru.auto.test.common.ui.Empty
import ru.auto.test.common.ui.Error
import ru.auto.test.common.ui.Loading
import ru.auto.test.common.ui.provide
import ru.auto.test.experiments.presentation.Experiments
import ru.auto.test.experiments.presentation.ExperimentsFeature
import ru.auto.test.experiments.router.ExperimentsCoordinator


@Composable
fun ExperimentsScreen(
    coordinator: ExperimentsCoordinator,
) {
    val feature: ExperimentsFeature = provide(ComponentManager::experimentsFeature)
    val state by feature.subscribeAsState()
    ExperimentsScaffold(
        experiments = state.content,
        onBackClick = { coordinator.close() },
        onAddClick = { coordinator.openAddTestId() },
        onTestIdClick = { experiment, testId ->
            feature.accept(Experiments.Msg.OnTestIdClick(experiment?.key, testId))
        },
        onUserTestIdLongClick = { testId ->
            feature.accept(Experiments.Msg.OnUserTestIdLongClick(testId))
        },
        onRemoveAllNoKeyUserTestIdsClick = {
            feature.accept(Experiments.Msg.OnRemoveAllNoKeyUserTestIdsClick)
        }
    )

    when (val alertState = state.alert) {
        is Experiments.AlertDialogState.RemoveTestId -> {
            ConfirmRemoveTestIdDialog(
                removedTestId = alertState.testId,
                onConfirm = { feature.accept(Experiments.Msg.OnUserTestIdRemoveConfirm(it)) },
                onDismissRequest = { feature.accept(Experiments.Msg.OnUserTestIdRemoveDismiss) }
            )
        }
        is Experiments.AlertDialogState.RemoveAllNoKeyTestIds -> {
            ConfirmRemoveAllNoKeyTestIds(
                onConfirm = { feature.accept(Experiments.Msg.OnRemoveAllNoKeyUserTestIdsConfirm) },
                onDismissRequest = { feature.accept(Experiments.Msg.OnRemoveAllNoKeyUserTestIdsDismiss) }
            )
        }
        is Experiments.AlertDialogState.Hidden -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentsScaffold(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    experiments: LoadableData<ExperimentsStore>,
    onTestIdClick: (ExperimentEntity?, TestIdEntity?) -> Unit,
    onUserTestIdLongClick: (TestIdEntity) -> Unit,
    onRemoveAllNoKeyUserTestIdsClick: () -> Unit,
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ExperimentsTopAppBar(
                onBackClick = onBackClick,
                onAddClick = onAddClick,
                scrollBehavior = scrollBehavior
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Content(
                experiments = experiments,
                onTestIdClick = onTestIdClick,
                onUserTestIdLongClick = onUserTestIdLongClick,
                onRemoveAllNoKeyUserTestIdsClick = onRemoveAllNoKeyUserTestIdsClick,
            )
        }
    }
}

@Composable
private fun ExperimentsTopAppBar(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    CenterAlignedTopAppBar(
        modifier = Modifier.statusBarsPadding(),
        scrollBehavior = scrollBehavior,
        title = { Text(stringResource(R.string.experiments)) },
        navigationIcon = { BackIconButton(onClick = onBackClick) },
        actions = { AddIconButton(onClick = onAddClick) }
    )
}

@Composable
private fun Content(
    experiments: LoadableData<ExperimentsStore>,
    onTestIdClick: (ExperimentEntity?, TestIdEntity?) -> Unit,
    onUserTestIdLongClick: (TestIdEntity) -> Unit,
    onRemoveAllNoKeyUserTestIdsClick: () -> Unit,
) {
    when (experiments) {
        is LoadableData.Initial, is LoadableData.Loading -> {
            Loading()
        }
        is LoadableData.Failure -> {
            Error(experiments.error)
        }
        is LoadableData.Success -> {
            if (experiments.value.experiments.isNotEmpty()) {
                ExperimentsList(
                    experiments = experiments.value,
                    onTestIdClick = onTestIdClick,
                    onUserTestIdLongClick = onUserTestIdLongClick,
                    onRemoveAllNoKeyUserTestIdsClick = onRemoveAllNoKeyUserTestIdsClick
                )
            } else {
                Empty(stringResource(R.string.experiments_nothing_found))
            }
        }
    }
}

@Composable
private fun ExperimentsList(
    experiments: ExperimentsStore,
    onTestIdClick: (ExperimentEntity?, TestIdEntity?) -> Unit,
    onUserTestIdLongClick: (TestIdEntity) -> Unit,
    onRemoveAllNoKeyUserTestIdsClick: () -> Unit,
) {
    val noKeyText = stringResource(id = R.string.no_key)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = DimenTokens.x2),
    ) {

        experiments.experiments.values.forEachIndexed { index, experiment ->
            experimentItemHeader(
                text = experiment.name ?: experiment.key,
                ticket = experiment.ticket
            )
            val anyUserTestIdsSelected = experimentTestIdItems(
                experiment = experiment,
                testIds = experiments.userTestIds[experiment.key].orEmpty(),
                selectedTestIds = experiments.selectedTestIds,
                addedByUser = true,
                onClick = onTestIdClick,
                onLongClick = onUserTestIdLongClick
            )
            val anyTestIdsSelected = experimentTestIdItems(
                experiment = experiment,
                testIds = experiments.testIds[experiment.key].orEmpty(),
                selectedTestIds = experiments.selectedTestIds,
                addedByUser = false,
                onClick = onTestIdClick,
                onLongClick = null
            )
            experimentDefaultTestIdItem(
                experiment.defaultDescription,
                selected = !anyTestIdsSelected && !anyUserTestIdsSelected,
                onClick = { onTestIdClick(experiment, null) }
            )
            if (index != experiments.experiments.size - 1) {
                item { Spacer(modifier = Modifier.size(DimenTokens.x2)) }
            }
        }
        if (experiments.noKeyUserTestIds.isNotEmpty()) {
            item { Spacer(modifier = Modifier.size(DimenTokens.x2)) }
            experimentItemHeader(
                text = noKeyText,
                action = {
                    Box(modifier = Modifier
                        .alignByBaseline()
                        .defaultMinSize(minHeight = DimenTokens.x12)
                        .clickable(
                            onClick = onRemoveAllNoKeyUserTestIdsClick,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(
                                    start = DimenTokens.x2,
                                    top = DimenTokens.x2,
                                    bottom = DimenTokens.x2,
                                ),
                            text = stringResource(id = R.string.remove_all),
                            color = AutoTheme.colorScheme.secondary,
                            style = AutoTheme.typography.body1Medium
                        )
                    }
                }
            )
            experiments.noKeyUserTestIds.forEachIndexed { index, testId ->
                item {
                    TestIdItem(
                        testId = testId.testId,
                        description = testId.description,
                        selected = testId.testId in experiments.selectedTestIds,
                        onClick = { onTestIdClick(null, testId) },
                        onLongClick = { onUserTestIdLongClick(testId) },
                        addedByUser = true,
                        singleSelect = false,
                        modifier = if (index == experiments.noKeyUserTestIds.lastIndex) {
                            Modifier
                                .clip(AutoTheme.shapes.large.bottom)
                                .background(AutoTheme.colorScheme.surface)
                                .padding(bottom = DimenTokens.x2)
                        } else {
                            Modifier.background(AutoTheme.colorScheme.surface)
                        }
                    )
                }
            }
        }
    }
}

fun LazyListScope.experimentItemHeader(
    text: String,
    ticket: String? = null,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    item {
        Row(
            modifier = Modifier
                .clip(AutoTheme.shapes.large.top)
                .background(AutoTheme.colorScheme.surface)
                .padding(horizontal = DimenTokens.x4)
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .alignByBaseline()
                    .weight(1f)
                    .padding(
                        top = DimenTokens.x4,
                        bottom = DimenTokens.x2
                    ),
                style = AutoTheme.typography.headline5Bold,
            )
            ticket?.let {
                Text(
                    text = ticket,
                    modifier = Modifier
                        .alignByBaseline()
                        .padding(start = DimenTokens.x2),
                    style = AutoTheme.typography.captionMedium,
                    color = AutoTheme.colorScheme.onSurface.contentEmphasisMedium
                )
            }
            action?.invoke(this)
        }
    }
}

fun LazyListScope.experimentTestIdItems(
    experiment: ExperimentEntity,
    testIds: List<TestIdEntity>,
    selectedTestIds: Set<String>,
    addedByUser: Boolean,
    onClick: (ExperimentEntity, TestIdEntity?) -> Unit,
    onLongClick: ((TestIdEntity) -> Unit)?,
): Boolean {
    var anySelected = false
    testIds.forEach { testId ->
        val selected = testId.testId in selectedTestIds
        anySelected = anySelected || selected
        item(experiment.key + testId.testId) {
            TestIdItem(
                testId = testId.testId,
                description = testId.description,
                selected = selected,
                onClick = { onClick(experiment, testId) },
                onLongClick = onLongClick?.let { { it(testId) } },
                addedByUser = addedByUser,
                modifier = Modifier.background(AutoTheme.colorScheme.surface)
            )
            Divider(
                Modifier
                    .fillMaxWidth()
                    .background(AutoTheme.colorScheme.surface)
                    .padding(horizontal = DimenTokens.x4)
            )
        }
    }
    return anySelected
}

fun LazyListScope.experimentDefaultTestIdItem(
    description: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    item {
        TestIdItem(
            testId = "Default",
            description = description,
            selected = selected,
            onClick = onClick,
            onLongClick = {},
            addedByUser = false,
            modifier = Modifier
                .clip(AutoTheme.shapes.large.bottom)
                .background(AutoTheme.colorScheme.surface)
                .padding(bottom = DimenTokens.x2)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TestIdItem(
    testId: String,
    description: String?,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    addedByUser: Boolean,
    modifier: Modifier = Modifier,
    singleSelect: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = DimenTokens.x12)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = DimenTokens.x4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (singleSelect) {
            RadioButton(
                selected = selected,
                onClick = null
            )
        } else {
            Checkbox(
                checked = selected,
                onCheckedChange = null
            )
        }
        TestIdText(
            modifier = Modifier
                .padding(start = DimenTokens.x4)
                .weight(1f),
            testId = testId,
            description = description,
            textStyle = AutoTheme.typography.subtitle
        )
        if (addedByUser) {
            Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = "Added by user",
                tint = androidx.compose.material3.LocalContentColor.current.contentEmphasisLow
            )
        }
    }
}

@Composable
private fun TestIdText(
    modifier: Modifier,
    testId: String,
    description: String?,
    textStyle: TextStyle = AutoTheme.typography.body1,
) {
    Row(modifier = modifier) {
        Text(
            text = testId,
            style = textStyle,
            color = AutoTheme.colorScheme.onSurface
        )
        if (description.isNotNullOrEmpty()) {
            Text(
                text = " — ",
                style = textStyle,
                color = AutoTheme.colorScheme.onSurface.contentEmphasisMedium
            )
            Text(
                text = description.orEmpty(),
                style = textStyle,
                color = AutoTheme.colorScheme.onSurface.contentEmphasisMedium
            )
        }
    }
}

@Composable
private fun ConfirmRemoveTestIdDialog(
    removedTestId: TestIdEntity,
    onConfirm: (TestIdEntity) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ConfirmAlertDialog(
        title = stringResource(R.string.remove_test_id) + " ${removedTestId.testId}?",
        onConfirm = { onConfirm(removedTestId) },
        onDismissRequest = onDismissRequest
    )
}

@Composable
private fun ConfirmRemoveAllNoKeyTestIds(
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    ConfirmAlertDialog(
        title = stringResource(R.string.remove_all_no_key_test_ids),
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest
    )
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_4
)
@Composable
fun ExperimentsScaffoldPreview() {
    AutoTheme {
        val experimentKey1 = "key1"
        val experimentKey2 = "key2"
        val testId1 = "111111"
        val testId2 = "111112"
        ExperimentsScaffold(
            experiments = LoadableData.Success(
                ExperimentsStore(
                    experiments = mapOf(
                        experimentKey1 to ExperimentEntity(
                            key = experimentKey1,
                            name = "Card gallery ad",
                            ticket = "AUTORUAPPS-18544",
                            defaultDescription = "Disabled"
                        ),
                        experimentKey2 to ExperimentEntity(
                            key = experimentKey2,
                            name = "Quarantine callback for dealers used cars",
                            ticket = "AUTORUAPPS-18545",
                            defaultDescription = "Disabled"
                        )
                    ),
                    testIds = mapOf(
                        experimentKey1 to listOf(
                            TestIdEntity(
                                experimentKey = experimentKey1,
                                testId = testId1,
                                description = "Enabled"
                            )
                        ),
                        experimentKey2 to listOf(
                            TestIdEntity(
                                experimentKey = experimentKey2,
                                testId = testId2,
                                description = "Enabled"
                            )
                        )
                    ),
                    userTestIds = emptyMap(),
                    selectedTestIds = setOf(testId2),
                )
            ),
            onBackClick = {},
            onAddClick = {},
            onTestIdClick = { _, _ -> },
            onUserTestIdLongClick = {},
            onRemoveAllNoKeyUserTestIdsClick = {}
        )
    }
}
