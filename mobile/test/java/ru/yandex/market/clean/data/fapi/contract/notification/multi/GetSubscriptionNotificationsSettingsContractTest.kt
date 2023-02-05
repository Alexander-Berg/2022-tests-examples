package ru.yandex.market.clean.data.fapi.contract.notification.multi

import com.google.gson.Gson
import junit.framework.TestCase
import ru.yandex.market.common.network.fapi.FapiVersions

class GetSubscriptionNotificationsSettingsContractTest : TestCase() {

    private val contract = GetSubscriptionNotificationsSettingsContract(
        gson = Gson(),
        uuid = UUID
    )

    fun testGetApiVersion() {
        assertEquals(FapiVersions.V1, contract.apiVersion)
    }

    fun testGetResolverName() {
        assertEquals("resolvePushSubscriptions", contract.resolverName)
    }

    fun testFormatParametersJson() {
        val data = mapOf(
            "uuid" to UUID
        )
        assertEquals(
            Gson().toJson(data),
            contract.parametersJsonString
        )
    }

    private companion object {
        const val UUID = "SomeRandomUUID"
    }
}