package com.yandex.mobile.realty.test.yandexrent.showings

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ShowingsActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentShowingsScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.showingWidget
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 29.06.2022
 */
@LargeTest
class ShowingsInNotRentRegionTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ShowingsActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(
            regionParams = RegionParams(
                0,
                0,
                "в Москве и МО",
                emptyMap(),
                emptyMap(),
                RegionParamsConfigImpl.DEFAULT.schoolInfo,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                0,
                null
            )
        ),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowEmptyScreenWithoutAction() {
        configureWebServer {
            registerShowings(showings = emptyList())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingsScreen> {
            fullscreenEmptyItem
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("emptyView"))
        }
    }

    @Test
    fun shouldHideOtherOffersButton() {
        configureWebServer {
            registerShowing(widget = showingWidget(WIDGET_TEXT))
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingsScreen> {
            notificationItem(WIDGET_TEXT)
                .waitUntil { listView.contains(this) }
            listView.doesNotContain(otherOffersButton)
        }
    }

    private companion object {

        const val WIDGET_TEXT = "Всё идет по плану"
    }
}
