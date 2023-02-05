package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.core.geometry.createPoint
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.FeedbackEvent
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class FeedbackEventParserTest : UriParserBaseTest() {

    @Test
    fun `Feedback to add organization is parsed correctly`() {
        val point = createPoint(61.401085, 55.115579)
        val uri = parseUri("yandexmaps://yandex.ru/maps/?feedback=add-organization&ll=${point.lon},${point.lat}")
        val event = FeedbackEvent.Parser.parse(uri)

        expectThat(event)
            .isA<FeedbackEvent.AddOrganization>()
            .get { this.point }
            .isEqualTo(point)
    }

    @Test
    fun `Feedback to edit organization is parsed correctly`() {
        val oid = "1018907821"
        val uri = parseUri("yandexmaps://yandex.ru/maps/?feedback=edit-organization&oid=$oid")
        val event = FeedbackEvent.Parser.parse(uri)

        expectThat(event)
            .isA<FeedbackEvent.EditOrganization>()
            .and {
                get { point }.isEqualTo(null)
                get { this.oid }.isEqualTo(oid)
            }
    }

    @Test
    fun `Feedback to add toponym is parsed correctly`() {
        val point = createPoint(61.401085, 55.115579)
        val uri = parseUri("yandexmaps://yandex.ru/maps/?feedback=add-toponym&ll=${point.lon},${point.lat}")
        val event = FeedbackEvent.Parser.parse(uri)

        expectThat(event)
            .isA<FeedbackEvent.AddToponym>()
            .get { this.point }
            .isEqualTo(point)
    }

    @Test
    fun `Legacy feedback to add organization is parsed correctly`() {
        val point = createPoint(61.401085, 55.115579)
        val uri = parseUri("https://yandex.ru/maps/mobile-feedback?type=business_add&geolocation=${point.lon},${point.lat}")
        val event = FeedbackEvent.Parser.parse(uri)

        expectThat(event)
            .isA<FeedbackEvent.AddOrganization>()
            .get { this.point }
            .isEqualTo(point)
    }

    @Test
    fun `Legacy feedback to edit organization is parsed correctly`() {
        val oid = "1018907821"
        val uri = parseUri("https://yandex.ru/maps/mobile-feedback?type=business_complaint&business_oid=$oid")
        val event = FeedbackEvent.Parser.parse(uri)

        expectThat(event)
            .isA<FeedbackEvent.EditOrganization>()
            .and {
                get { point }.isEqualTo(null)
                get { this.oid }.isEqualTo(oid)
            }
    }
}
