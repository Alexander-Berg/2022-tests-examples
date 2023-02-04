package ru.auto.test.testid.presentation

import ru.auto.ara.util.backgroundToUi
import ru.auto.data.util.maybeValue
import ru.auto.test.common.data.ExperimentsRepository
import ru.auto.test.common.presentation.wrapWithObservableEffectHandler
import rx.Single

fun AddTestIdFeature.wrapWithAddTestIdsEffectHandler(
    experimentsRepository: ExperimentsRepository,
) = wrapWithObservableEffectHandler(
    initialEffects = setOf(AddTestId.Eff.ObserveExperimentsKeys)
) {
    ofType(AddTestId.Eff.ObserveExperimentsKeys::class.java)
        .switchMap {
            experimentsRepository.observeExperiments()
                .map<AddTestId.Msg> { experiments ->
                    AddTestId.Msg.OnExperimentsChanged(
                        experiments = experiments,
                        keys = experiments.maybeValue?.experiments?.values?.map { it.key }.orEmpty()
                    )
                }
                .backgroundToUi()
        }
}.wrapWithObservableEffectHandler {
    ofType(AddTestId.Eff.SaveTestId::class.java)
        .flatMap {
            experimentsRepository.addUserTestId(it.testId, it.experiments)
                .andThen(Single.just(AddTestId.Msg.OnTestIdSaved))
                .toObservable()
                .backgroundToUi()
        }
}
