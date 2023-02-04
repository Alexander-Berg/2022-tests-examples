package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PublicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.UserOfferDraftRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author matek3022 on 2020-07-08.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class RedirectPhonesTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule = PublicationFormActivityTestRule(launchActivity = false)
    private val draftRule = UserOfferDraftRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        SetupDefaultAppStateRule(),
        activityTestRule,
        draftRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun shouldShowRedirectPhonesDisabled() {
        configureWebServer {
            registerUserProfile(enabledRedirectPhones = false, isNaturalPerson = true)
            registerUserProfile(enabledRedirectPhones = false, isNaturalPerson = true)
        }

        preparePublicationFormScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            containsRedirectPhonesBlock(false)
        }
    }

    @Test
    fun shouldShowRedirectPhonesEnabled() {
        configureWebServer {
            registerUserProfile(enabledRedirectPhones = true, isNaturalPerson = true)
            registerUserProfile(enabledRedirectPhones = true, isNaturalPerson = true)
        }

        preparePublicationFormScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            containsRedirectPhonesBlock(true)
        }
    }

    @Test
    fun shouldNotShowRedirectPhones() {
        configureWebServer {
            registerUserProfile(enabledRedirectPhones = false, isNaturalPerson = false)
            registerUserProfile(enabledRedirectPhones = false, isNaturalPerson = false)
        }

        preparePublicationFormScreen()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            isRedirectPhonesBlockNotShown()
        }
    }

    private fun preparePublicationFormScreen() {
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
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
}
