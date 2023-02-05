package ru.yandex.yandexmaps.overlays.internal.redux.specific

import com.yandex.mapkit.traffic.TrafficColor
import org.junit.Test
import ru.yandex.yandexmaps.overlays.api.EnabledOverlay
import ru.yandex.yandexmaps.overlays.api.OverlaysState
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.trafficEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.TrafficAction
import ru.yandex.yandexmaps.overlays.internal.redux.reduce
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class TrafficSpecificTest {

    private fun OverlaysState.traffic(block: EnabledOverlay.Traffic.() -> Unit) {
        expectThat(enabledOverlay).isA<EnabledOverlay.Traffic>()
        block(enabledOverlay as EnabledOverlay.Traffic)
    }

    @Test
    fun `changing traffic from loading to loaded works correct`() {
        val reduced = trafficEnabledState.reduce(TrafficAction.ChangeToLoaded(10, TrafficColor.RED))
        reduced.traffic {
            expectThat(this).isA<EnabledOverlay.Traffic.Loaded>()
            val loaded = this as EnabledOverlay.Traffic.Loaded
            expectThat(loaded.value).isEqualTo(10)
            expectThat(loaded.color).isEqualTo(TrafficColor.RED)
        }
    }

    @Test
    fun `changing traffic from loading to unavailable works correct`() {
        val reduced = trafficEnabledState.reduce(TrafficAction.ChangeToUnavailable)

        reduced.traffic {
            expectThat(this).isEqualTo(EnabledOverlay.Traffic.Unavailable)
        }
    }

    @Test
    fun `changing traffic from unavailable to loading works correct`() {
        val trafficUnavailableState = DefaultStates.withEnabled(EnabledOverlay.Traffic.Unavailable)
        val reduced = trafficUnavailableState.reduce(TrafficAction.ChangeToLoading)
        reduced.traffic {
            expectThat(this).isEqualTo(EnabledOverlay.Traffic.Loading)
        }
    }
}
