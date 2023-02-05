package ru.yandex.disk.redux.testutil

import org.assertj.core.api.Assertions
import ru.yandex.disk.redux.Action
import ru.yandex.disk.redux.Model
import ru.yandex.disk.redux.StateMachine

fun <STATE: Any, MODEL: Model, EVENT: Action> StateMachine.Transition<STATE, MODEL, EVENT>.contains(from: STATE, to: STATE, event: EVENT) {
    Assertions.assertThat(this.from).isEqualTo(from)
    Assertions.assertThat(this.event).isEqualTo(event)
    Assertions.assertThat(this.to).isEqualTo(to)
}
