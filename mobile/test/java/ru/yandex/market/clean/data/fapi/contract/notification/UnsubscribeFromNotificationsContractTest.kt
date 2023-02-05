package ru.yandex.market.clean.data.fapi.contract.notification

import com.google.gson.Gson
import junit.framework.TestCase
import ru.yandex.market.common.network.fapi.FapiVersions

class UnsubscribeFromNotificationsContractTest : TestCase() {

    private val contract = UnsubscribeFromNotificationsContract(
        gson = Gson(),
        uuid = UUID,
        appName = APP_NAME
    )

    fun testGetResolverName() {
        assertEquals(contract.resolverName, "unsubscribeFromNotifications")
    }

    fun testGetApiVersion() {
        assertEquals(contract.apiVersion, FapiVersions.V1)
    }

    fun testFormatParametersJson() {
        val data = mapOf(
            "uuid" to UUID,
            "appName" to APP_NAME
        )
        assertEquals(
            contract.formatParametersJson(),
            Gson().toJson(data)
        )
    }

    companion object {
        private const val UUID = "SomeRandomUUID"
        private const val APP_NAME = "MarketApp"
    }
}