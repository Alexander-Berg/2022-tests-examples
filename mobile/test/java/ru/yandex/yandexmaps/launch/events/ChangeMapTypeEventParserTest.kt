package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.MapChangingParams
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.ChangeMapTypeEvent

class ChangeMapTypeEventParserTest : UriParserBaseTest() {

    @Test
    fun mapVector_parsedCorrect() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/?l=map")
        val event = ChangeMapTypeEvent.Parser.parse(uri)

        assert(event is ChangeMapTypeEvent)

        val changeMapTypeEvent = event as ChangeMapTypeEvent
        assert(changeMapTypeEvent.mapChangingParams.mapAppearance!! == MapChangingParams.MapAppearance.VECTOR_MAP)
    }

    @Test
    fun trafficLayer_parsedCorrect() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/?l=trf")
        val event = ChangeMapTypeEvent.Parser.parse(uri)

        assert(event is ChangeMapTypeEvent)

        val changeMapTypeEvent = event as ChangeMapTypeEvent
        assert(changeMapTypeEvent.mapChangingParams.layersConfig.traffic ?: false)
    }

    @Test
    fun satelliteWithCarparks_parsedCorrect() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/?l=sat,carparks")
        val event = ChangeMapTypeEvent.Parser.parse(uri)

        assert(event is ChangeMapTypeEvent)

        val changeMapTypeEvent = event as ChangeMapTypeEvent
        assert(changeMapTypeEvent.mapChangingParams.mapAppearance!! == MapChangingParams.MapAppearance.SATELLITE)
        assert(changeMapTypeEvent.mapChangingParams.layersConfig.carparks ?: false)
    }

    @Test
    fun scooters_parsedCorrect() {
        val uri = parseUri("yandexmaps://maps.yandex.ru/?l=kicksharing")
        val event = ChangeMapTypeEvent.Parser.parse(uri)

        assert(event is ChangeMapTypeEvent)

        val changeMapTypeEvent = event as ChangeMapTypeEvent
        assert(changeMapTypeEvent.mapChangingParams.layersConfig.scooters ?: false)
    }
}
