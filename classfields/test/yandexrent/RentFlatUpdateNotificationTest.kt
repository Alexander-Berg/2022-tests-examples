package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.*
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 11/17/21.
 */
@LargeTest
class RentFlatUpdateNotificationTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun showServicesUpdateFallback() {
        configureWebServer {
            registerTenantRentFlat(
                notification = jsonObject {
                    "fallback" to jsonObject {
                        "title" to "У вас старая версия приложения"
                        "subtitle" to "Некоторые действия в ней недоступны"
                        "updateAction" to jsonObject { }
                    }
                }
            )
        }

        checkUpdateFallback()
    }

    @Test
    fun showFlatUpdateFallback() {
        configureWebServer {
            registerTenantRentFlat(
                notification = jsonObject {
                    "fallback" to jsonObject {
                        "title" to "У вас старая версия приложения"
                        "subtitle" to "Некоторые действия в ней недоступны"
                        "updateAction" to jsonObject { }
                    }
                }
            )
        }

        checkUpdateFallback()
    }

    @Test
    fun showFlatTodoUpdateFallback() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerConfirmedTodo" to jsonObject {
                        "items" to jsonArrayOf(
                            jsonObject {
                                "addFlatPhotos" to jsonObject {}
                                "done" to false
                            },
                            jsonObject {
                                "addFlatInfo" to jsonObject {}
                                "done" to false
                            },
                            jsonObject {
                                "addPassport" to jsonObject {}
                                "done" to false
                            },
                            jsonObject {
                                "newTodo" to jsonObject {}
                                "done" to false
                            },
                        )
                    }
                }
            )
        }

        checkUpdateFallback()
    }

    @Test
    fun showServicesTodoUpdateFallback() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerPaymentInfoTodo" to jsonObject {
                        "items" to jsonArrayOf(
                            jsonObject {
                                "addPaymentCard" to jsonObject {}
                                "done" to false
                            },
                            jsonObject {
                                "addInn" to jsonObject {}
                                "done" to false
                            },
                            jsonObject {
                                "newTodo" to jsonObject {}
                                "done" to false
                            },
                        )
                    }
                }
            )
        }

        checkUpdateFallback()
    }

    @Test
    fun showOnlyOneUpdateFallback() {
        configureWebServer {
            registerRentFlat(
                rentRole = RENT_ROLE_OWNER,
                notifications = listOf(
                    jsonObject {
                        "fallback" to jsonObject {
                            "title" to "У вас старая версия приложения"
                            "subtitle" to "Некоторые действия в ней недоступны"
                            "updateAction" to jsonObject { }
                        }
                    },
                    jsonObject {
                        "ownerConfirmedTodo" to jsonObject {
                            "items" to jsonArrayOf(
                                jsonObject {
                                    "addFlatPhotos" to jsonObject {}
                                    "done" to false
                                },
                                jsonObject {
                                    "addFlatInfo" to jsonObject {}
                                    "done" to false
                                },
                                jsonObject {
                                    "addPassport" to jsonObject {}
                                    "done" to false
                                },
                                jsonObject {
                                    "newTodo" to jsonObject {}
                                    "done" to false
                                },
                            )
                        }
                    }
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val updateTitle = getResourceString(R.string.yandex_rent_update_fallback_title)

            notificationItem(updateTitle)
                .waitUntil { listView.contains(this) }

            listView.isContentStateMatches(getTestRelatedFilePath("servicesState"))
        }
    }

    private fun checkUpdateFallback() {
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerMarketIntent()

        onScreen<RentFlatScreen> {
            val updateTitle = getResourceString(R.string.yandex_rent_update_fallback_title)

            listView.scrollTo(notificationItem(updateTitle))
                .isViewStateMatches("RentFlatUpdateNotificationTest/updateFallback")
                .invoke {
                    actionButton.click()
                }

            intended(matchesMarketIntent())
        }
    }
}
