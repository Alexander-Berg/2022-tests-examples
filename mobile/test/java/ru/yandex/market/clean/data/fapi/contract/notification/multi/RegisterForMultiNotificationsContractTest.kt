package ru.yandex.market.clean.data.fapi.contract.notification.multi

import com.google.gson.Gson
import junit.framework.TestCase
import ru.yandex.market.clean.data.model.dto.notifications.request.RegisterForMultiNotificationsRequestParamsDto
import ru.yandex.market.common.network.fapi.FapiVersions

class RegisterForMultiNotificationsContractTest : TestCase() {

    private val contract = RegisterForMultiNotificationsContract(
        gson = Gson(),
        params = RegisterForMultiNotificationsRequestParamsDto(
            uuid = UUID,
            pushToken = PUSH_TOKEN,
            appName = APP_NAME,
            platform = PLATFORM,
            loginTime = LOGIN_TIME,
            enabledBySystem = ENABLED_BY_SYSTEM,
            appVersion = APP_VERSION,
            osVersion = OS_VERSION
        )
    )

    fun testGetApiVersion() {
        assertEquals(FapiVersions.V1, contract.apiVersion)
    }

    fun testGetResolverName() {
        assertEquals("registerForPushNotifications", contract.resolverName)
    }

    fun testFormatParametersJson() {
        val data = mapOf(
            "uuid" to UUID,
            "pushToken" to PUSH_TOKEN,
            "appName" to APP_NAME,
            "platform" to PLATFORM,
            "loginTime" to LOGIN_TIME,
            "enabledBySystem" to ENABLED_BY_SYSTEM,
            "appVersion" to APP_VERSION.toString(),
            "osVersion" to OS_VERSION
        )
        assertEquals(
            Gson().toJson(data),
            contract.parametersJsonString
        )
    }

    private companion object {
        const val UUID = "SomeRandomUUID"
        const val PUSH_TOKEN = "SomeRandomPushToken"
        const val APP_NAME = "MarketApp"
        const val PLATFORM = "ANDROID"
        const val LOGIN_TIME = 0L
        const val ENABLED_BY_SYSTEM = true
        const val APP_VERSION = 10
        const val OS_VERSION = "SomeRandomOsVersion"
    }
}