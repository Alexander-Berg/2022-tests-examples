package ru.auto.test.experiments.di

import ru.auto.core_ui.tea.TeaFeatureSimpleSet
import ru.auto.core_ui.tea.wrapWithEffectReplay
import ru.auto.test.common.data.ExperimentsRepository
import ru.auto.test.experiments.presentation.Experiments
import ru.auto.test.experiments.presentation.ExperimentsFeature
import ru.auto.test.experiments.presentation.wrapWithExperimentsEffectHandler

fun provideExperimentsFeature(deps: ExperimentsFeatureDependencies): ExperimentsFeature =
    TeaFeatureSimpleSet(
        initialState = Experiments.State(),
        reducer = Experiments::reduce
    ).wrapWithExperimentsEffectHandler(
        deps.experimentsRepository
    ).wrapWithEffectReplay()

interface ExperimentsFeatureDependencies {
    val experimentsRepository: ExperimentsRepository
}
