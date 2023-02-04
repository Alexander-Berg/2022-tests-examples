package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
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
 * Created by Alena Malchikhina on 15.04.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteCardDetailsTest {

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
    fun shouldShowAllDetailsOnMoreButtonTap() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatAllDetails.json")
        }
        activityTestRule.launchActivity()

        performOnSiteCardScreen {
            collapseAppBar()

            waitUntil { containsDetailsSectionTitle() }

            containsDetailSectionItemView(R.string.site_agreement_type)
            performOnDetailSectionView(R.string.site_agreement_type) {
                isTextEquals("214 ФЗ, ДДУ")
            }

            containsDetailSectionItemView(R.string.site_building_phase)
            performOnDetailSectionView(R.string.site_building_phase) {
                isTextEquals("2")
            }

            containsDetailSectionItemView(R.string.site_buildings)
            performOnDetailSectionView(R.string.site_buildings) {
                isTextEquals("6")
            }

            containsDetailSectionItemView(R.string.site_floors)
            performOnDetailSectionView(R.string.site_floors) {
                isTextEquals("до 29")
            }

            containsShowMoreDetailsButton()
            scrollByFloatingButtonHeight()
            isShowMoreDetailsButtonTextEquals("8 особенностей")

            tapOn(lookup.matchesShowMoreDetailsSectionButton())

            waitUntil { doesNotContainShowMoreDetailsButton() }

            containsDetailSectionItemView(R.string.site_apartments)
            performOnDetailSectionView(R.string.site_apartments) {
                isTextEquals("1776")
            }

            containsDetailSectionItemView(R.string.nb_ceiling_height)
            performOnDetailSectionView(R.string.nb_ceiling_height) {
                isTextEquals("3,1 м")
            }

            containsDetailSectionItemView(R.string.site_walls_type)
            performOnDetailSectionView(R.string.site_walls_type) {
                isTextEquals(
                    "монолитный, кирпичный, " +
                        "кирпично-монолитный, панельный, блочный, " +
                        "деревянный, железобетонный, металлический"
                )
            }

            containsDetailSectionItemView(R.string.nb_decoration)
            performOnDetailSectionView(R.string.nb_decoration) {
                isTextEquals("чистовая, черновая, под ключ")
            }

            containsDetailSectionItemView(R.string.site_parking_places)
            performOnDetailSectionView(R.string.site_parking_places) {
                isTextEquals("подземный - 1408,\nесть открытый,\nкрытый - 14")
            }

            containsAdditionalDetailsTitle()

            containsAdditionalDetailsSectionItemView(R.string.nb_summary_closed_area)
            containsAdditionalDetailsSectionItemView(R.string.site_security)
            containsAdditionalDetailsSectionItemView(R.string.site_concierge)
        }
    }

    @Test
    fun shouldShowCeilingHeightRangeDetailOnMoreButtonTap() {
        configureWebServer {
            registerSiteWithOfferStat(
                "siteWithOfferStatAllDetailsWithCeilingHeightRange.json"
            )
        }
        activityTestRule.launchActivity()

        performOnSiteCardScreen {
            collapseAppBar()

            waitUntil { containsDetailsSectionTitle() }
            containsShowMoreDetailsButton()
            scrollByFloatingButtonHeight()
            tapOn(lookup.matchesShowMoreDetailsSectionButton())
            waitUntil { doesNotContainShowMoreDetailsButton() }

            containsDetailSectionItemView(R.string.nb_ceiling_height)
            performOnDetailSectionView(R.string.nb_ceiling_height) {
                isTextEquals("3,1–3,2 м")
            }
        }
    }

    @Test
    fun shouldNotShowMoreDetailsButton() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatFiveDetails.json")
        }
        activityTestRule.launchActivity()

        performOnSiteCardScreen {
            collapseAppBar()

            waitUntil { containsDetailsSectionTitle() }

            containsDetailSectionItemView(R.string.site_agreement_type)
            performOnDetailSectionView(R.string.site_agreement_type) {
                isTextEquals("214 ФЗ, ДДУ")
            }

            containsDetailSectionItemView(R.string.site_building_phase)
            performOnDetailSectionView(R.string.site_building_phase) {
                isTextEquals("2")
            }

            containsDetailSectionItemView(R.string.site_buildings)
            performOnDetailSectionView(R.string.site_buildings) {
                isTextEquals("6")
            }

            containsDetailSectionItemView(R.string.site_floors)
            performOnDetailSectionView(R.string.site_floors) {
                isTextEquals("до 29")
            }

            containsDetailSectionItemView(R.string.site_apartments)
            performOnDetailSectionView(R.string.site_apartments) {
                isTextEquals("1776")
            }

            doesNotContainShowMoreDetailsButton()
        }
    }

    private fun DispatcherRegistry.registerSiteWithOfferStat(responseFileName: String) {
        register(
            request {
                path("1.0/siteWithOffersStat.json")
            },
            response {
                assetBody("siteCardTest/$responseFileName")
            }
        )
    }
}
