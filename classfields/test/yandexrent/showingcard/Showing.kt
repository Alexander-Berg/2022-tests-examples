package com.yandex.mobile.realty.test.yandexrent.showingcard

import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.services.CONTRACT_ID
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.yandexrent.APARTMENT_IMAGE_URL
import com.yandex.mobile.realty.test.yandexrent.PAYMENT_ID
import com.yandex.mobile.realty.test.yandexrent.SHOWING_ID
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.SHOWING_TYPE_ACCENT
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.SHOWING_TYPE_IMPORTANT
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.SHOWING_TYPE_WARNING
import com.yandex.mobile.realty.utils.jsonObject
import okhttp3.mockwebserver.MockResponse

object Showing {

    const val OFFER_ID = "offerId0001"
    const val VIRTUAL_TOUR_URL = "https://localthost/tour?only-content=true"
    const val WIDGET_LINK_TEXT = "дополнительной информацией"
    const val WIDGET_LINK_URL = "https://localhost/info"
    const val WIDGET_SIMPLE_TEXT = "Работаем над вашей заявкой"
    const val ACTION_PAY_TEXT = "Оплатить"
    const val FALLBACK_URL = "https://localhost/showing-fallback"
    private const val EMPTY_ARRAY = "[]"

    fun body(
        widget: String,
        generalInfo: String = EMPTY_ARRAY,
        houseServices: String = EMPTY_ARRAY,
        facilities: String = EMPTY_ARRAY,
    ): String {
        return """
            {
                "data": {
                    "showing": {
                        "__typename": "Showing",
                        "showingId": "$SHOWING_ID",
                        "flat": {
                            "__typename": "Flat",
                            "flatId": "$FLAT_ID",
                            "address": {
                                "__typename": "FlatAddress",
                                "addressFromStreetToFlat": "Ланское шоссе, 20к1, кв. 524",
                                "address": "Россия, Санкт-Петербург, Ланское шоссе, 20к1"
                            },
                            "realtyOfferId": "$OFFER_ID",
                            "retouchedPhotos": [
                                {
                                    "__typename": "Image",
                                    "namespace": "arenda-feed",
                                    "groupId": 1401666,
                                    "name": "1c0a150c43c969c4c96cadbec08db3f7",
                                    "imageUrls": [
                                        {
                                            "__typename": "ImageUrl",
                                            "alias": "orig",
                                            "url": "$APARTMENT_IMAGE_URL"
                                        },
                                        {
                                            "__typename": "ImageUrl",
                                            "alias": "250x250",
                                            "url": "$APARTMENT_IMAGE_URL"
                                        },
                                        {
                                            "__typename": "ImageUrl",
                                            "alias": "1024x1024",
                                            "url": "$APARTMENT_IMAGE_URL"
                                        }
                                    ]
                                }
                            ]
                        },
                        "flatQuestionnaire": {
                            "__typename": "FlatQuestionnaire",
                            "payments": {
                                "__typename": "Payments",
                                "adValue": 4400000
                            },
                            "media": {
                                "__typename": "Media",
                                "tour3dUrl": "$VIRTUAL_TOUR_URL"
                            },
                            "generalInfoView": $generalInfo,
                            "houseServicesView": $houseServices,
                            "facilitiesView": $facilities
                        },
                        "widget": $widget,
                        "roommates": [
                            {
                                "__typename": "RommateUser"
                            } 
                        ]
                    }
                }
            }
        """.trimIndent()
    }

    fun simpleBody(widget: String): String {
        return """
            {
                "data": {
                    "showing": {
                        "__typename": "Showing",
                        "showingId": "$SHOWING_ID",
                        "flat": {
                            "__typename": "Flat",
                            "flatId": "$FLAT_ID",
                            "address": {
                                "__typename": "FlatAddress",
                                "address": "Россия, Санкт-Петербург, Ланское шоссе, 20к1"
                            },
                            "retouchedPhotos": []
                        },
                        "flatQuestionnaire": {
                            "__typename": "FlatQuestionnaire",
                            "generalInfoView": [],
                            "houseServicesView": [],
                            "facilitiesView": []
                        },
                        "widget": $widget,
                        "roommates": []
                    }
                }
            }
        """.trimIndent()
    }

