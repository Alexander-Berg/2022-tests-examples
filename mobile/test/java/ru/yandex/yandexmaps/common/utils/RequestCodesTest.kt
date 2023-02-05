package ru.yandex.yandexmaps.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.yandex.yandexmaps.multiplatform.analytics.GeneratedAppAnalytics

class RequestCodesTest {

    @Test
    fun shouldGenerateValidIncrementalCodes() {
        val first = RequestCodes.generateIncremental()
        val second = RequestCodes.generateIncremental()
        val third = RequestCodes.generateIncremental()

        assertEquals(first + 1, second)
        assertEquals(second + 1, third)
        assertTrue(RequestCodes.isCommon(first))
        assertTrue(RequestCodes.isCommon(second))
        assertTrue(RequestCodes.isCommon(third))
    }

    @Test
    fun shouldGenerateValidAuthCodes() {

        val defaultCode = RequestCodes.generateAuth(null)
        assertEquals(defaultCode, 0x00001000)
        assertTrue(RequestCodes.isAuth(defaultCode))

        val thirdLoginSuccessCode = RequestCodes.generateAuth(GeneratedAppAnalytics.LoginSuccessReason.values()[2])
        assertEquals(thirdLoginSuccessCode, 0x00001003)
        assertTrue(RequestCodes.isAuth(defaultCode))
    }

    @Test
    fun testGroupApplying() {
        val code = RequestCodes.applyGroup(0x70a69189, 0xe000)
        assertEquals(0x0000e189, code)
    }

    @Test
    fun testDecodeInitialCodeFromGroup() {
        val code = RequestCodes.Settings.generateRequestCode(1)
        assertEquals(1, RequestCodes.decodeRequestCodeFromGroup(code))
    }

    @Test
    fun testEnableNotificationsAndEnableChannel() {
        val code = RequestCodes.Settings.generateRequestCode(1)
        assertEquals(true, RequestCodes.Settings.isEnableNotifications(code))
        assertEquals(false, RequestCodes.Settings.isEnableChannel(code))
    }
}
