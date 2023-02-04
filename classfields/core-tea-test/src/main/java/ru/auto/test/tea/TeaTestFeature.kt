package ru.auto.test.tea

import io.qameta.allure.kotlin.Allure
import ru.auto.core_ui.tea.Feature
import ru.auto.core_ui.tea.TeaSetReducer
import ru.auto.core_ui.util.Disposable
import ru.auto.testextension.parameterJson

class TeaTestFeature<Msg : Any, State : Any, Effect : Any>(
    private val initialState: State,
    private val reducer: TeaSetReducer<Msg, State, Effect>
) : Feature<Msg, State, Effect> {
    init {
        Allure.step("Feature created with initialState") {
            parameterJson("state", initialState)
        }
    }
    override var currentState: State = initialState
        private set
    var latestEffects = emptySet<Effect>()
        private set
    override fun accept(msg: Msg) {
        Allure.step("Accepting message ${msg.javaClass.simpleName}") {
            parameterJson("message", msg)
            val (state, effects) = reducer(msg, currentState)
            Allure.step("Produced state and effects ${effects.joinToString { it.javaClass.simpleName }}") {
                parameterJson("state", state)
                parameterJson("effects", effects)
                currentState = state
                latestEffects = effects
            }
        }
    }
    override fun dispose() = Allure.step("Feature disposal requested")
    override fun subscribe(stateConsumer: (state: State) -> Unit, effectConsumer: (eff: Effect) -> Unit): Disposable {
        stateConsumer(initialState)
        Allure.step("Feature was subscribed")
        return object : Disposable {
            override fun dispose() = Unit
        }
    }
}
