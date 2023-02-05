package ru.yandex.market.clean.data.model.dto.realtimesignals

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.beru.android.BuildConfig
import ru.yandex.market.clean.domain.model.RealtimeUserSignalEventType
import ru.yandex.market.json.toStringUsing

class StoriesRealtimeSignalTest {

    private val gson = Gson()

    private val storySignal = StoriesRealtimeSignal(
        eventName = RealtimeUserSignalEventType.STORY_SCREEN_VISIBLE.value,
        userInfo = RealtimeSignalUserInfo(PUID, UUID, YAUID, GAID),
        storyId = STORY_ID,
    )

    @Test
    fun `Should return correctly formatted string`() {
        val actual = storySignal.toJson(gson) toStringUsing gson
        val expected = buildString {
            append("{")
            append("\"eventType\":\"StoriesVisible\",")
            append("\"value\":")
            append("\"{")
            append("\\\"user\\\":{\\\"uuid\\\":\\\"UUID\\\",\\\"yandexuid\\\":\\\"YAUID\\\",\\\"gaid\\\":\\\"GAID\\\"")
            append("},")
            append("\\\"platform\\\":{\\\"type\\\":\\\"app\\\",\\\"os\\\":\\\"android\\\",")
            append("\\\"app_version\\\":\\\"${BuildConfig.SHORT_VERSION_NAME}\\\"},")
            append("\\\"storiesId\\\":\\\"$STORY_ID\\\"")
            append("}\"")
            append("}")
        }

        assertThat(actual).isEqualTo(expected)
    }

    private companion object {
        val PUID = null
        const val UUID = "UUID"
        const val YAUID = "YAUID"
        const val GAID = "GAID"
        const val STORY_ID = "STORY_ID"
    }
}
