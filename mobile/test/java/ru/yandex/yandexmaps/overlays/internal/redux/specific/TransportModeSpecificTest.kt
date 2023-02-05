package ru.yandex.yandexmaps.overlays.internal.redux.specific

import org.junit.Test
import ru.yandex.yandexmaps.overlays.api.OverlaysState
import ru.yandex.yandexmaps.overlays.api.TransportMode
import ru.yandex.yandexmaps.overlays.api.TransportMode.Vehicles.Availability.AVAILABLE
import ru.yandex.yandexmaps.overlays.api.TransportMode.Vehicles.Availability.UNAVAILABLE
import ru.yandex.yandexmaps.overlays.api.transportMode
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.transportEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.TransportAction
import ru.yandex.yandexmaps.overlays.internal.redux.reduce
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class TransportModeSpecificTest {

    private fun OverlaysState.vehicles(block: TransportMode.Vehicles.() -> Unit) {
        expectThat(transportMode()).isA<TransportMode.Vehicles>()
        block(transportMode() as TransportMode.Vehicles)
    }

    @Test
    fun `change to regions works correct`() {
        val vehiclesTransportState = DefaultStates.withEnabled(TransportMode.Vehicles())
        val reduced = vehiclesTransportState.reduce(TransportAction.ChangeToRegions)

        expectThat(reduced.transportMode()).isA<TransportMode.Regions>()
    }

    @Test
    fun `change to vehicles works correct`() {
        val regionsTransportState = DefaultStates.withEnabled(TransportMode.Regions())
        val reduced = regionsTransportState.reduce(TransportAction.ChangeToVehicles)

        expectThat(reduced.transportMode()).isA<TransportMode.Vehicles>()
    }

    @Test
    fun `changing transport from expected to available on span works correct`() {
        val reduced = transportEnabledState.reduce(TransportAction.ChangeTransportToAvailable)
        reduced.vehicles {
            expectThat(availability).isEqualTo(AVAILABLE)
        }
    }

    @Test
    fun `changing transport from expected to unavailable on span works correct`() {
        val reduced = transportEnabledState.reduce(TransportAction.ChangeTransportToUnavailable)
        reduced.vehicles {
            expectThat(availability).isEqualTo(UNAVAILABLE)
        }
    }
}
