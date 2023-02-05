package ru.yandex.market.di

import ru.yandex.market.clean.data.store.AutoDetectedRegionDataStore
import ru.yandex.market.clean.domain.model.DeliveryAvailability
import ru.yandex.market.mocks.State
import ru.yandex.market.mocks.state.AutoDetectedByCoordinatesRegionState
import ru.yandex.market.mocks.state.AutoDetectedRegionState
import ru.yandex.market.mocks.tryObtain
import javax.inject.Inject

class TestAutoDetectedRegionDataStore @Inject constructor(
    private val states: List<State>
) : AutoDetectedRegionDataStore {

    override var autoDetectedRegionId: Long?
        get() = states.tryObtain<AutoDetectedRegionState>()?.regionId
        set(value) {
            // no-op
        }

    override var autoDetectedRegionByCoordinates: DeliveryAvailability?
        get() = states.tryObtain<AutoDetectedByCoordinatesRegionState>()?.deliveryAvailability
        set(value) {
            // no-op
        }
}