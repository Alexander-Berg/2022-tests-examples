package ru.auto.data.model.network.nodejs.xiva

import com.google.gson.Gson
import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.data.model.network.xiva.NWSocketChatMessage

/**
 *
 * @author jagger on 04.08.17.
 */
@RunWith(AllureRunner::class) class XivaMessageConvertionTest {

    @Test
    fun `chat message convertion is correct`() {
        val socketMessage = "{\"chat\":{\"message_sent\":{\"message\":{\"id\":\"11e77b3d5f7516a08296590c06b04d1f\",\"room_id\":\"dafc0ef704583d07df28810f3d397666\",\"author\":\"f89247392cb7d35f\",\"created\":\"2017-08-07T06:55:18.282Z\",\"payload\":{\"content_type\":\"TEXT_PLAIN\",\"value\":\"ппп\"},\"provided_id\":\"0\"},\"previous_message_id\":\"11e7793bfd64c3008296590c06b04d1f\"}}}"
        val gson = Gson()
        val message = gson.fromJson(socketMessage, NWSocketChatMessage::class.java)
        checkNotNull(message?.chat?.message_sent?.message)
    }
}
