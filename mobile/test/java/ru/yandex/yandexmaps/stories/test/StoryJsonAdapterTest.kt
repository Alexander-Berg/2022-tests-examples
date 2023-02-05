package ru.yandex.yandexmaps.stories.test

import org.junit.Test
import ru.yandex.yandexmaps.common.utils.extensions.adapterForType
import ru.yandex.yandexmaps.stories.model.AssetType
import ru.yandex.yandexmaps.stories.model.PhotoAsset
import ru.yandex.yandexmaps.stories.model.Story
import ru.yandex.yandexmaps.stories.model.StoryScreen
import ru.yandex.yandexmaps.stories.model.VideoAsset
import ru.yandex.yandexmaps.stories.moshi.Serializer
import java.text.SimpleDateFormat
import java.util.*

internal class StoryJsonAdapterTest {

    @Test
    fun testStoriesParsing() {
        val videoScreen = StoryScreen.Video(
            "36190d7487f55jyogj13n",
            AssetType.VIDEO,
            emptyList(),
            emptyList(),
            listOf(
                VideoAsset(
                    1124.0,
                    2436.0,
                    "https://maps-stories-original.s3.yandex.net/v1/stories/story_collection_60/1x.mp4"
                ),
                VideoAsset(
                    1080.0,
                    1920.0,
                    "https://maps-stories-original.s3.yandex.net/v1/stories/story_collection_60/1.mp4"
                )
            )
        )
        val photoScreen = StoryScreen.Photo(
            "b40981a7469c8jyogjsu9",
            AssetType.PHOTO,
            emptyList(),
            emptyList(),
            listOf(
                PhotoAsset(
                    1124.0,
                    2436.0,
                    "https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016c3e05bb1e5c68e942e9878b60c3/%s"
                ),
                PhotoAsset(
                    1080.0,
                    1920.0,
                    "https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016c3e05bb1e5c68e942e9878b60c3/%s"
                )
            )
        )
        val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val startDate = iso8601Format.parse("2019-07-30T06:47:41.00Z")
        val endDate = iso8601Format.parse("2022-07-30T06:47:41.000Z")

        val expectedStory = Story(
            id = "82615425-2dc1-477d-8c8f-7bd4f43fa6bb",
            screens = listOf(
                videoScreen,
                photoScreen
            ),
            title = null,
            startDate = startDate,
            endDate = endDate
        )

        val result = Serializer.moshi.adapterForType<Story>().fromJson(testJson)

        assert(result == expectedStory)
    }
}

val testJson = """
    {
        "id": "82615425-2dc1-477d-8c8f-7bd4f43fa6bb",
        "startDate": "2019-07-30T06:47:41.000Z",
        "endDate": "2022-07-30T06:47:41.000Z",
        "screens": [{
            "id": "36190d7487f55jyogj13n",
            "type": "video",
            "buttons": [],
            "buttonsV2": [],
            "content": [{
                "url": "https://maps-stories-original.s3.yandex.net/v1/stories/story_collection_60/1x.mp4",
                "width": 1124,
                "height": 2436
            }, {
                "url": "https://maps-stories-original.s3.yandex.net/v1/stories/story_collection_60/1.mp4",
                "width": 1080,
                "height": 1920
            }]
        }, {
            "id": "b40981a7469c8jyogjsu9",
            "type": "photo",
            "buttons": [],
            "buttonsV2": [],
            "content": [{
                "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016c3e05bb1e5c68e942e9878b60c3/%s",
                "width": 1124,
                "height": 2436
            }, {
                "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016c3e05bb1e5c68e942e9878b60c3/%s",
                "width": 1080,
                "height": 1920
            }]
        }]
    }
""".trimIndent()
