package ru.yandex.yandexmaps.launch.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.yandexmaps.launch.UriParserBaseTest
import ru.yandex.yandexmaps.multiplatform.uri.parser.api.events.AddExperimentsEvent

class AddExperimentsEventParserTest : UriParserBaseTest() {

    @Test
    fun serviceIdAndParams_parsedCorrect() {

        val serviceId = "COOL_SERVICE"
        val uri = parseUri("yandexmaps://add_exp/$serviceId?cool_param=1111&another-param=another-value&just_param")
        val event = AddExperimentsEvent.Parser.parse(uri) as AddExperimentsEvent

        assertThat(event.serviceId).isEqualTo(serviceId)

        assertThat(event.parameters).isEqualTo(
            mapOf(
                "cool_param" to "1111",
                "another-param" to "another-value",
                "just_param" to ""
            )
        )
    }

    @Test
    fun noServiceIdAndParams_parsedCorrect() {

        val uri = parseUri("yandexmaps://add_exp?cool_param=1111&another-param=another-value&just_param")
        val event = AddExperimentsEvent.Parser.parse(uri) as AddExperimentsEvent

        assertThat(event.serviceId).isNull()

        assertThat(event.parameters).isEqualTo(
            mapOf(
                "cool_param" to "1111",
                "another-param" to "another-value",
                "just_param" to ""
            )
        )
    }

    @Test
    fun noParams_parsedCorrect() {
        val uri = parseUri("yandexmaps://add_exp")
        val event = AddExperimentsEvent.Parser.parse(uri) as AddExperimentsEvent

        assertThat(event.serviceId).isNull()
        assertThat(event.parameters).isEmpty()
    }
}
