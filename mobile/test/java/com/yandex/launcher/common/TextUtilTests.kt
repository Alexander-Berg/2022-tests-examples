package com.yandex.launcher.common

import android.app.Application
import android.os.Build
import com.yandex.launcher.common.util.TextUtils
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], application = Application::class)
class TextUtilTests {

    lateinit var jsonObject: JSONObject

    @Before
    fun setup() {
        jsonObject = JSONObject(
            "{\"ALLAPPS_BUTTON\":{\"all_apps_button_shape\":\"PILLOW\",\"all_apps_button_use_preset\":false,\"all_apps_button_preset_index\":0}," +
                    "\"EFFECTS\":{\"effect_list_preference\":\"SOFT_ZOOM\"}," +
                    "\"GRID\":{\"rows_list_preference\":5,\"columns_list_preference\":5,\"show_workspace_icon_text\":true,\"workspace_icon_size_modifier\":0}," +
                    "\"HOME_SCREENS\":{\"cyclic_preference\":false,\"defaultHomeScreenId\":-1,\"zen_enabled\":true}," +
                    "\"HOMEWIDGET\":{\"homewidget_parts\":\"[CLOCK, WEATHER, DATE, ALARM_CLOCK]\",\"homewidget_scale\":true,\"homewidget_till_alarm\":false}," +
                    "\"ICON\":{\"icon_preference\":\"EXTERNAL\",\"icon_package_preference\":\"com.yandex.launcher.externaltheme.wot\"}," +
                    "\"THEMES\":{\"activeTheme\":\"com.yandex.launcher.externaltheme.wot\",\"adaptive_color_scheme\":\"-16777216;-1;-1\",\"accentColor\":" +
                    "\"RED\",\"accentBaseColor\":\"BLUE\"}," +
                    "\"NOTIFICATION\":{\"badge_preference\":true}," +
                    "\"SEARCH\":{\"search_engine_preference\":\"YANDEX\",\"search_widget_preference\":true,\"settings_show_alice_icon\":true}," +
                    "\"ALICE\":{\"voice_activation\":true}}"
        )
    }

    @Test
    fun compressAndDecompressByteArray() {
        val stringToCompress = "test    String..////"
        val uncompressedByteArray = stringToCompress.toByteArray()

        val compressedBytes = TextUtils.compressByteArray(uncompressedByteArray)
        val decompressedBytes = TextUtils.decompressByteArray(compressedBytes)

        Assert.assertArrayEquals(uncompressedByteArray, decompressedBytes)
    }

    @Test
    fun compressAndDecompressString() {
        val stringToCompress = "test  312@DA*jfvxAA((xz§ll  String..////"

        val compressedBytes = TextUtils.compressByteArray(stringToCompress.toByteArray())
        val decompressedBytes = TextUtils.decompressByteArray(compressedBytes)

        Assert.assertEquals(stringToCompress, String(decompressedBytes))
    }

    @Test
    fun compressAndDecompressBase64String() {
        val testString = "test  312@DA*jfvxAA((xz§ll  String..////"

        val compressedBase64String = TextUtils.compressStringToBase64(testString)
        val decompressedString = TextUtils.decompressBase64ToString(compressedBase64String)

        Assert.assertEquals(decompressedString, testString)
    }

    @Test
    fun `compress and decompress JSONObject through byte array`() {
        val compressionStart = System.nanoTime()
        val compressedBytes = TextUtils.compressByteArray(jsonObject.toString().toByteArray())
        println("bytes compression time: ${System.nanoTime() - compressionStart} ns")

        val decompressionStart = System.nanoTime()
        val decompressedBytes = TextUtils.decompressByteArray(compressedBytes)
        println("bytes decompression time: ${System.nanoTime() - decompressionStart} ns")

        println("bytes compressed size: ${compressedBytes.size}")
        println("bytes decompressed size: ${decompressedBytes.size}")

        Assert.assertEquals(jsonObject.toString(), String(decompressedBytes))
    }

    @Test
    fun `compress and decompress JSONObject through base64 string`() {
        val compressionStart = System.nanoTime()
        val compressedString = TextUtils.compressStringToBase64(jsonObject.toString())
        println("base64 compression time: ${System.nanoTime() - compressionStart} ns")

        val decompressionStart = System.nanoTime()
        val decompressedString = TextUtils.decompressBase64ToString(compressedString)
        println("base 64 decompression time: ${System.nanoTime() - decompressionStart} ns")

        println("base 64 compressed size: ${compressedString.length}")
        println("base 64 decompressed size: ${decompressedString.length}")

        Assert.assertEquals(jsonObject.toString(), decompressedString)
    }
}
