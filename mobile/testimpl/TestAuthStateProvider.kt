package ru.yandex.yandexmaps.multiplatform.polling.internal.testimpl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import ru.yandex.yandexmaps.multiplatform.polling.api.dependencies.PollingAuthState
import ru.yandex.yandexmaps.multiplatform.polling.api.dependencies.PollingAuthStateProvider

internal class TestAuthStateProvider(states: List<PollingAuthState>) : PollingAuthStateProvider {
    private val values = states.asFlow()
    override val currentState = states[0]

    override fun states(): Flow<PollingAuthState> = values
}

internal class MutableTestAuthStateProvider(initialState: PollingAuthState) : PollingAuthStateProvider {
    private val subj = MutableStateFlow(initialState)

    suspend fun emit(value: PollingAuthState) {
        subj.emit(value)
    }

    override val currentState: PollingAuthState
        get() = subj.value

    override fun states(): Flow<PollingAuthState> = subj
}
