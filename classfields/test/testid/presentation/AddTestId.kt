package ru.auto.test.testid.presentation

import ru.auto.core_ui.tea.Feature
import ru.auto.data.util.LoadableData
import ru.auto.data.util.maybeValue
import ru.auto.settings.provider.ExperimentKey
import ru.auto.settings.provider.TestId
import ru.auto.settings.provider.TestIdEntity
import ru.auto.test.common.model.ExperimentsStore

typealias AddTestIdFeature = Feature<AddTestId.Msg, AddTestId.State, AddTestId.Eff>
typealias AddTestIdReduceResult = Pair<AddTestId.State, Set<AddTestId.Eff>>

object AddTestId {

    data class State(
        val testId: TestId = "",
        val description: String = "",
        val key: ExperimentKey = "",
        val experiments: LoadableData<ExperimentsStore> = LoadableData.Initial,
        val keys: List<String> = emptyList(),
        val checkEmptyTestId: Boolean = false
    ) {

        val testIdError: TestIdError? = when {
            testIdsByKey.any { it.testId == testId } -> TestIdError.AlreadyExist
            checkEmptyTestId && testId.isBlank() -> TestIdError.Empty
            else -> null
        }

        val isValid get() = testIdError == null

        private val testIdsByKey get() = experiments.maybeValue?.let {
            it.testIds[key].orEmpty() + it.userTestIds[key].orEmpty()
        }.orEmpty()
    }

    enum class TestIdError {
        AlreadyExist,
        Empty
    }

    sealed class Msg {
        data class OnDescriptionChange(val description: String) : Msg()
        data class OnKeyChange(val key: ExperimentKey) : Msg()
        data class OnTestIdChange(val testId: TestId) : Msg()

        object OnSaveClick : Msg()
        object OnCloseClick : Msg()

        data class OnExperimentsChanged(
            val experiments: LoadableData<ExperimentsStore>,
            val keys: List<String>,
        ) : Msg()

        object OnTestIdSaved : Msg()
    }

    sealed class Eff {
        object ObserveExperimentsKeys : Eff()

        data class SaveTestId(val testId: TestIdEntity, val experiments: ExperimentsStore) : Eff()

        object Close : Eff()
    }

    fun reduce(msg: Msg, state: State): AddTestIdReduceResult = when (msg) {
        is Msg.OnKeyChange -> {
            state.copy(key = msg.key) to setOf()
        }
        is Msg.OnDescriptionChange -> {
            state.copy(description = msg.description) to setOf()
        }
        is Msg.OnTestIdChange -> {
            state.copy(testId = msg.testId, checkEmptyTestId = false) to setOf()
        }
        is Msg.OnSaveClick -> {
            val checkedState = state.copy(checkEmptyTestId = true)
            val experimentsStore = state.experiments.maybeValue

            if (checkedState.isValid && experimentsStore != null) {
                checkedState to setOf(
                    Eff.SaveTestId(
                        experiments = experimentsStore,
                        testId = TestIdEntity(
                            experimentKey = state.key,
                            testId = state.testId,
                            description = state.description
                        )
                    )
                )
            } else {
                checkedState to setOf()
            }
        }
        is Msg.OnExperimentsChanged -> {
            state.copy(
                experiments = msg.experiments,
                keys = msg.keys
            ) to setOf()
        }
        is Msg.OnTestIdSaved -> {
            state to setOf(Eff.Close)
        }
        is Msg.OnCloseClick -> {
            state to setOf(Eff.Close)
        }
    }

}
