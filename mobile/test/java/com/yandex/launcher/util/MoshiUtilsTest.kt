package com.yandex.launcher.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.common.util.StringsMapJsonAdapter
import com.yandex.launcher.data.MarketAppInfo
import com.yandex.launcher.referrer.ReferrerInfo
import com.yandex.launcher.seamlesssearch.CookieHolderJar
import com.yandex.launcher.wallpapers.PreviousWallpaperInfo
import com.yandex.launcher.wallpapers.WallpaperMetadata
import com.yandex.launcher.wallpapers.WallpapersFeedInfo
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Arrays

class MoshiUtilsTest: BaseRobolectricTest() {

    @Test
    fun `should parse map via fromJson`() {
        val testMapString = """{
            "int": 10,
            "float": 10.1,
            "string": "string",
            "boolean": "true"
        }""".trimIndent()
        val inputStream = testMapString.byteInputStream(StandardCharsets.UTF_8)
        @Suppress("UNCHECKED_CAST")
        val map = MoshiUtils.fromJson(inputStream, StringsMapJsonAdapter())
                as Map<String?, String?>

        assertThat(map["int"], equalTo("10"))
        assertThat(map["float"], equalTo("10.1"))
        assertThat(map["string"], equalTo("string"))
        assertThat(map["boolean"], equalTo("true"))
    }

    @Test
    fun `should parse CookieHolderJar and convert back as same value`() {
        val cookieHolderJar = CookieHolderJar(1001L,
            mutableListOf(CookieHolderJar.Entry("http://test.url", listOf("test", "test1", "test2")),
                CookieHolderJar.Entry("http://test1.url", listOf("test", "test1", "test2")),
            CookieHolderJar.Entry("http://test2.url", listOf("test", "test1", "test2"))))
        val json = MoshiUtils.toJson(cookieHolderJar, CookieHolderJar::class.java)
        val convertedCookieHolderJar = MoshiUtils.fromJson(json, CookieHolderJar::class.java)

        assertThat(cookieHolderJar, equalTo(convertedCookieHolderJar))
    }

    @Test
    fun `should parse WallpaperFeedInfo and convert back as same value`() {
        val wallpapersFeedInfo = WallpapersFeedInfo("test_id", 1000L)
        val json = MoshiUtils.toJson(wallpapersFeedInfo, WallpapersFeedInfo::class.java)
        val convertedWallpapersFeedInfo = MoshiUtils.fromJson(json, WallpapersFeedInfo::class.java)

        assertThat(wallpapersFeedInfo, equalTo(convertedWallpapersFeedInfo))
    }

    @Test
    fun `should parse PreviousWallpaperInfo and convert back as same value`() {
        val previousWallpaperInfo = PreviousWallpaperInfo()
        previousWallpaperInfo.averageColors = intArrayOf(10, 20, 30)
        previousWallpaperInfo.imageFileName = "imagefilename"

        val json = MoshiUtils.toJson(previousWallpaperInfo, PreviousWallpaperInfo::class.java)
        val convertedPreviousWallpaperInfo = MoshiUtils.fromJson(json,
            PreviousWallpaperInfo::class.java)

        assertThat(previousWallpaperInfo, equalTo(convertedPreviousWallpaperInfo))
    }

    @Test
    fun `should parse MarketAppInfo array via fromJsonArray`() {
        val testArrayString = """[
            {
                "package_name": "com.yandex.package1",
                "title": "title1",
                "description": "description1",
                "rating": 1,
                "rating_count": 1,
                "icon": "icon.url.1",
                "impression_id": "impression_1",
                "adnetwork": { "name": "ad_network1",
                               "offer_id": "offer_id1",
                               "click_url": "url.1",
                               "on_show_callback_url": "callback_url.1"
                              }
            },
            {
                "package_name": "com.yandex.package2",
                "title": "title2",
                "description": "description2",
                "rating": 2,
                "rating_count": 2,
                "icon": "icon.url.2",
                "impression_id": "impression_2",
                "adnetwork": { "name": "ad_network2",
                               "offer_id": "offer_id2",
                               "click_url": "url.2",
                               "on_show_callback_url": "callback_url.2"
                              }
            },
            {
                "package_name": "com.yandex.package3",
                "title": "title3",
                "description": "description3",
                "rating": 3,
                "rating_count": 3,
                "icon": "icon.url.3",
                "impression_id": "impression_3",
                "adnetwork": { "name": "ad_network3",
                               "offer_id": "offer_id3",
                               "click_url": "url.3",
                               "on_show_callback_url": "callback_url.3"
                              }
            }
        ]""".trimIndent()
        val inputStream = testArrayString.byteInputStream(StandardCharsets.UTF_8)
        val infoArray = MoshiUtils.fromJsonArray(inputStream, MarketAppInfo::class.java)

        for (info in infoArray) {
            val index = info.packageName[info.packageName.length - 1]

            assertThat(info.packageName, equalTo("com.yandex.package$index"))
            assertThat(info.title, equalTo("title$index"))
            assertThat(info.description, equalTo("description$index"))
            assertThat(info.description, equalTo("description$index"))
            assertThat(info.rating, equalTo(Character.getNumericValue(index).toFloat()))
            assertThat(info.iconUrl, equalTo("icon.url.$index"))
            assertThat(info.adNetwork.offerID, equalTo("offer_id$index"))
        }
    }

    @Test
    fun `should parse WallpaperMetadata and convert back as same value`() {
        val wallpaperMetadata = WallpaperMetadata("wallpaperid",
            "url",
            "collectionid",
            "title",
            "path")
        val json = MoshiUtils.toJson(wallpaperMetadata, WallpaperMetadata::class.java)
        val convertedWallpaperMetadata = MoshiUtils.fromJson(json, wallpaperMetadata::class.java)

        assertThat(wallpaperMetadata, equalTo(convertedWallpaperMetadata))
    }

    @Test
    fun `should parse String array and convert back as same value`() {
        val arr = arrayOf("1", "2", "3")
        val json = MoshiUtils.toJson(arr, Array<String>::class.java)
        val convertedArr = MoshiUtils.fromJson(json, Array<String>::class.java)

        assertThat(Arrays.equals(arr, convertedArr), equalTo(true))
    }

    @Test
    fun `should parse ReferrerInfo and convert back as same value`() {
        val referrerInfo = ReferrerInfo(mapOf(Pair("key1", "val1"), Pair("key2", "val2"), Pair("key3", "val3")))
        val json = MoshiUtils.toJson(referrerInfo, ReferrerInfo::class.java)
        val convertedReferrerInfo = MoshiUtils.fromJson(json, ReferrerInfo::class.java)

        assertThat(referrerInfo, equalTo(convertedReferrerInfo))
    }
}
