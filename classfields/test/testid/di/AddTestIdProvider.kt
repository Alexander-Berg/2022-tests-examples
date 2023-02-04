package ru.auto.test.testid.di

import ru.auto.core_ui.tea.TeaFeatureSimpleSet
import ru.auto.test.common.data.ExperimentsRepository
import ru.auto.test.testid.presentation.AddTestId
import ru.auto.test.testid.presentation.AddTestIdFeature
import ru.auto.test.testid.presentation.wrapWithAddTestIdsEffectHandler

fun provideAddTestIdFeature(deps: AddTestIdFeatureDependencies): AddTestIdFeature =
    TeaFeatureSimpleSet(
        initialState = AddTestId.State(),
        reducer = AddTestId::reduce
    ).wrapWithAddTestIdsEffectHandler(
        deps.experimentsRepository
    )

interface AddTestIdFeatureDependencies {
    val experimentsRepository: ExperimentsRepository
}
