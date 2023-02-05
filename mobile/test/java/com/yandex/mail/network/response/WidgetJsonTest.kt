package com.yandex.mail.network.response

import com.yandex.mail.runners.IntegrationTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(IntegrationTestRunner::class)
class WidgetJsonTest : GsonTest() {

    private val WIDGET_JSON_RESPONSE_STRING = "{\n" +
            "                \"info\": {\n" +
            "                    \"mid\": \"171418260816790782\",\n" +
            "                    \"type\": \"calendar\",\n" +
            "                    \"subtype\": \"past\",\n" +
            "                    \"valid\": false,\n" +
            "                    \"double\": false\n" +
            "                },\n" +
            "                \"controls\": [\n" +
            "                    {\n" +
            "                        \"role\": \"logo\",\n" +
            "                        \"type\": \"logo\",\n" +
            "                        \"attributes\": {\n" +
            "                            \"color\": \"transparent\",\n" +
            "                            \"has_html_entities\": false,\n" +
            "                            \"icon\": \"\",\n" +
            "                            \"label\": \"then milk\",\n" +
            "                            \"text_color\": \"#000000\",\n" +
            "                            \"zubchiki\": false\n" +
            "                        }\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"role\": \"title\",\n" +
            "                        \"type\": \"text\",\n" +
            "                        \"attributes\": {\n" +
            "                            \"has_html_entities\": false,\n" +
            "                            \"label\": \"Test event 2\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"role\": \"description-1\",\n" +
            "                        \"type\": \"text\",\n" +
            "                        \"attributes\": {\n" +
            "                            \"has_html_entities\": true,\n" +
            "                            \"label\": \"2 февраля, Воскресенье, с 18:00 по 18:30\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                ]\n" +
            "            }"

    @Test
    @Throws(IOException::class)
    fun fromJson() {
        val widget = gson.fromJson(WIDGET_JSON_RESPONSE_STRING, WidgetJson::class.java)
        assertThat(widget.info.mid).isEqualTo(171418260816790782L)
        assertThat(widget.info.type).isEqualTo("calendar")
        assertThat(widget.info.subtype).isEqualTo("past")
    }
}
