package com.yandex.mobile.realty.test.yandexrent

import android.content.Intent
import androidx.test.espresso.intent.matcher.IntentMatchers
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.test.services.CONTRACT_ID
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.hamcrest.Matcher

/**
 * @author sorokinandrei on 4/14/22
 */
const val OWNER_REQUEST_ID = "ownerRequestId00001"
const val SHOWING_ID = "showingId0001"
const val PAYMENT_ID = "paymentId00001"
const val APARTMENT_IMAGE_URL = "https://localhost:8080/apartment-photo-small.webp"

fun rentImage(url: String, name: String = "name"): JsonObject {
    return jsonObject {
        "namespace" to "arenda"
        "groupId" to "group"
        "name" to name
        "imageUrls" to jsonArrayOf(
            jsonObject {
                "alias" to "1024x1024"
                "url" to url
            },
            jsonObject {
                "alias" to "orig"
                "url" to url
            }
        )
    }
}

fun contractNotification(type: String): JsonObject {
    return jsonObject {
        type to jsonObject {
            "contractId" to CONTRACT_ID
        }
    }
}

fun matchesAcquiringIntent(): Matcher<Intent> {
    return NamedIntentMatcher(
        "Открытие экрана оплаты через Тинькофф",
        IntentMatchers.hasComponent("ru.tinkoff.acquiring.sdk.ui.activities.PaymentActivity")
    )
}

fun contractSummary(): JsonArray {
    return Gson().fromJson(
        """
                    [{
                        "name": "Основная информация:",
                        "items": [{
                            "name": "Сумма ежемесячного платежа",
                            "value": "100000 Р/мес"
                        }, {
                            "name": "Тип недвижимости",
                            "value": "Квартира"
                        }, {
                            "name": "День арендной платы",
                            "value": "4"
                        }, {
                            "name": "Дата сдачи",
                            "value": "24.06.2022"
                        }, {
                            "name": "Тип договора",
                            "value": "От собственника"
                        }, {
                            "name": "Статус арендодателя",
                            "value": "Физ. лицо"
                        }, {
                            "name": "Наличие животных",
                            "value": "Без животных"
                        }]
                    }, {
                        "name": "Данные собственника",
                        "items": [{
                            "name": "ФИО",
                            "value": "Владелец Андрей"
                        }, {
                            "name": "Телефон",
                            "value": "+79336687741"
                        }, {
                            "name": "Электронная почта",
                            "value": "owner_a@yandex.ru"
                        }]
                    }, {
                        "name": "Данные жильца",
                        "items": [{
                            "name": "ФИО",
                            "value": "Жилец Пётр"
                        }, {
                            "name": "Телефон",
                            "value": "+79336687742"
                        }, {
                            "name": "Электронная почта",
                            "value": "tenant_p@gmail.com"
                        }]
                    }]
        """.trimIndent(),
        JsonArray::class.java
    )
}
