package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnSiteCardScreen
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
 * @author shpigun on 11.12.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteCardDescriptionTest {

    private var activityTestRule = SiteCardActivityTestRule(
        siteId = "0",
        launchActivity = false
    )

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun showDescriptionWithExpandButton() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatAllInfo.json")
        }

        activityTestRule.launchActivity()

        performOnSiteCardScreen {
            collapseAppBar()

            waitUntil { containsDetailsSectionTitle() }
            isDescriptionTextMatches(
                "SiteCardDescriptionTest/showDescriptionWithExpandButton/collapsed"
            )
            scrollToPosition(lookup.matchesDescriptionTextView())
                .tapOnLinkText("подробнее\u2026")

            waitUntil { containsDescriptionText() }
            isDescriptionTextMatches(
                "SiteCardDescriptionTest/showDescriptionWithExpandButton/expanded"
            )
        }
    }

    @Test
    fun showDescriptionWithoutExpandButton() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatShortDescription.json")
        }

        activityTestRule.launchActivity()

        performOnSiteCardScreen {
            collapseAppBar()
            waitUntil { containsDetailsSectionTitle() }
            isDescriptionTextMatches(
                "SiteCardDescriptionTest/showDescriptionWithoutExpandButton/description"
            )
        }
    }

    private fun DispatcherRegistry.registerSiteWithOfferStat(responseFileName: String) {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("siteCardTest/$responseFileName")
            }
        )
    }

    companion object {

        private const val SITE_ID = "0"
    }
}
