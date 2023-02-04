package com.yandex.mobile.realty.test.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SettingsActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnSavedSearchesStandaloneScreen
import com.yandex.mobile.realty.core.robot.performOnSettingsScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author scrooge on 22.04.2019.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SettingsTest {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = SettingsActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldOpenSetting() {
        activityTestRule.launchActivity()

        performOnSettingsScreen {
            tapOn(lookup.matchesNotificationsSubscriptionsButton())
        }

        performOnSavedSearchesStandaloneScreen {
            waitUntil { isToolbarTitleShown() }
        }
    }

    @Test
    fun shouldShowCheckedRedirectPhonesFields() {
        authorizationRule.setUserAuthorized()
        configureWebServer {
            registerUserProfile(enabledRedirectPhones = true, isNaturalPerson = true)
            registerUserProfile(enabledRedirectPhones = true, isNaturalPerson = true)
        }
        activityTestRule.launchActivity()

        performOnSettingsScreen {
            waitUntil { containsProfileSectionTitle() }
            containsRedirectPhonesBlock(true)
        }
    }

    @Test
    fun shouldShowNotCheckedRedirectPhonesFields() {
        authorizationRule.setUserAuthorized()
        configureWebServer {
            registerUserProfile(enabledRedirectPhones = false, isNaturalPerson = true)
            registerUserProfile(enabledRedirectPhones = false, isNaturalPerson = true)
        }
        activityTestRule.launchActivity()

        performOnSettingsScreen {
            waitUntil { containsProfileSectionTitle() }
            containsRedirectPhonesBlock(false)
        }
    }

    @Test
    fun shouldUncheckRedirectFieldWhenTap() {
        authorizationRule.setUserAuthorized()
        configureWebServer {
            registerUserProfile(enabledRedirectPhones = true, isNaturalPerson = true)
            registerUserProfile(enabledRedirectPhones = true, isNaturalPerson = true)
            registerUserProfilePatch(false)
        }
        activityTestRule.launchActivity()

        performOnSettingsScreen {
            waitUntil { containsProfileSectionTitle() }
            containsRedirectPhonesBlock(true)
            tapOn(lookup.matchesRedirectPhonesCheckbox())
            waitUntil {
                containsRedirectPhonesBlock(false)
            }
        }
    }

    @Test
    fun shouldCheckRedirectFieldWhenTap() {
        authorizationRule.setUserAuthorized()
        configureWebServer {
            registerUserProfile(enabledRedirectPhones = false, isNaturalPerson = true)
            registerUserProfile(enabledRedirectPhones = false, isNaturalPerson = true)
            registerUserProfilePatch(true)
        }
        activityTestRule.launchActivity()

        performOnSettingsScreen {
            waitUntil { containsProfileSectionTitle() }
            containsRedirectPhonesBlock(false)
            tapOn(lookup.matchesRedirectPhonesCheckbox())
            waitUntil {
                containsRedirectPhonesBlock(true)
            }
        }
    }

    @Test
    fun shouldNotShowRedirectPhonesFields() {
        activityTestRule.launchActivity()

        performOnSettingsScreen {
            doesNotContainsProfileSectionTitle()
            doesNotContainsRedirectPhonesBlock()
        }
    }

    @Test
    fun shouldNotShowRedirectPhonesFieldsWithJuridicPerson() {
        authorizationRule.setUserAuthorized()
        configureWebServer {
            registerUserProfile(enabledRedirectPhones = false, isNaturalPerson = false)
            registerUserProfile(enabledRedirectPhones = false, isNaturalPerson = false)
        }
        activityTestRule.launchActivity()

        performOnSettingsScreen {
            doesNotContainsProfileSectionTitle()
            doesNotContainsRedirectPhonesBlock()
        }
    }

    private fun DispatcherRegistry.registerUserProfile(
        enabledRedirectPhones: Boolean,
        isNaturalPerson: Boolean
    ) {
        val paymentType = if (isNaturalPerson) "NATURAL_PERSON" else "JURIDICAL_PERSON"
        val body = """{
                                "response": {
                                    "valid": true,
                                    "user": {
                                        "name": "John",
                                        "status": "active",
                                        "phones": [
                                            {
                                                "id": "1",
                                                "phone": "+7111*****44",
                                                "select": true,
                                                "fullPhone": "+71112223344"
                                            }
                                        ],
                                        "email" : "john@gmail.com",
                                        "type": "OWNER",
                                        "redirectPhones": $enabledRedirectPhones,
                                        "paymentType": "$paymentType",
                                        "capaUser": false
                                    }
                                }
                            }"""
        register(
            request {
                path("1.0/user")
                method("GET")
            },
            response {
                setBody(body)
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfilePatch(
        enabledRedirectPhones: Boolean
    ) {
        register(
            request {
                path("1.0/user")
                method("PATCH")
                body(
                    """{
                                "user": {
                                    "redirectPhones": $enabledRedirectPhones
                                }
                            }
                         """
                )
            },
            response {
                setBody("")
            }
        )
    }
}
