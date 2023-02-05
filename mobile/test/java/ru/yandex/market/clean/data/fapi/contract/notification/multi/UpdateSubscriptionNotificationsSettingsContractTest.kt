package ru.yandex.market.clean.data.fapi.contract.notification.multi

import com.google.gson.Gson
import junit.framework.TestCase
import ru.yandex.market.clean.data.model.dto.notifications.request.UpdateSubscriptionNotificationRequestDto
import ru.yandex.market.clean.data.model.dto.notifications.request.UpdateSubscriptionNotificationSettingsRequestParams
import ru.yandex.market.common.network.fapi.FapiVersions
import ru.yandex.market.json.jsonObject
import ru.yandex.market.json.toStringUsing

class UpdateSubscriptionNotificationsSettingsContractTest : TestCase() {

    private val contract = UpdateSubscriptionNotificationsSettingsContract(
        gson = Gson(),
        requestParams = UpdateSubscriptionNotificationSettingsRequestParams(
            uuid = UUID,
            notifications = listOf(
                UpdateSubscriptionNotificationRequestDto(
                    id = ID,
                    entity = ENTITY,
                    status = STATUS,
                    type = TYPE,
                    updatedTime = UPDATED_TIME
                )
            )
        )
    )

    fun testGetApiVersion() {
        assertEquals(FapiVersions.V1, contract.apiVersion)
    }

    fun testGetResolverName() {
        assertEquals("updatePushSubscriptions", contract.resolverName)
    }

    fun testFormatParametersJson() {
        val expected = jsonObject {
            "uuid" % UUID
            "subscriptions" % array {
                element(
                    UpdateSubscriptionNotificationRequestDto(
                        id = ID,
                        entity = ENTITY,
                        status = STATUS,
                        type = TYPE,
                        updatedTime = UPDATED_TIME
                    ).toJson()
                )
            }
        } toStringUsing Gson()
        assertEquals(
            expected,
            contract.parametersJsonString,
        )
    }

    private companion object {
        const val UUID = "SomeRandomUUID"
        const val ID = 1L
        const val ENTITY = "SomeRandomEntity"
        const val STATUS = "SomeRandomStatus"
        const val TYPE = "SomeRandomType"
        const val UPDATED_TIME = 0L
    }
}