/*
 * This file is a part of the Yandex Search for Android project.
 *
 * (C) Copyright 2019. Yandex, LLC. All rights reserved.
 *
 * Author: Alexander Skvortsov <askvortsov@yandex-team.ru>
 */

package ru.yandex.searchplugin.taxi.configuration.kit

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import okio.buffer
import okio.sink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream

@RunWith(JUnit4::class)
class StartupBodyConverterTest {

    @Test
    fun whenGcmTokenAndInstallIdAreNullConvertResultIsNull() {
        assertNull(StartupBodyConverter.convert(mock()))
    }

    @Test
    fun whenGcmTokenIsNullConvertResultIsNull() {
        assertNull(StartupBodyConverter.convert(mock {
            on { installId } doReturn "install_id"
            on { deviceId } doReturn "device_id"
        }))
    }

    @Test
    fun whenInstallIdIsNullConvertResultIsNull() {
        assertNull(StartupBodyConverter.convert(mock {
            on { gcmToken } doReturn "gcm_token_value"
            on { deviceId } doReturn "device_id"
        }))
    }

    @Test
    fun whenDeviceIdIsNullConvertResultIsNull() {
        assertNull(StartupBodyConverter.convert(mock {
            on { installId } doReturn "install_id"
            on { gcmToken } doReturn "gcm_token_value"
        }))
    }

    @Test
    fun whenInstallIdAndGcmTokenAndDeviceIdAreNotNullConvertsBodySuccessfully() {
        val installIdValue = "install_id"
        val gcmTokenValue = "gcm_token_value"
        val deviceIdValue = "device_id"
        val expectedData = """
        {
            "uuid": "$installIdValue",
            "metrica_device_id": "$deviceIdValue",
            "push_tokens": {
                "gcm_token": "$gcmTokenValue"
            }
        }
        """.trimIndent()
        val body = StartupBodyConverter.convert(mock {
            on { installId } doReturn installIdValue
            on { gcmToken } doReturn gcmTokenValue
            on { deviceId } doReturn deviceIdValue
        })
        val stream = ByteArrayOutputStream()
        val buffer = stream.sink().buffer()
        body?.writeTo(buffer)
        buffer.emit()
        assertEquals(expectedData, stream.toString())
    }
}