    fun simpleGeneralInfo(): String {
        return """
            [
                {
                    "__typename": "GeneralInfoItem",
                    "name": "Площадь общая",
                    "value": "524 м²"
                }
            ]
        """.trimIndent()
    }

    fun fullGeneralInfo(): String {
        return """
            [
                {
                    "__typename": "GeneralInfoItem",
                    "name": "Площадь общая",
                    "value": "524 м²"
                },
                {
                    "__typename": "GeneralInfoItem",
                    "name": "Площадь кухни",
                    "value": "12 м²"
                },
                {
                    "__typename": "GeneralInfoItem",
                    "name": "Кол-во комнат",
                    "value": "1"
                },
                {
                    "__typename": "GeneralInfoItem",
                    "name": "Этаж",
                    "value": "2 из 5"
                },
                {
                    "__typename": "GeneralInfoItem",
                    "name": "Лифт",
                    "value": "2 пассажирских,1 грузовой"
                },
                {
                    "__typename": "GeneralInfoItem",
                    "name": "Животные",
                    "value": "Нельзя"
                }
            ]
        """.trimIndent()
    }

    fun fullHouseServices(): String {
        return """
            [
                {
                    "__typename": "HouseServiceItem",
                    "name": "Что ещё оплачивает жилец",
                    "value": "Электроэнергию, интернет, газ, отопление, вода холодная / горячая"
                },
                {
                    "__typename": "HouseServiceItem",
                    "name": "Интернет",
                    "value": "Подключён провайдер"
                },
                {
                    "__typename": "HouseServiceItem",
                    "name": "Стоимость интернета в месяц",
                    "value": "500₽"
                },
                {
                    "__typename": "HouseServiceItem",
                    "name": "Средняя стоимость коммуналки в месяц",
                    "value": "1200₽"
                }
            ]
        """.trimIndent()
    }

    fun fullFacilities(): String {
        return """
            [
                {
                    "__typename": "FacilityItem",
                    "name": "Холодильник",
                    "additionalInfo": null,
                    "type": "FRIDGE"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Духовой шкаф",
                    "additionalInfo": null,
                    "type": "OVEN"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Кухонная плита",
                    "additionalInfo": "Газовая",
                    "type": "STOVE"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Микроволновка",
                    "additionalInfo": null,
                    "type": "MICROWAVE"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Посуда",
                    "additionalInfo": null,
                    "type": "DISHES"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Посудомоечная машина",
                    "additionalInfo": null,
                    "type": "DISHWASHER"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Стиральная машина",
                    "additionalInfo": null,
                    "type": "WASHING_MACHINE"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Сушильная машина",
                    "additionalInfo": null,
                    "type": "DRYING_MACHINE"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Бойлер",
                    "additionalInfo": null,
                    "type": "BOILER"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Тёплые полы",
                    "additionalInfo": null,
                    "type": "WARM_FLOOR"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Раскладной диван",
                    "additionalInfo": null,
                    "type": "SOFA_BED"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Постельное бельё",
                    "additionalInfo": null,
                    "type": "BEDCLOTHES"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Пылесос",
                    "additionalInfo": null,
                    "type": "VACUUM_CLEANER"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Кондиционер",
                    "additionalInfo": null,
                    "type": "CONDITIONER"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Телевизор",
                    "additionalInfo": null,
                    "type": "TV"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Парковка",
                    "additionalInfo": "Городская/во дворе - бесплатная, городская - платная, за шлагбаумом, подземная",
                    "type": "PARKING"
                },
                {
                    "__typename": "FacilityItem",
                    "name": "Новое удобство",
                    "additionalInfo": "Новое удобное удобство",
                    "type": "NEW_FACILITY_TYPE"
                }
            ]
        """.trimIndent()
    }

    fun widgetWithUrlFallback(type: String = SHOWING_TYPE_ACCENT): String {
        return """
            {
                "__typename": "ShowingWidget",
                "html": "Заполните анкету и ознакомьтесь с <a href=\"$WIDGET_LINK_URL\">$WIDGET_LINK_TEXT</a>",
                "type": "$type",
                "widgetAction": {
                    "__typename": "ShowingWidgetAction",
                    "buttonText": "Заполнить",
                    "action": {
                        "__typename": "SomeNewAction"
                    },
                    "fallback": {
                        "__typename": "WidgetActionFallbackUrl",
                        "url": "$FALLBACK_URL"
                    }
                }
            }
        """.trimIndent()
    }

