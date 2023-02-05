package ru.yandex.yandexmaps.launch.events

import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.OrganizationEvent
import kotlin.test.assertEquals

class OpenOrganizationEventParserTest : UriParserBaseTest() {

    private val organizationId = "1001625855"
    private val anotherOrganizationId = "1001625855"

    @Test
    fun uriWithParam_parsedCorrect() {
        val uri = parseUri("http://maps.yandex.ru/?oid=$organizationId")
        val event = OrganizationEvent.Parser.parse(uri)
        assert(event is OrganizationEvent.Open)

        val openOrganizationEvent = event as OrganizationEvent
        assert(openOrganizationEvent.organizationId == organizationId)
    }

    @Test
    fun uriWithAddReview_parsedCorrect() {
        val uri = parseUri("http://maps.yandex.ru/?oid=$organizationId&add-review=1")
        val event = OrganizationEvent.Parser.parse(uri)
        assert(event is OrganizationEvent.ComposeReview)

        val openOrganizationEvent = event as OrganizationEvent
        assert(openOrganizationEvent.organizationId == organizationId)
    }

    @Test
    fun uriWithGallery_parsedCorrect() {
        val uri = parseUri("http://maps.yandex.ru/?oid=$anotherOrganizationId&photos[business]=$organizationId")
        val event = OrganizationEvent.Parser.parse(uri)
        assert(event is OrganizationEvent.Gallery)

        val openOrganizationEvent = event as OrganizationEvent
        assert(openOrganizationEvent.organizationId == organizationId)
    }

    @Test
    fun uriWithStory_parsedCorrect() {
        val storyId = "12345"
        val uri = parseUri("http://maps.yandex.ru/?oid=$anotherOrganizationId&stories[business]=$organizationId&stories[id]=$storyId")
        val event = OrganizationEvent.Parser.parse(uri)
        assertEquals(OrganizationEvent.Story(organizationId = organizationId, storyId = storyId), event)
    }

    @Test
    fun uriWithKnownTabId_parsedCorrect() {
        val uri = parseUri("http://maps.yandex.ru/?oid=$organizationId&tab=${OrganizationEvent.Tab.Biglion.id}")
        val event = OrganizationEvent.Parser.parse(uri)
        assert(event is OrganizationEvent.Open)

        val openOrganizationEvent = event as OrganizationEvent
        assert(openOrganizationEvent.organizationId == organizationId)
    }

    @Test
    fun uriWithUnknownTabId_parsedCorrect() {
        val unknownTabId = OrganizationEvent.Tab.values().joinToString { it.id }
        val uri = parseUri("http://maps.yandex.ru/?oid=$organizationId&tab=$unknownTabId")
        val event = OrganizationEvent.Parser.parse(uri)
        assert(event is OrganizationEvent.Open)

        val openOrganizationEvent = event as OrganizationEvent
        assert(openOrganizationEvent.organizationId == organizationId)
    }
}
