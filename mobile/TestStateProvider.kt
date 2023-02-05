package ru.yandex.yandexmaps.multiplatform.taxi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ru.yandex.yandexmaps.multiplatform.redux.api.StateProvider
import ru.yandex.yandexmaps.multiplatform.taxi.internal.redux.state.TaxiRootState

internal class TestStateProvider(initialState: TaxiRootState) : StateProvider<TaxiRootState> {
    override val currentState: TaxiRootState = initialState

    override fun states(): Flow<TaxiRootState> = flowOf(currentState)
}
