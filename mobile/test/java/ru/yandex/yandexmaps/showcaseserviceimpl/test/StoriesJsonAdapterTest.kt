package ru.yandex.yandexmaps.showcaseserviceimpl.test

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types.newParameterizedType
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.Image
import ru.yandex.maps.showcase.showcaseserviceapi.showcase.models.StoryCard
import ru.yandex.yandexmaps.common.utils.extensions.adapterForType
import ru.yandex.yandexmaps.stories.model.AssetType
import ru.yandex.yandexmaps.stories.model.PhotoAsset
import ru.yandex.yandexmaps.stories.model.STORY_ASSET_TYPE
import ru.yandex.yandexmaps.stories.model.STORY_ASSET_TYPE_PHOTO
import ru.yandex.yandexmaps.stories.model.STORY_ASSET_TYPE_VIDEO
import ru.yandex.yandexmaps.stories.model.STORY_BUTTON_TYPE
import ru.yandex.yandexmaps.stories.model.STORY_BUTTON_TYPE_ADD_BOOKMARK
import ru.yandex.yandexmaps.stories.model.STORY_BUTTON_TYPE_ADD_TO_CALENDAR
import ru.yandex.yandexmaps.stories.model.STORY_BUTTON_TYPE_OPEN_URL
import ru.yandex.yandexmaps.stories.model.StoryScreen
import ru.yandex.yandexmaps.stories.model.StoryScreenButton
import ru.yandex.yandexmaps.stories.model.VideoAsset
import java.text.SimpleDateFormat
import java.util.*

internal class StoriesJsonAdapterTest {

    @Test
    fun testStoriesParsing() {
        val videoScreen = StoryScreen.Video(
            "36190d7487f55jyogj13n",
            AssetType.VIDEO,
            emptyList(),
            listOf(
                StoryScreenButton.OpenUrl(
                    tags = emptyList(),
                    title = "Подробнее",
                    icon = null,
                    backgroundColor = "4CA6FF",
                    titleColor = "FFFFFF",
                    url = "yandexmaps://open_url?url=https%3A%2F%2Fwww.smartmsk.com%2Fsciencedelivery"
                )
            ),
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
        val expectedStoryCard = StoryCard(
            "82615425-2dc1-477d-8c8f-7bd4f43fa6bb",
            "Где попробовать мясо будущего",
            Image("https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016c3e05bb1e5c68e942e9878b60c3/%s"),
            startDate,
            endDate,
            listOf(
                videoScreen,
                photoScreen
            )
        )

        val listPhotoAsset = newParameterizedType(List::class.java, PhotoAsset::class.java)
        val listVideoAsset = newParameterizedType(List::class.java, VideoAsset::class.java)
        val photoListAdapter = Moshi.Builder().build().adapter<List<VideoAsset>>(newParameterizedType(List::class.java, PhotoAsset::class.java))
        val videoListAdapter = Moshi.Builder().build().adapter<List<VideoAsset>>(newParameterizedType(List::class.java, VideoAsset::class.java))

        val result = Moshi.Builder()
            .add(
                PolymorphicJsonAdapterFactory.of(StoryScreen::class.java, STORY_ASSET_TYPE)
                    .withSubtype(StoryScreen.Photo::class.java, STORY_ASSET_TYPE_PHOTO)
                    .withSubtype(StoryScreen.Video::class.java, STORY_ASSET_TYPE_VIDEO)
            )
            .add(
                PolymorphicJsonAdapterFactory.of(StoryScreenButton::class.java, STORY_BUTTON_TYPE)
                    .withSubtype(StoryScreenButton.OpenUrl::class.java, STORY_BUTTON_TYPE_OPEN_URL)
                    .withSubtype(StoryScreenButton.AddBookmark::class.java, STORY_BUTTON_TYPE_ADD_BOOKMARK)
                    .withSubtype(StoryScreenButton.AddToCalendar::class.java, STORY_BUTTON_TYPE_ADD_TO_CALENDAR)
            )
            .add(listVideoAsset, videoListAdapter)
            .add(listPhotoAsset, photoListAdapter)
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .build()
            .adapterForType<StoryCard>()
            .fromJson(testJson)

        Assertions.assertThat(result).isEqualTo(expectedStoryCard)
    }
}

val testJson = """
    {
        "id": "82615425-2dc1-477d-8c8f-7bd4f43fa6bb",
        "title": "Где попробовать мясо будущего",
        "previewImage": {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016c3e05bb1e5c68e942e9878b60c3/%s"
        },
        "startDate": "2019-07-30T06:47:41.000Z",
        "endDate": "2022-07-30T06:47:41.000Z",
        "screens": [{
            "id": "36190d7487f55jyogj13n",
            "type": "video",
            "buttons": [],
            "buttonsV2": [
                {
                  "url": "yandexmaps://open_url?url=https%3A%2F%2Fwww.smartmsk.com%2Fsciencedelivery",
                  "tags": [],
                  "type": "openURL",
                  "title": "Подробнее",
                  "titleColor": "FFFFFF",
                  "backgroundColor": "4CA6FF"
                }
              ],
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
