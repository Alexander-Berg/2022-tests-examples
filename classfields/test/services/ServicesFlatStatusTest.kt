package com.yandex.mobile.realty.test.services

import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.domain.model.yandexrent.RentFlatStatus
import com.yandex.mobile.realty.domain.model.yandexrent.RentFlatStatus.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 30.11.2021
 */
@LargeTest
class ServicesFlatStatusTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowTenantFlat() {
        configureWebServer {
            registerTenantServicesInfo()
            registerTenantRentFlats(retouchedPhoto = retouchedPhoto())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("flat"))
        }
    }

    @Test
    fun shouldShowOwnerFlatInDraftStatus() {
        shouldShowOwnerFlat(DRAFT)
    }

    @Test
    fun shouldShowOwnerFlatInWaitingForConfirmationStatus() {
        shouldShowOwnerFlat(WAITING_FOR_CONFIRMATION)
    }

    @Test
    fun shouldShowOwnerFlatInConfirmedStatus() {
        shouldShowOwnerFlat(CONFIRMED)
    }

    @Test
    fun shouldShowOwnerFlatInWorkInProgressStatus() {
        shouldShowOwnerFlat(WORK_IN_PROGRESS)
    }

    @Test
    fun shouldShowOwnerFlatInRentedStatus() {
        shouldShowOwnerFlat(RENTED, retouchedPhoto(), contractInfo(insuranceIsActive = false))
    }

    @Test
    fun shouldShowOwnerFlatInRentedAndInsuredStatus() {
        shouldShowOwnerFlat(RENTED, retouchedPhoto(), contractInfo(insuranceIsActive = true))
    }

    @Test
    fun shouldShowOwnerFlatInDeniedStatus() {
        shouldShowOwnerFlat(DENIED)
    }

    @Test
    fun shouldShowOwnerFlatInCancelledWithoutSigningStatus() {
        shouldShowOwnerFlat(CANCELLED_WITHOUT_SIGNING)
    }

    @Test
    fun shouldShowOwnerFlatInAfterRentStatus() {
        shouldShowOwnerFlat(AFTER_RENT)
    }

    @Test
    fun shouldShowFlatWithEmptyAddress() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats(
                flatAddress = null,
                status = DRAFT.name
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(getResourceString(R.string.yandex_rent_flat_empty_address))
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("flat"))
        }
    }

    private fun shouldShowOwnerFlat(
        status: RentFlatStatus,
        retouchedPhoto: JsonObject? = null,
        contractInfo: JsonObject? = null
    ) {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats(
                status = status.name,
                retouchedPhoto = retouchedPhoto,
                contractInfo = contractInfo
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("flat"))
        }
    }

    private fun retouchedPhoto(): JsonObject {
        return jsonObject {
            "namespace" to "arenda-feed"
            "groupId" to "65725"
            "name" to "b40c250ae699afeaf2980d0741517d08"
            "imageUrls" to jsonArrayOf(
                jsonObject {
                    "alias" to "1024x1024"
                    "url" to FLAT_IMAGE_URL
                },
                jsonObject {
                    "alias" to "250x250"
                    "url" to FLAT_IMAGE_URL
                }
            )
        }
    }

    companion object {

        private const val FLAT_IMAGE_URL = "https://localhost:8080/apartment-photo-small.webp"
    }
}
