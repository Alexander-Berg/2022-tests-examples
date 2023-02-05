package ru.yandex.yandexmaps.overlays.internal.redux

import ru.yandex.yandexmaps.overlays.api.EnabledOverlay
import ru.yandex.yandexmaps.overlays.api.OverlaysState
import ru.yandex.yandexmaps.overlays.api.TransportFilters
import ru.yandex.yandexmaps.overlays.api.TransportLinesFilter
import ru.yandex.yandexmaps.overlays.api.TransportMode
import ru.yandex.yandexmaps.overlays.api.TransportOverlay
import ru.yandex.yandexmaps.overlays.api.TransportTypesFilter

object DefaultStates {

    val noneEnabledState: OverlaysState = withEnabled(enabled = EnabledOverlay.None)
    val carparksEnabledState: OverlaysState = withEnabled(enabled = EnabledOverlay.Carparks())
    val panoramaEnabledState: OverlaysState = withEnabled(enabled = EnabledOverlay.Panorama)
    val trafficEnabledState: OverlaysState = withEnabled(enabled = EnabledOverlay.Traffic.Loading)
    val transportEnabledState: OverlaysState = withEnabled(TransportMode.Vehicles())

    fun withEnabled(enabled: EnabledOverlay): OverlaysState {
        return OverlaysState(
            enabledOverlay = enabled,
            transportOverlays = listOf(
                TransportOverlay(
                    filters = TransportFilters(
                        typesFilter = TransportTypesFilter.All,
                        linesFilter = TransportLinesFilter.EMPTY
                    ),
                    mode = TransportMode.Hidden
                )
            ),
            roadEventsVisible = true
        )
    }

    fun withEnabled(transportMode: TransportMode): OverlaysState {
        return OverlaysState(
            enabledOverlay = EnabledOverlay.None,
            transportOverlays = listOf(
                TransportOverlay(
                    filters = TransportFilters(
                        typesFilter = TransportTypesFilter.All,
                        linesFilter = TransportLinesFilter.EMPTY
                    ),
                    mode = transportMode,
                )
            ),
            roadEventsVisible = true
        )
    }
}
