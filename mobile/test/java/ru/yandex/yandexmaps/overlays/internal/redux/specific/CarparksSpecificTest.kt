package ru.yandex.yandexmaps.overlays.internal.redux.specific

import org.junit.Test
import ru.yandex.yandexmaps.overlays.api.EnabledOverlay
import ru.yandex.yandexmaps.overlays.api.EnabledOverlay.Carparks.Availability.AVAILABLE
import ru.yandex.yandexmaps.overlays.api.EnabledOverlay.Carparks.Availability.UNAVAILABLE
import ru.yandex.yandexmaps.overlays.api.OverlaysState
import ru.yandex.yandexmaps.overlays.internal.redux.CarparksAction
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.carparksEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.reduce
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class CarparksSpecificTest {

    private fun OverlaysState.carparks(block: EnabledOverlay.Carparks.() -> Unit) {
        expectThat(enabledOverlay).isA<EnabledOverlay.Carparks>()
        block(enabledOverlay as EnabledOverlay.Carparks)
    }

    @Test
    fun `changing availability from expected to available works correct`() {
        val reduced = carparksEnabledState.reduce(CarparksAction.ChangeToAvailable)
        reduced.carparks {
            expectThat(availability).isEqualTo(AVAILABLE)
        }
    }

    @Test
    fun `changing availability from expected to unavailable works correct`() {
        val reduced = carparksEnabledState.reduce(CarparksAction.ChangeToUnavailable)
        reduced.carparks {
            expectThat(availability).isEqualTo(UNAVAILABLE)
        }
    }
}
