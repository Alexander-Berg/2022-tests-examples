package com.yandex.mobile.realty.test.callButton

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.metrica.EventMatcher
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.ExpectedRequest
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher

/**
 * @author misha-kozlov on 23.04.2020
 */
abstract class CallButtonTest : BaseTest() {

    protected fun DispatcherRegistry.registerOfferPhone(offerId: String) {
        register(
            request {
                path("2.0/offers/$offerId/phones")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "contacts": [{
                                        "phones": [{
                                            "phoneNumber": "$PHONE"
                                        }],
                                        "statParams": "$STAT_PARAMS"
                                    }]
                                }
                            }"""
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerFewOfferPhones(offerId: String) {
        register(
            request {
                path("2.0/offers/$offerId/phones")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "contacts": [{
                                        "phones": [{
                                            "phoneNumber": "$PHONE"
                                        }, {
                                            "phoneNumber": "+71112223344"
                                        }],
                                        "statParams": "$STAT_PARAMS"
                                    }]
                                }
                            }"""
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerEmptyOfferPhones(offerId: String) {
        register(
            request {
                path("2.0/offers/$offerId/phones")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "contacts": [{
                                        "phones": []
                                    }]
                                }
                            }"""
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerSitePhone(siteId: String) {
        register(
            request {
                path("2.0/newbuilding/$siteId/contacts")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "salesDepartments": [{
                                        "phones": ["$PHONE"],
                                        "statParams": "$STAT_PARAMS"
                                    }]
                                }
                            }"""
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerEmptySitePhones(siteId: String) {
        register(
            request {
                path("2.0/newbuilding/$siteId/contacts")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "salesDepartments": []
                                }
                            }"""
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerVillagePhone(villageId: String) {
        register(
            request {
                path("2.0/village/$villageId/contacts")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "salesDepartments": [{
                                        "phones": ["$PHONE"],
                                        "statParams": "$STAT_PARAMS"
                                    }]
                                }
                            }"""
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerEmptyVillagePhones(villageId: String) {
        register(
            request {
                path("2.0/village/$villageId/contacts")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "salesDepartments": []
                                }
                            }"""
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerAgencyPhone(uid: String) {
        register(
            request {
                path("2.0/agencies/active/user/uid:$uid/phones")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "phones": [{
                                        "wholePhoneNumber": "$PHONE"
                                    }]
                                }
                            }"""
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerEmptyAgencyPhones(uid: String) {
        register(
            request {
                path("2.0/agencies/active/user/uid:$uid/phones")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "phones": []
                                }
                            }"""
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerOfferPhoneCallEvent(
        offerId: String,
        eventPlace: String,
        currentScreen: String
    ): ExpectedRequest {
        return registerPhoneCallEvent(
            jsonObject {
                "events" to jsonArrayOf(
                    jsonObject {
                        "eventType" to "PHONE_CALL"
                        "requestContext" to jsonObject {
                            "eventPlace" to eventPlace
                            "mobileReferer" to jsonObject {
                                "currentScreen" to currentScreen
                            }
                        }
                        "objectInfo" to jsonObject {
                            "offerInfo" to jsonObject {
                                "offerId" to offerId
                            }
                        }
                    }
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerSitePhoneCallEvent(
        siteId: String,
        eventPlace: String,
        currentScreen: String
    ): ExpectedRequest {
        return registerPhoneCallEvent(
            jsonObject {
                "events" to jsonArrayOf(
                    jsonObject {
                        "eventType" to "PHONE_CALL"
                        "requestContext" to jsonObject {
                            "eventPlace" to eventPlace
                            "mobileReferer" to jsonObject {
                                "currentScreen" to currentScreen
                            }
                        }
                        "objectInfo" to jsonObject {
                            "siteInfo" to jsonObject {
                                "siteId" to siteId
                            }
                        }
                    }
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerVillagePhoneCallEvent(
        villageId: String,
        eventPlace: String,
        currentScreen: String
    ): ExpectedRequest {
        return registerPhoneCallEvent(
            jsonObject {
                "events" to jsonArrayOf(
                    jsonObject {
                        "eventType" to "PHONE_CALL"
                        "requestContext" to jsonObject {
                            "eventPlace" to eventPlace
                            "mobileReferer" to jsonObject {
                                "currentScreen" to currentScreen
                            }
                        }
                        "objectInfo" to jsonObject {
                            "villageInfo" to jsonObject {
                                "villageId" to villageId
                            }
                        }
                    }
                )
            }
        )
    }

    @Suppress("MaxLineLength")
    private fun DispatcherRegistry.registerPhoneCallEvent(
        eventLogBody: JsonObject
    ): ExpectedRequest {
        val eventLogRequest = register(
            request {
                method("POST")
                path("1.0/event/log")
                partialBody(eventLogBody)
            },
            response {
                setBody("""{"response": {}}""")
            }
        )
        return eventLogRequest
    }

    protected fun offerPhoneCallEvent(
        offerId: String,
        source: Any,
        categories: JsonArray
    ): EventMatcher {
        return event("Позвонить") {
            "id" to offerId
            "Источник" to jsonObject { "для офферов" to source }
            "Категория объявления" to categories
        }
    }

    protected fun sitePhoneCallEvent(
        siteId: String,
        source: Any,
        categories: JsonArray
    ): EventMatcher {
        return event("Позвонить") {
            "id" to "site_$siteId"
            "Источник" to jsonObject { "для новостроек" to source }
            "Категория объявления" to categories
        }
    }

    protected fun villagePhoneCallEvent(
        villageId: String,
        source: Any,
        categories: JsonArray
    ): EventMatcher {
        return event("Позвонить") {
            "id" to "village_$villageId"
            "Источник" to jsonObject { "для КП" to source }
            "Категория объявления" to categories
        }
    }

    protected fun offerSnippet(source: String): JsonObject {
        return jsonObject { "сниппет объявления" to source }
    }

    protected fun siteSnippet(source: String): JsonObject {
        return jsonObject { "сниппет новостройки" to source }
    }

    protected fun villageSnippet(source: String): JsonObject {
        return jsonObject { "сниппет КП" to source }
    }

    protected fun registerCallIntent() {
        intending(hasAction(Intent.ACTION_CALL))
            .respondWith(ActivityResult(Activity.RESULT_OK, null))
    }

    protected fun isCallStarted() {
        intended(matchesCallIntent())
    }

    private fun matchesCallIntent(): Matcher<Intent> {
        return NamedIntentMatcher(
            "запуск звонилки с номером $PHONE",
            allOf(
                hasAction(Intent.ACTION_CALL),
                hasData("tel:$PHONE")
            )
        )
    }

    companion object {
        const val PHONE = "+79998887766"
        const val STAT_PARAMS = "FFFF2222"
    }
}
