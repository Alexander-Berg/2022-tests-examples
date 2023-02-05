package ru.yandex.yandexmaps.multiplatform.polling.internal.testimpl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ru.yandex.yandexmaps.multiplatform.polling.internal.PollingServiceState
import ru.yandex.yandexmaps.multiplatform.redux.api.StateProvider

internal class TestStateProvider(initialState: PollingServiceState) : StateProvider<PollingServiceState> {

    private val statesSubj = MutableStateFlow(initialState)

    override val currentState: PollingServiceState
        get() = statesSubj.value

    override fun states(): Flow<PollingServiceState> = statesSubj
}
