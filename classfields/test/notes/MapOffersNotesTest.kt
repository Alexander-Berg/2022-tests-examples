package com.yandex.mobile.realty.test.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnNoteScreen
import com.yandex.mobile.realty.core.robot.performOnOfferMenuDialog
import com.yandex.mobile.realty.core.robot.performOnSearchBottomSheetScreen
import com.yandex.mobile.realty.core.robot.performOnSearchMapScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 1/26/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MapOffersNotesTest : NotesTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldAddNoteToMapOffer() {
        configureWebServer {
            registerMapSearchWithOneOffer()
            registerOffer()
            registerNoteSaving(OFFER_ID, TEXT)
        }

        activityTestRule.launchActivity()

        performOnSearchMapScreen {
            waitUntil { isMapViewShown() }
            moveMapTo(LATITUDE, LONGITUDE)
            waitUntil { containsPin(OFFER_ID) }
            tapOnPin(OFFER_ID)
        }
        performOnSearchBottomSheetScreen {
            waitUntil { isCollapsed() }
            waitUntil { containsOfferSnippet(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesMenuButton())
            }
        }
        performOnOfferMenuDialog {
            isAddNoteButtonShown()
            tapOn(lookup.matchesAddNoteButton())
        }
        performOnNoteScreen {
            typeText(lookup.matchesInputView(), TEXT)
            tapOn(lookup.matchesSubmitButton())
        }
        performOnSearchBottomSheetScreen {
            waitUntil { containsOfferSnippet(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                isNoteShown(TEXT)
            }
        }
    }

    @Test
    fun shouldAddNoteToMultiHouseOffer() {
        configureWebServer {
            registerMapSearchWithMultiHouse()
            registerOffer()
            registerNoteSaving(OFFER_ID, TEXT)
        }

        activityTestRule.launchActivity()

        performOnSearchMapScreen {
            waitUntil { isMapViewShown() }
            moveMapTo(LATITUDE, LONGITUDE)
            waitUntil { containsPin(MULTI_HOUSE_ID) }
            tapOnPin(MULTI_HOUSE_ID)
        }
        performOnSearchBottomSheetScreen {
            waitUntil { isCollapsed() }
            waitUntil { containsOfferSnippet(OFFER_ID) }
            expand()
            waitUntil { isExpanded() }
            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesMenuButton())
            }
        }
        performOnOfferMenuDialog {
            isAddNoteButtonShown()
            tapOn(lookup.matchesAddNoteButton())
        }
        performOnNoteScreen {
            typeText(lookup.matchesInputView(), TEXT)
            tapOn(lookup.matchesSubmitButton())
        }
        performOnSearchBottomSheetScreen {
            waitUntil { containsOfferSnippet(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                isNoteShown(TEXT)
            }
        }
    }

    private fun DispatcherRegistry.registerMapSearchWithOneOffer() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                assetBody("callButtonTest/pointStatisticSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMapSearchWithMultiHouse() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                assetBody("callButtonTest/pointStatisticSearchMultiHouse.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("notesTest/offerWithSiteSearch.json")
            }
        )
    }

    companion object {

        private const val MULTI_HOUSE_ID = "3"
        private const val LATITUDE = 55.75793
        private const val LONGITUDE = 37.597424
    }
}
