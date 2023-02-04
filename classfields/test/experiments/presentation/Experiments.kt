package ru.auto.test.experiments.presentation

import ru.auto.core_ui.tea.Feature
import ru.auto.data.util.LoadableData
import ru.auto.settings.provider.ExperimentKey
import ru.auto.settings.provider.TestIdEntity
import ru.auto.test.common.model.ExperimentsStore

typealias ExperimentsFeature = Feature<Experiments.Msg, Experiments.State, Experiments.Eff>
typealias ExperimentsReduceResult = Pair<Experiments.State, Set<Experiments.Eff>>

object Experiments {

    data class State(
        val content: LoadableData<ExperimentsStore> = LoadableData.Initial,
        val alert: AlertDialogState = AlertDialogState.Hidden,
    )

    sealed class AlertDialogState {
        class RemoveTestId(val testId: TestIdEntity) : AlertDialogState()
        object RemoveAllNoKeyTestIds : AlertDialogState()
        object Hidden : AlertDialogState()
    }

    sealed class Msg {

        data class OnTestIdClick(val experimentKey: ExperimentKey?, val testId: TestIdEntity?) : Msg()
        data class OnUserTestIdLongClick(val testId: TestIdEntity) : Msg()
        data class OnUserTestIdRemoveClick(val testId: TestIdEntity) : Msg()
        data class OnUserTestIdRemoveConfirm(val testId: TestIdEntity) : Msg()
        object OnUserTestIdRemoveDismiss : Msg()

        object OnRemoveAllNoKeyUserTestIdsClick : Msg()
        object OnRemoveAllNoKeyUserTestIdsConfirm : Msg()
        object OnRemoveAllNoKeyUserTestIdsDismiss : Msg()

        class OnExperimentsChanged(val experiments: LoadableData<ExperimentsStore>) : Msg()
    }

    sealed class Eff {
        object FetchExperiments : Eff()
        object ObserveExperiments : Eff()
        class UpdateSelectedTestId(
            val experimentKey: ExperimentKey?,
            val testId: TestIdEntity?,
            val experiments: ExperimentsStore,
        ) : Eff()

        class RemoveUserTestId(
            val testId: TestIdEntity,
            val experiments: ExperimentsStore,
        ) : Eff()

        class RemoveAllNoKeyUserTestIds(val experiments: ExperimentsStore) : Eff()
    }

    fun reduce(msg: Msg, state: State): ExperimentsReduceResult = when (msg) {
        is Msg.OnTestIdClick -> {
            state to if (state.content is LoadableData.Success) {
                setOf(
                    Eff.UpdateSelectedTestId(msg.experimentKey, msg.testId, state.content.value),
                )
            } else {
                setOf()
            }
        }
        is Msg.OnExperimentsChanged -> {
            state.copy(content = msg.experiments) to setOf()
        }
        is Msg.OnUserTestIdLongClick -> {
            state.copy(
                alert = AlertDialogState.RemoveTestId(msg.testId)
            ) to setOf()
        }
        is Msg.OnUserTestIdRemoveConfirm -> {
            state.copy(
                alert = AlertDialogState.Hidden
            ) to if (state.content is LoadableData.Success) {
                setOf(Eff.RemoveUserTestId(msg.testId, state.content.value))
            } else {
                setOf()
            }
        }
        is Msg.OnUserTestIdRemoveDismiss -> {
            state.copy(
                alert = AlertDialogState.Hidden,
            ) to setOf()
        }
        is Msg.OnUserTestIdRemoveClick -> {
            state.copy(
                alert = AlertDialogState.RemoveTestId(msg.testId)
            ) to setOf()
        }
        is Msg.OnRemoveAllNoKeyUserTestIdsClick -> {
            state.copy(
                alert = AlertDialogState.RemoveAllNoKeyTestIds
            ) to setOf()
        }
        is Msg.OnRemoveAllNoKeyUserTestIdsConfirm -> {
            if (state.content is LoadableData.Success) {
                state.copy(
                    alert = AlertDialogState.Hidden,
                ) to setOf(
                    Eff.RemoveAllNoKeyUserTestIds(state.content.value),
                )
            } else {
                state to setOf()
            }
        }
        is Msg.OnRemoveAllNoKeyUserTestIdsDismiss -> {
            state.copy(
                alert = AlertDialogState.Hidden,
            ) to setOf()
        }
    }

}
