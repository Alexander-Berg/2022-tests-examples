package ru.auto.feature.stories.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.feature.stories.model.StoryResponse
import ru.auto.test.runner.AllureRobolectricRunner
import kotlin.test.assertEquals

@RunWith(AllureRobolectricRunner::class)
class StoryResponseSerializerTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `should parse full stories and exclude unknown elements`() {
        val jsonString = """
            {
              "stories": [
                {
                  "id": "41001de0-c77a-4000-b6e3-9158d3827433",
                  "version": "1",
                  "pages": 1,
                  "image": "https://autoru-stories.s3.yandex.net/stories/41001de0-c77a-4000-b6e3-9158d3827433/default_1@",
                  "image_full": "https://autoru-stories.s3.yandex.net/stories/41001de0-c77a-4000-b6e3-9158d3827433/images/1_3@",
                  "image_preview": null,
                  "native_story": "https://autoru-stories.s3.yandex.net/stories/41001de0-c77a-4000-b6e3-9158d3827433/story.xml",
                  "background": "#000000",
                  "text": "#ffffff",
                  "title": "Лучшие цены на новые автомобили",
                  "geo": [
                    1
                  ]
                }
              ],
              "stories_base_path": "https://autoru-stories.s3.yandex.net/stories"
            }
        """.trimIndent()
        val stories = json.decodeFromString<StoryResponse>(jsonString).stories
        assertEquals(1, stories.size)
    }


    @Test
    fun `should parse empty if broken`() {
        val jsonString = """
            {
              "stories": [
                {
                  "broken": "41001de0-c77a-4000-b6e3-9158d3827433"
                }
              ]
            }
        """.trimIndent()
        val stories = json.decodeFromString<StoryResponse>(jsonString).stories
        assertEquals(0, stories.size)
    }

    @Test
    fun `should exclude broken stories`() {
        val jsonString = """
            {
              "stories": [
                {
                  "id": "41001de0-c77a-4000-b6e3-9158d3827433",
                  "version": "1",
                  "pages": 1,
                  "image": "https://autoru-stories.s3.yandex.net/stories/41001de0-c77a-4000-b6e3-9158d3827433/default_1@",
                  "image_full": "https://autoru-stories.s3.yandex.net/stories/41001de0-c77a-4000-b6e3-9158d3827433/images/1_3@",
                  "image_preview": null,
                  "native_story": "https://autoru-stories.s3.yandex.net/stories/41001de0-c77a-4000-b6e3-9158d3827433/story.xml",
                  "background": "#000000",
                  "text": "#ffffff",
                  "title": "Лучшие цены на новые автомобили",
                  "geo": [
                    1
                  ]
                },
                {
                  "broken": "41001de0-c77a-4000-b6e3-9158d3827433"
                }
              ]
            }
        """.trimIndent()
        val stories = json.decodeFromString<StoryResponse>(jsonString).stories
        assertEquals(1, stories.size)
    }

    @Test
    fun `should parse empty if there is not stories element`() {
        val jsonString = "{}"
        val stories = json.decodeFromString<StoryResponse>(jsonString).stories
        assertEquals(0, stories.size)
    }

    @Test
    fun `should parse minimal story`() {
        val jsonString = """
            {
              "stories": [
                {
                  "id": "41001de0-c77a-4000-b6e3-9158d3827433",
                  "version": "1",
                  "pages": 1
                }
              ]
            }
        """.trimIndent()
        val stories = json.decodeFromString<StoryResponse>(jsonString).stories
        assertEquals(1, stories.size)
    }
}
