package ru.yandex.market.clean.data.fapi.contract.notification

import com.google.gson.Gson
import junit.framework.TestCase
import ru.yandex.market.common.network.fapi.FapiVersions

class SubscribeToNotificationsContractTest : TestCase() {

    private val contract = SubscribeToNotificationsContract(
        gson = Gson(),
        pushToken = PUSH_TOKEN,
        uuid = UUID,
        appName = APP_NAME,
    )

    fun testGetResolverName() {
        assertEquals(contract.resolverName, "subscribeToNotifications")
    }

    fun testGetApiVersion() {
        assertEquals(contract.apiVersion, FapiVersions.V1)
    }

    fun testFormatParametersJson() {
        val data = mapOf(
            "uuid" to UUID,
            "pushToken" to PUSH_TOKEN,
            "appName" to APP_NAME,
        )
        assertEquals(
            contract.formatParametersJson(),
            Gson().toJson(data)
        )
    }

    private companion object {
        const val PUSH_TOKEN = "RandomPushToken"
        const val UUID = "SomeRandomUUID"
        const val APP_NAME = "MarketApp"
    }
}