package ru.yandex.yandexmaps.overlays.internal.redux.general

import org.junit.Test
import ru.yandex.yandexmaps.overlays.api.EnabledOverlay
import ru.yandex.yandexmaps.overlays.api.Overlay
import ru.yandex.yandexmaps.overlays.api.TransportMode
import ru.yandex.yandexmaps.overlays.api.transportMode
import ru.yandex.yandexmaps.overlays.internal.redux.ChangeOverlay.Disable
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.carparksEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.panoramaEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.trafficEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.DefaultStates.transportEnabledState
import ru.yandex.yandexmaps.overlays.internal.redux.reduce
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class DisableTest {

    @Test
    fun `carparks disabling works correct`() {
        val reduced = carparksEnabledState.reduce(Disable(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.None)
    }

    @Test
    fun `disabling panorama does not affect enabled carparks`() {
        val reduced = carparksEnabledState.reduce(Disable(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `disabling traffic does not affect enabled carparks`() {
        val reduced = carparksEnabledState.reduce(Disable(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `disabling transport does not affect enabled carparks`() {
        val reduced = carparksEnabledState.reduce(Disable(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `panorama disabling works correct`() {
        val reduced = panoramaEnabledState.reduce(Disable(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.None)
    }

    @Test
    fun `disabling carparks does not affect enabled panorama`() {
        val reduced = panoramaEnabledState.reduce(Disable(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `disabling traffic does not affect enabled panorama`() {
        val reduced = panoramaEnabledState.reduce(Disable(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `disabling transport does not affect enabled panorama`() {
        val reduced = panoramaEnabledState.reduce(Disable(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `traffic disabling works correct`() {
        val reduced = trafficEnabledState.reduce(Disable(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.None)
    }

    @Test
    fun `disabling carparks does not affect enabled traffic`() {
        val reduced = trafficEnabledState.reduce(Disable(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `disabling panorama does not affect enabled traffic`() {
        val reduced = trafficEnabledState.reduce(Disable(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `disabling transport does not affect enabled traffic`() {
        val reduced = trafficEnabledState.reduce(Disable(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `transport disabling works correct`() {
        val reduced = transportEnabledState.reduce(Disable(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.None)
    }

    @Test
    fun `disabling carparks does not affect enabled transport`() {
        val reduced = transportEnabledState.reduce(Disable(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.transportMode()).isNotEqualTo(TransportMode.Hidden)
    }

    @Test
    fun `disabling panorama does not affect enabled transport`() {
        val reduced = transportEnabledState.reduce(Disable(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.transportMode()).isNotEqualTo(TransportMode.Hidden)
    }

    @Test
    fun `disabling traffic does not affect enabled transport`() {
        val reduced = transportEnabledState.reduce(Disable(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.transportMode()).isNotEqualTo(TransportMode.Hidden)
    }
}
