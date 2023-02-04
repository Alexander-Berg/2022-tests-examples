package com.yandex.mobile.realty.test.concierge

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConciergeProposalScreen
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.core.webserver.success
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Created by Alena Malchikhina on 26.02.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ConciergeProposalTest {

    val activityTestRule = FilterActivityTestRule(launchActivity = false)
    private val regionParams = RegionParams(
        REGION_ID,
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

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(regionParams = regionParams, listMode = true),
        activityTestRule,
    )

    @Test
    fun shouldSendConciergeProposal() {
        configureWebServer {
            registerConciergeProposal(success())
            registerOfferWithSiteSearch("OFFER")
            registerUserProfile()
        }

        activityTestRule.launchActivity()

        onScreen<FiltersScreen> {
            dealTypeSelector.click()
            dealTypePopupBuy.click()
            propertyTypeSelector.click()
            propertyTypePopupApartment.click()
            apartmentCategorySelectorAny.click()

            submitButton.click()
        }

        onScreen<SearchListScreen> {
            waitUntil {
                listView.contains(conciergeSnippetItem)
            }
            conciergeSnippetItem.view.isViewStateMatches("ConciergeProposalTest/conciergeSnippet")
            conciergeSnippetItem.view.click()
        }

        onScreen<ConciergeProposalScreen> {
            phoneInputView.waitUntil { isCompletelyDisplayed() }
            isViewStateMatches("ConciergeProposalTest/conciergeProposalScreen")

            phoneInputView.textMatches(PHONE_PRESENTATION)

            sendButton.click()

            fullscreenSuccessView.waitUntil {
                isCompletelyDisplayed()
            }
            isViewStateMatches("ConciergeProposalTest/conciergeProposalSuccess")
            okButton.click()
        }

        onScreen<SearchListScreen> {
            waitUntil {
                listView.contains(offerSnippet("1"))
            }
        }
    }

    @Test
    fun shouldShowErrorView() {
        configureWebServer {
            registerOfferWithSiteSearch("NEWBUILDING")
            registerConciergeProposal(error())
            registerConciergeProposal(success())
        }

        activityTestRule.launchActivity()

        onScreen<FiltersScreen> {
            dealTypeSelector.click()
            dealTypePopupBuy.click()
            propertyTypeSelector.click()
            propertyTypePopupApartment.click()
            apartmentCategorySelectorNew.click()

            submitButton.click()
        }

        onScreen<SearchListScreen> {
            waitUntil {
                listView.contains(conciergeSnippetItem)
            }
            conciergeSnippetItem.view.isViewStateMatches("ConciergeProposalTest/conciergeSnippet")
            conciergeSnippetItem.view.click()
        }

        onScreen<ConciergeProposalScreen> {
            phoneInputView.waitUntil { isCompletelyDisplayed() }

            sendButton.click()
            phoneErrorView.waitUntil { isCompletelyDisplayed() }
                .isTextEquals(R.string.empty_phone_error_text)
            phoneInputView.typeText(PARTIAL_PHONE_TYPED)
            sendButton.click()
            phoneErrorView.isTextEquals(R.string.generic_phone_error_text)
            phoneInputView.clearText()

            phoneInputView.typeText(PHONE_TYPED)
            phoneInputView.textMatches(PHONE_PRESENTATION)
            phoneErrorView.waitUntil { isHidden() }

            sendButton.click()
            toastView(getResourceString(R.string.error_concierge_request))
                .isCompletelyDisplayed()
            sendButton.click()

            fullscreenSuccessView.waitUntil { isCompletelyDisplayed() }
            okButton.click()
        }

        onScreen<SearchListScreen> {
            waitUntil {
                listView.contains(offerSnippet("1"))
            }
        }
    }

    @Test
    fun shouldNotShowConciergeSnippetForSecondaryOffers() {
        configureWebServer {
            registerOfferWithSiteSearch("OFFER", "NO")
        }

        activityTestRule.launchActivity()

        onScreen<FiltersScreen> {
            dealTypeSelector.click()
            dealTypePopupBuy.click()
            propertyTypeSelector.click()
            propertyTypePopupApartment.click()
            apartmentCategorySelectorSecondary.click()

            submitButton.click()
        }

        onScreen<SearchListScreen> {
            waitUntil {
                listView.contains(offerSnippet("1"))
            }
            listView.doesNotContain(conciergeSnippetItem)
        }
    }

    private fun DispatcherRegistry.registerOfferWithSiteSearch(
        objectType: String,
        isNewFlat: String? = null
    ) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")

                queryParam("page", "0")
                queryParam("category", "APARTMENT")
                queryParam("type", "SELL")
                queryParam("objectType", objectType)
                isNewFlat?.let { queryParam("newFlat", isNewFlat) }
            },
            response {
                assetBody("conciergeTest/offerWithSiteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfile() {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                assetBody("conciergeTest/userProfile.json")
            }
        )
    }

    private fun DispatcherRegistry.registerConciergeProposal(response: MockResponse) {
        register(
            request {
                method("POST")
                path("2.0/concierge/ticket")
                body(
                    """
                      {
                        "rgid": $REGION_ID,
                        "phone": $PHONE
                      }  
                    """.trimIndent()
                )
            },
            response
        )
    }

    companion object {
        private const val PHONE = "+71112223344"
        private const val PHONE_TYPED = "1112223344"
        private const val PARTIAL_PHONE_TYPED = "1112223"
        private const val PHONE_PRESENTATION = "+7 (111) 222-33-44"
        private const val REGION_ID = 0
    }
}
