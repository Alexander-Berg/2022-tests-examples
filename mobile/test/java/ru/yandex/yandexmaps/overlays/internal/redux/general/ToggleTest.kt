package ru.yandex.yandexmaps.overlays.internal.redux.general

import org.junit.Test
import ru.yandex.yandexmaps.overlays.api.EnabledOverlay
import ru.yandex.yandexmaps.overlays.api.Overlay
import ru.yandex.yandexmaps.overlays.api.TransportMode
import ru.yandex.yandexmaps.overlays.api.transportMode
import ru.yandex.yandexmaps.overlays.internal.redux.ChangeOverlay.Toggle
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

class ToggleTest {

    @Test
    fun `toggling carparks enables carparks`() {
        val reduced = noneEnabledState.reduce(Toggle(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `toggling carparks disables carparks if carparks are enabled`() {
        val reduced = carparksEnabledState.reduce(Toggle(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.None)
    }

    @Test
    fun `toggling carparks enables carparks if panorama is enabled`() {
        val reduced = panoramaEnabledState.reduce(Toggle(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `toggling carparks enables carparks if traffic is enabled`() {
        val reduced = trafficEnabledState.reduce(Toggle(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `toggling carparks enables carparks if transport is enabled`() {
        val reduced = transportEnabledState.reduce(Toggle(Overlay.CARPARKS, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Carparks>()
    }

    @Test
    fun `toggling panorama enables panorama if disabled state`() {
        val reduced = noneEnabledState.reduce(Toggle(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `toggling panorama disables panorama if panorama is enabled`() {
        val reduced = panoramaEnabledState.reduce(Toggle(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.None)
    }

    @Test
    fun `toggling panorama enables panorama if carparks are enabled`() {
        val reduced = carparksEnabledState.reduce(Toggle(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `toggling panorama enables panorama if traffic is enabled`() {
        val reduced = trafficEnabledState.reduce(Toggle(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `toggling panorama enables panorama if transport is enabled`() {
        val reduced = transportEnabledState.reduce(Toggle(Overlay.PANORAMA, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.Panorama)
    }

    @Test
    fun `toggling traffic enables traffic`() {
        val reduced = noneEnabledState.reduce(Toggle(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `toggling traffic disables traffic if traffic is enabled`() {
        val reduced = trafficEnabledState.reduce(Toggle(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.None)
    }

    @Test
    fun `toggling traffic enables traffic if carparks are enabled`() {
        val reduced = carparksEnabledState.reduce(Toggle(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `toggling traffic enables traffic if panorama is enabled `() {
        val reduced = panoramaEnabledState.reduce(Toggle(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `toggling traffic enables traffic if transport is enabled`() {
        val reduced = transportEnabledState.reduce(Toggle(Overlay.TRAFFIC, savePermanent = true))
        expectThat(reduced.enabledOverlay).isA<EnabledOverlay.Traffic>()
    }

    @Test
    fun `toggling transport enables transport`() {
        val reduced = noneEnabledState.reduce(Toggle(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.transportMode()).isNotEqualTo(TransportMode.Hidden)
    }

    @Test
    fun `toggling transport disables transport if transport is enabled`() {
        val reduced = transportEnabledState.reduce(Toggle(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.enabledOverlay).isEqualTo(EnabledOverlay.None)
    }

    @Test
    fun `toggling transport enables transport if carparks are enabled`() {
        val reduced = carparksEnabledState.reduce(Toggle(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.transportMode()).isA<TransportMode.Vehicles>()
    }

    @Test
    fun `toggling transport enables transport if panorama is enabled`() {
        val reduced = panoramaEnabledState.reduce(Toggle(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.transportMode()).isNotEqualTo(TransportMode.Hidden)
    }

    @Test
    fun `toggling transport enables transport if traffic is enabled`() {
        val reduced = trafficEnabledState.reduce(Toggle(Overlay.TRANSPORT, savePermanent = true))
        expectThat(reduced.transportMode()).isNotEqualTo(TransportMode.Hidden)
    }
}
