package com.yandex.mobile.realty.test.yandexrent

import com.google.gson.JsonObject
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.utils.jsonObject
import okhttp3.mockwebserver.MockResponse

/**
 * @author misha-kozlov on 24.10.2021
 */
open class RentUtilitiesTest : BaseTest() {

    protected fun DispatcherRegistry.registerPeriod(
        receipts: JsonObject? = null,
        paymentConfirmation: JsonObject? = null
    ) {
        register(
            request {
                path("2.0/rent/user/me/flats/$FLAT_ID/house-services/periods/$PERIOD_ID")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "period" to "2021-10"
                        receipts?.let { "houseServiceReceipt" to it }
                        paymentConfirmation?.let { "paymentConfirmation" to it }
                    }
                }
            }
        )
    }

    protected fun conflictError(): MockResponse {
        return response {
            setResponseCode(409)
            jsonBody {
                "error" to jsonObject {
                    "code" to "CONFLICT"
                    "message" to ERROR_MESSAGE
                }
            }
        }
    }

    companion object {

        const val PERIOD = "2021-10"
        const val PERIOD_ID = "2"
        const val TITLE = "Данные за\u00A0октябрь"
        const val IMAGE_URL = "https://localhost:8080/receipt.webp"
        const val NAMESPACE = "arenda"
        const val GROUP_ID = "65493"
        const val ERROR_MESSAGE = "Ошибочка вышла"
    }
}
