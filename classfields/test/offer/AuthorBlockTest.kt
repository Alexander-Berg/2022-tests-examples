package com.yandex.mobile.realty.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.clearExternalImagesDir
import com.yandex.mobile.realty.core.createImageOnExternalDir
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.OfferCardRobot
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.rule.DateRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AgencyCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author solovevai on 03.09.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AuthorBlockTest {

    private var activityTestRule = OfferCardActivityTestRule(offerId = "0", launchActivity = false)
    private val dateRule = DateRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        dateRule
    )

    @After
    fun clearImages() {
        clearExternalImagesDir()
    }

    @Test
    fun shouldShowAuthorBlockWithFullOwnerInfo() {
        configureWebServer {
            registerOfferCardWithFullOwnerInfo()
        }
        setTestDate()
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            collapseAppBar()
            waitUntil { containsAuthorView() }
            isAuthorBlockMatches("/AuthorBlockTest/shouldShowAuthorBlockWithFullOwnerInfo")
        }
    }

    @Test
    fun shouldShowAuthorBlockWithDegradedOwnerInfo() {
        configureWebServer {
            registerOfferCardWithDegradedOwnerInfo()
        }
        setTestDate()
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            collapseAppBar()
            waitUntil { containsAuthorView() }
            isAuthorBlockMatches("/AuthorBlockTest/shouldShowAuthorBlockWithDegradedOwnerInfo")
        }
    }

    @Test
    fun shouldShowAuthorBlockWithAgentInfo() {
        configureWebServer {
            registerOfferCardWithAgent()
        }
        setTestDate()
        activityTestRule.launchActivity()

        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            collapseAppBar()
            waitUntil { containsAuthorView() }
            isAuthorBlockMatches("/AuthorBlockTest/shouldShowAuthorBlockWithAgentInfo")
        }
    }

    @Test
    fun shouldShowAuthorBlockWithAgencyInfo() {
        configureWebServer {
            registerOfferCardWithAgency()
        }
        setTestDate()
        activityTestRule.launchActivity()

        createImageOnExternalDir(rColor = 255, gColor = 0, bColor = 0)

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            collapseAppBar()
            waitUntil { containsAuthorView() }
            isAuthorBlockMatches("/AuthorBlockTest/shouldShowAuthorBlockWithAgencyInfo")
        }
    }

    @Test
    fun shouldShowAuthorBlockWithDeveloperInfo() {
        configureWebServer {
            registerOfferCardWithDeveloper()
        }
        setTestDate()
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            collapseAppBar()
            waitUntil { containsAuthorView() }
            isAuthorBlockMatches("/AuthorBlockTest/shouldShowAuthorBlockWithDeveloperInfo")
        }
    }

    @Test
    fun shouldShowAuthorBlockWithPrivateAgentInfo() {
        configureWebServer {
            registerOfferCardWithPrivateAgent()
        }
        setTestDate()
        activityTestRule.launchActivity()

        createImageOnExternalDir(rColor = 0, gColor = 0, bColor = 255)

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            collapseAppBar()
            waitUntil { containsAuthorView() }
            isAuthorBlockMatches("/AuthorBlockTest/shouldShowAuthorBlockWithPrivateAgentInfo")
        }
    }

    @Test
    fun shouldShowAuthorBlockWithYandexRentInfo() {
        configureWebServer {
            registerOfferCardWithYandexRent()
        }
        activityTestRule.launchActivity()

        createImageOnExternalDir(rColor = 255, gColor = 0, bColor = 0)

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            collapseAppBar()
            waitUntil { containsAuthorView() }
            scrollByFloatingButtonHeight()
            isAuthorBlockMatches("/AuthorBlockTest/shouldShowAuthorBlockWithYandexRentInfo")

            tapOn(lookup.matchesAuthorView())
        }

        onScreen<AgencyCardScreen> {
            expandedAppBarTitle.waitUntil { isTextEquals("Яндекс.Аренда") }
        }
    }

    private fun setTestDate() {
        dateRule.setDate(2020, 12, 14)
    }

    private fun OfferCardRobot.isAuthorBlockMatches(key: String) {
        onView(lookup.matchesAuthorView())
            .takeViewScreenshot()
            .check(key)
    }

    private fun DispatcherRegistry.registerOfferCardWithFullOwnerInfo() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("authorTest/cardWithFullOwnerInfo.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferCardWithDegradedOwnerInfo() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("authorTest/cardWithDegradedOwnerInfo.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferCardWithAgent() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("authorTest/cardWithAgentInfo.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferCardWithAgency() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("authorTest/cardWithAgencyInfo.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferCardWithDeveloper() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("authorTest/cardWithDeveloperInfo.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferCardWithPrivateAgent() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("authorTest/cardWithPrivateAgentInfo.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferCardWithYandexRent() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("authorTest/cardWithYandexRentInfo.json")
            }
        )
    }
}
