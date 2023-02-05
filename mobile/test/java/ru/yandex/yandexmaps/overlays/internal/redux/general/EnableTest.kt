package ru.yandex.yandexmaps.overlays.internal.redux.general

import org.junit.Test
import ru.yandex.yandexmaps.overlays.api.EnabledOverlay
import ru.yandex.yandexmaps.overlays.api.Overlay
import ru.yandex.yandexmaps.overlays.api.TransportMode
import ru.yandex.yandexmaps.overlays.api.transportMode
import ru.yandex.yandexmaps.overlays.internal.redux.ChangeOverlay.Enable
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.carparksEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.noneEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.panoramaEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.trafficEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.transportEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.reduce
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class EnableTest {

    @Test
    fun `carparks enabling works correct`() {
        val reduced = noneEnabledState.reduce(Enable(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `panorama enabling works correct`() {
        val reduced = noneEnabledState.reduce(Enable(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `traffic enabling works correct`() {
        val reduced = noneEnabledState.reduce(Enable(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `transport enabling works correct`() {
        val reduced = noneEnabledState.reduce(Enable(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.transportMode()).isNotEqualTo(TransportMode.Hidden)
    }

    @Test
    fun `enabling carparks does not change the state if it is already enabled`() {
        val reduced = carparksEnabledState.reduce(Enable(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `enabling panorama works if carparks are enabled`() {
        val reduced = carparksEnabledState.reduce(Enable(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `enabling traffic works if carparks are enabled`() {
        val reduced = carparksEnabledState.reduce(Enable(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `enabling transport works if carparks are enabled`() {
        val reduced = carparksEnabledState.reduce(Enable(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.transportMode()).isNotEqualTo(TransportMode.Hidden)
    }

    @Test
    fun `enabling carparks works if panorama is enabled`() {
        val reduced = panoramaEnabledState.reduce(Enable(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `enabling panorama does not change the state if it is already enabled`() {
        val reduced = panoramaEnabledState.reduce(Enable(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `enabling traffic works if panorama is enabled`() {
        val reduced = panoramaEnabledState.reduce(Enable(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `enabling transport works if panorama is enabled`() {
        val reduced = panoramaEnabledState.reduce(Enable(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.transportMode()).isA<TransportMode.Vehicles>()
    }

    @Test
    fun `enabling carparks works if traffic is enabled`() {
        val reduced = trafficEnabledState.reduce(Enable(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `enabling panorama works if traffic is enabled`() {
        val reduced = trafficEnabledState.reduce(Enable(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `enabling traffic does not change the state if it is already enabled`() {
        val reduced = trafficEnabledState.reduce(Enable(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `enabling transport works if traffic is enabled`() {
        val reduced = trafficEnabledState.reduce(Enable(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.transportMode()).isA<TransportMode.Vehicles>()
    }

    @Test
    fun `enabling carparks works if transport is enabled`() {
        val reduced = transportEnabledState.reduce(Enable(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `enabling panorama works if transport is enabled`() {
        val reduced = transportEnabledState.reduce(Enable(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `enabling traffic works if transport is enabled`() {
        val reduced = transportEnabledState.reduce(Enable(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `enabling transport does not change the state if it is already enabled`() {
        val reduced = transportEnabledState.reduce(Enable(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.transportMode()).isNotEqualTo(TransportMode.Hidden)
    }
}
