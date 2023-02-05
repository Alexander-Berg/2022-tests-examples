package ru.yandex.market.test.rule

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import ru.yandex.market.mocks.State
import ru.yandex.market.mocks.StateController

class InitialStateTestRule(
    private val stateController: StateController,
    private val states: List<State>
) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return StateStatement(base, stateController, states)
    }

    private class StateStatement(
        private val base: Statement,
        private val stateController: StateController,
        private val states: List<State>
    ) : Statement() {

        override fun evaluate() {
            stateController.setState(*states.toTypedArray())
            base.evaluate()
        }
    }
}