    fun widgetWithoutAction(): String {
        return """
            {
                "__typename": "ShowingWidget",
                "html": "$WIDGET_SIMPLE_TEXT",
                "type": "$SHOWING_TYPE_ACCENT"
            }
        """.trimIndent()
    }

    fun widgetWithUpdateFallback(): String {
        return """
            {
                "__typename": "ShowingWidget",
                "html": "Заполните анкету",
                "type": "$SHOWING_TYPE_ACCENT",
                "widgetAction": {
                    "__typename": "ShowingWidgetAction",
                    "buttonText": "Заполнить",
                    "action": {
                        "__typename": "SomeNewAction"
                    },
                    "fallback": {
                        "__typename": "WidgetActionFallbackUpdate"
                    }
                }
            }
        """.trimIndent()
    }

    fun widgetSaveCheckInDate(): String {
        return """
            {
                "__typename": "ShowingWidget",
                "html": "Собственник хочет сдать вам квартиру. Следующий шаг — выбрать дату заселения.",
                "type": "$SHOWING_TYPE_IMPORTANT",
                "widgetAction": {
                    "__typename": "ShowingWidgetAction",
                    "buttonText": "Выбрать дату",
                    "action": {
                        "__typename": "WidgetActionSaveCheckInDate"
                    }
                }
            }
        """.trimIndent()
    }

    fun widgetShareLinkToQuestionnaire(): String {
        return """
            {
                "__typename": "ShowingWidget",
                "html": "Поделитесь ссылкой с другими жильцами, с кем вы планируете жить.",
                "type": "$SHOWING_TYPE_ACCENT",
                "widgetAction": {
                    "__typename": "ShowingWidgetAction",
                    "buttonText": "Поделиться ссылкой",
                    "action": {
                        "__typename": "WidgetActionShareLinkToQuestionnaire"
                    }
                }
            }
        """.trimIndent()
    }

    fun widgetExploreConditionsOfHousingServices(): String {
        return """
            {
                "__typename": "ShowingWidget",
                "html": "Пожалуйста, ознакомьтесь с условиями ЖКХ как можно скорее",
                "type": "$SHOWING_TYPE_WARNING",
                "widgetAction": {
                    "__typename": "ShowingWidgetAction",
                    "buttonText": "Смотреть",
                    "action": {
                        "__typename": "WidgetActionExploreConditionsOfHousingServices"
                    }
                }
            }
        """.trimIndent()
    }

    fun widgetSignContract(): String {
        return """
            {
                "__typename": "ShowingWidget",
                "html": "Ознакомьтесь с договором, подпишите и потом оплатите первый месяц аренды",
                "type": "$SHOWING_TYPE_ACCENT",
                "widgetAction": {
                    "__typename": "ShowingWidgetAction",
                    "buttonText": "Подписать",
                    "action": {
                        "__typename": "WidgetActionSignContract",
                        "contractId": "$CONTRACT_ID"
                    }
                }
            }
        """.trimIndent()
    }

    fun widgetPayFirstMonth(): String {
        return """
            {
                "__typename": "ShowingWidget",
                "html": "Ознакомьтесь с договором, подпишите и потом оплатите первый месяц аренды",
                "type": "$SHOWING_TYPE_ACCENT",
                "widgetAction": {
                    "__typename": "ShowingWidgetAction",
                    "buttonText": "$ACTION_PAY_TEXT",
                    "action": {
                        "__typename": "WidgetActionPayFirstMonth",
                        "contractId": "$CONTRACT_ID",
                        "paymentId": "$PAYMENT_ID"
                    }
                }
            }
        """.trimIndent()
    }
}

fun DispatcherRegistry.registerShowingDetails(body: String) {
    registerShowingDetails(
        response { setBody(body) }
    )
}

fun DispatcherRegistry.registerShowingDetails(response: MockResponse) {
    register(
        request {
            method("POST")
            path("2.0/graphql")
            jsonPartialBody {
                "operationName" to "GetRentShowing"
                "variables" to jsonObject {
                    "showingId" to SHOWING_ID
                }
            }
        },
        response
    )
}
