package com.yandex.mobile.realty.test.services

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.UserOfferDraftRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AddOfferModeDialogScreen
import com.yandex.mobile.realty.core.screen.ConciergeProposalScreen
import com.yandex.mobile.realty.core.screen.MortgageProgramListScreen
import com.yandex.mobile.realty.core.screen.PublicationFormScreen
import com.yandex.mobile.realty.core.screen.PublicationWizardScreen
import com.yandex.mobile.realty.core.screen.ReportsScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.state.RealtyAppStateEditor
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 17.08.2021
 */
@LargeTest
class ServicesTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)
    private val appStateRule = SetupDefaultAppStateRule()
    private val draftRule = UserOfferDraftRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        activityTestRule,
        appStateRule,
        draftRule
    )

    @Test
    fun shouldOpenMortgageProgramList() {
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            mortgageServiceView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("ServicesTest/mortgageTile")
                .click()
        }
        onScreen<MortgageProgramListScreen> {
            waitUntil { listView.contains(screenTitleItem) }
        }
    }

    @Test
    fun shouldOpenPublicationWizard() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<ServicesScreen> {
            addOfferView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("ServicesTest/addOfferTile")
                .click()
        }
        onScreen<PublicationWizardScreen> {
            expandedToolbarTitleView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals("Новое объявление")
        }
    }

    @Test
    fun shouldDismissDraftAndOpenPublicationWizard() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }

        activityTestRule.launchActivity()
        draftRule.prepareSellApartment()
        authorizationRule.registerAuthorizationIntent()

        onScreen<ServicesScreen> {
            addOfferView
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<AddOfferModeDialogScreen> {
            titleView.waitUntil { isCompletelyDisplayed() }
            newOfferButton.click()
        }
        onScreen<PublicationWizardScreen> {
            expandedToolbarTitleView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals("Новое объявление")
        }
    }

    @Test
    fun shouldOpenPublicationFormWithDraft() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
        }

        activityTestRule.launchActivity()
        draftRule.prepareSellApartment()
        authorizationRule.registerAuthorizationIntent()

        onScreen<ServicesScreen> {
            addOfferView
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<AddOfferModeDialogScreen> {
            titleView.waitUntil { isCompletelyDisplayed() }
            draftButton.click()
        }
        onScreen<PublicationFormScreen> {
            expandedToolbarTitleView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals("Продать квартиру")
        }
    }

    @Test
    fun shouldOpenReportsScreen() {
        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<ServicesScreen> {
            reportsServiceView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("ServicesTest/reportsTile")
                .click()
        }
        onScreen<ReportsScreen> {
            waitUntil { toolbarTitleView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldOpenManualLinkInWebView() {
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            manualServiceView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("ServicesTest/manualTile")
                .click()
        }
        onScreen<WebViewScreen> {
            webView.waitUntil {
                isPageUrlEquals(
                    "https://realty.yandex.ru/journal/" +
                        "?utm_source=app_android_service&only-content=true"
                )
            }
            shareButton.isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldNotShowConciergeIfHasConciergeFalse() {
        appStateRule.setState {
            setupNoConciergeRegionParams()
        }
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            waitUntil { listView.contains(servicesTitleItem) }
            listView.doesNotContain(conciergeServiceView)
        }
    }

    @Test
    fun shouldShowAndOpenConciergeIfHasConciergeTrue() {
        appStateRule.setState {
            setupConciergeRegionParams()
        }
        configureWebServer {
            registerUserProfile()
        }
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            conciergeServiceView
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<ConciergeProposalScreen> {
            phoneInputView.waitUntil { isCompletelyDisplayed() }
            isViewStateMatches("ServicesTest/conciergeProposalScreen")
        }
    }

    private fun DispatcherRegistry.registerUserProfile() {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                assetBody("publishForm/userOwnerNoMosRu.json")
            }
        )
    }

    private fun RealtyAppStateEditor.setupConciergeRegionParams() {
        regionParams.set(
            RegionParams(
                0,
                0,
                "в Москве и МО",
                RegionParamsConfigImpl.DEFAULT.heatMapTypes,
                RegionParamsConfigImpl.DEFAULT.filters,
                RegionParamsConfigImpl.DEFAULT.schoolInfo,
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                0,
                null
            )
        )
    }

    private fun RealtyAppStateEditor.setupNoConciergeRegionParams() {
        regionParams.set(
            RegionParams(
                0,
                0,
                "в Москве и МО",
                RegionParamsConfigImpl.DEFAULT.heatMapTypes,
                RegionParamsConfigImpl.DEFAULT.filters,
                RegionParamsConfigImpl.DEFAULT.schoolInfo,
                true,
                true,
                true,
                true,
                false,
                false,
                false,
                0,
                null
            )
        )
    }

    private companion object {

        const val FLAT_ADDRESS = "Ланское шоссе, 20к3, кв. 100"
    }
}
