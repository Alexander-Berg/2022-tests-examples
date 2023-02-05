/*
 * This file is a part of the Yandex Search for Android project.
 *
 * (C) Copyright 2019. Yandex, LLC. All rights reserved.
 *
 * Author: Alexander Skvortsov <askvortsov@yandex-team.ru>
 */

package ru.yandex.searchplugin.taxi.configuration.kit

import com.squareup.moshi.JsonClass
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException

@RunWith(JUnit4::class)
class StartupMoshiTest {

    @Test
    @Throws(IOException::class)
    fun validResponse() {
        val data = """
        {
            "authorization_status": "authorized",
            "parameters": {
                "eats": {
                    "address_control_loading": "superapp_eats_address_control_loading",
			        "address_control_title": "superapp_eats_address_control_title",
			        "address_input_placeholder": "superapp_eats_address_input_placeholder",
			        "address_search_on_map_header": "superapp_eats_address_search_on_map_header",
			        "courier_max_distance_to_focus": 5000,
			        "enabled": true,
			        "header_image_tag": "superapp_eats_header_tag",
			        "mode": "eats",
			        "promo": "banner",
			        "service": "eats",
			        "service_name": "superapp_eats_service_name",
			        "splash_image_tag": "superapp_eats_splash_tag",
		        	"title": "superapp_eats_card_title",
		        	"url": "https://tc.mobile.yandex.net/4.0/eda-superapp/",
		        	"user_agent_component": "Superapp/Eats"
                },
                "grocery": {
                    "address_control_loading": "superapp_grocery_address_control_loading",
                    "address_control_title": "superapp_grocery_address_control_title",
			        "address_input_placeholder": "superapp_grocery_address_input_placeholder",
			        "address_search_on_map_header": "superapp_grocery_address_search_on_map_header",
			        "enabled": true,
			        "header_image_tag": "superapp_grocery_header_tag",
			        "mode": "grocery",
			        "promo": "banner",
			        "service": "grocery",
			        "service_name": "superapp_grocery_service_name",
			        "splash_image_tag": "superapp_grocery_splash_tag",
			        "subtitle": "superapp_grocery_card_subtitle",
			        "title": "superapp_grocery_card_title",
			        "url": "https://tc.mobile.yandex.net/4.0/eda-superapp/lavka",
			        "user_agent_component": "Superapp/Grocery"
                },
                "pharmacy": {
                    "address_control_loading": "superapp_pharmacy_address_control_loading",
                    "address_control_title": "superapp_pharmacy_address_control_title",
			        "address_input_placeholder": "superapp_pharmacy_address_input_placeholder",
			        "address_search_on_map_header": "superapp_pharmacy_address_search_on_map_header",
			        "enabled": true,
			        "header_image_tag": "superapp_pharmacy_header_tag",
			        "mode": "pharmacy",
			        "promo": "banner",
			        "service": "pharmacy",
			        "service_name": "superapp_pharmacy_service_name",
			        "splash_image_tag": "superapp_pharmacy_splash_tag",
			        "subtitle": "superapp_pharmacy_card_subtitle",
			        "title": "superapp_pharmacy_card_title",
			        "url": "https://tc.mobile.yandex.net/4.0/eda-superapp/apteki",
			        "user_agent_component": "Superapp/Pharmacy"
                },
                "tracking_api": "https://tc.mobile.yandex.net/4.0/eda-superapp/api/v2/orders/tracking",
                "api_base_url": "https://tc.mobile.yandex.net/payments",
                "billing_base_url": "https://tc.mobile.yandex.net/billing",
                "allow_late_login": true
            }
        }
        """
        val expectedResult = StartupResponseJson(
            StartupResponseJson.AuthorizationStatus.AUTHORIZED,
            StartupResponseJson.ParametersResponseJson(
                StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                    "superapp_eats_address_control_loading",
                    "superapp_eats_address_control_title",
                    "superapp_eats_address_input_placeholder",
                    "superapp_eats_address_search_on_map_header",
                    5000,
                    true,
                    "eats",
                    "superapp_eats_service_name",
                    "superapp_eats_card_title",
                    "https://tc.mobile.yandex.net/4.0/eda-superapp/",
                    "Superapp/Eats"
                ),
                StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                    "superapp_grocery_address_control_loading",
                    "superapp_grocery_address_control_title",
                    "superapp_grocery_address_input_placeholder",
                    "superapp_grocery_address_search_on_map_header",
                    null,
                    true,
                    "grocery",
                    "superapp_grocery_service_name",
                    "superapp_grocery_card_title",
                    "https://tc.mobile.yandex.net/4.0/eda-superapp/lavka",
                    "Superapp/Grocery"
                ),
                StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                    "superapp_pharmacy_address_control_loading",
                    "superapp_pharmacy_address_control_title",
                    "superapp_pharmacy_address_input_placeholder",
                    "superapp_pharmacy_address_search_on_map_header",
                    null,
                    true,
                    "pharmacy",
                    "superapp_pharmacy_service_name",
                    "superapp_pharmacy_card_title",
                    "https://tc.mobile.yandex.net/4.0/eda-superapp/apteki",
                    "Superapp/Pharmacy"
                ),
                "https://tc.mobile.yandex.net/4.0/eda-superapp/api/v2/orders/tracking",
                "https://tc.mobile.yandex.net/payments",
                "https://tc.mobile.yandex.net/billing",
                true
            )
        )
        assertEquals(expectedResult, makeResult<StartupResponseJson>(data))
    }

    @Test
    fun partialArrayParsing() {
        val data = """
        { "list": [{ "value": "a" }, "b", 1, "c", null, { "value": "d"}] }
        """
        val expectedResult = TestResponseJson(
            listOf("a", "d").map { TestResponseJson.InnerResponseJson(it) }
        )
        assertEquals(expectedResult, makeResult<TestResponseJson>(data))
    }

    @Test(expected = IOException::class)
    fun invalidJson() {
        makeResult<StartupResponseJson>("")
    }

    @Test
    fun invalidAuthStatusParsedAsNull() {
        val data = """
        {
            "authorization_status": "invalid",
            "parameters": {
                "eats": {
                    "address_control_loading": "superapp_eats_address_control_loading",
                    "service": "test_service",
                    "url": "test_url"
                }
            }
        }
        """
        val expectedResponse = StartupResponseJson(
            null,
            StartupResponseJson.ParametersResponseJson(
                StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                    "superapp_eats_address_control_loading",
                    service = "test_service",
                    url = "test_url"
                )
            )
        )
        assertEquals(expectedResponse, makeResult<StartupResponseJson>(data))
    }

    @Test
    fun whenMissingNonNullFieldsEatsIsNull() {
        val data = """
        {
            "authorization_status": "invalid",
            "parameters": {
                "eats": {
                    "address_control_loading": "superapp_eats_address_control_loading",
                    "service": "test_service"
                }
            }
        }
        """
        val expectedResponse = StartupResponseJson(
            null,
            StartupResponseJson.ParametersResponseJson()
        )
        assertEquals(expectedResponse, makeResult<StartupResponseJson>(data))
    }

    @Test
    fun errorParsing() {
        val data = """
        {
            "details": {
                "support_info": {
                    "support_page": {
                        "url": "support_url"
                    },
                    "support_phone": "123"
                }
            },
            "code": "error_code",
            "message": "error"
        }
        """
        val expectedResult = StartupErrorResponseJson(
            "error_code",
            "error",
            StartupErrorResponseJson.DetailsResponseJson(
                StartupErrorResponseJson.SupportInfoResponseJson(
                    StartupErrorResponseJson.SupportInfoResponseJson.SupportPageResponseJson(
                        "support_url"
                    ),
                    "123"
                )
            )
        )
        assertEquals(expectedResult, makeResult<StartupErrorResponseJson>(data))
    }
}

@Throws(IOException::class)
private inline fun <reified T> makeResult(data: String): T? {
    return StartupMoshi.value.adapter(T::class.java).fromJson(data)
}

@JsonClass(generateAdapter = true)
internal data class TestResponseJson(
    val list: List<InnerResponseJson>
) {
    @JsonClass(generateAdapter = true)
    data class InnerResponseJson(
        var value: String
    )
}
