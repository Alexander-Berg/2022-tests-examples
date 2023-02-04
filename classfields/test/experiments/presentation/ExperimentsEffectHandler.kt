package ru.auto.test.experiments.presentation

import ru.auto.ara.util.backgroundToUi
import ru.auto.test.common.data.ExperimentsRepository
import ru.auto.test.common.presentation.wrapWithCompletableEffectHandler
import ru.auto.test.common.presentation.wrapWithObservableEffectHandler

fun ExperimentsFeature.wrapWithExperimentsEffectHandler(
    experimentsRepository: ExperimentsRepository,
) = wrapWithCompletableEffectHandler(
    initialEffects = setOf(Experiments.Eff.FetchExperiments)
) {
    ofType(Experiments.Eff.FetchExperiments::class.java)
        .flatMapCompletable {
            experimentsRepository
                .fetchExperiments()
                .backgroundToUi()
        }
        .toCompletable()
}.wrapWithObservableEffectHandler(
    initialEffects = setOf(Experiments.Eff.ObserveExperiments)
) {
    ofType(Experiments.Eff.ObserveExperiments::class.java)
        .switchMap {
            experimentsRepository.observeExperiments()
                .map<Experiments.Msg> { Experiments.Msg.OnExperimentsChanged(it) }
                .backgroundToUi()
        }
}.wrapWithCompletableEffectHandler {
    ofType(Experiments.Eff.UpdateSelectedTestId::class.java)
        .flatMap {
            experimentsRepository.updateExperimentSelectedTestId(it.experimentKey, it.testId, it.experiments)
                .toObservable<Experiments.Msg>()
                .backgroundToUi()
        }
        .toCompletable()
}.wrapWithCompletableEffectHandler {
    ofType(Experiments.Eff.RemoveUserTestId::class.java)
        .flatMapCompletable {
            experimentsRepository
                .removeUserTestId(it.testId, it.experiments)
                .backgroundToUi()
        }
        .toCompletable()
}.wrapWithCompletableEffectHandler {
    ofType(Experiments.Eff.RemoveAllNoKeyUserTestIds::class.java)
        .flatMapCompletable {
            experimentsRepository
                .removeNoKeyUserTestIds(it.experiments)
                .backgroundToUi()
        }
        .toCompletable()
}
