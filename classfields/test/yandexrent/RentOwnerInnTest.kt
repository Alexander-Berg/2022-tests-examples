package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.RentOwnerInnScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.utils.jsonObject
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrey-bgm on 28/10/2021.
 */
@LargeTest
class RentOwnerInnTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun addInn() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerWithoutInn" to jsonObject {}
                }
            )
            registerAddInn(success())
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val title = getResourceString(R.string.yandex_rent_owner_no_inn_title)

        onScreen<RentFlatScreen> {
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentOwnerInnScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
            isViewStateMatches(getTestRelatedFilePath("emptyContent"))
            innInputView.typeText("500100732259")
            addButton.click()
        }

        onScreen<RentFlatScreen> {
            waitUntil { listView.doesNotContain(notificationItem(title)) }
        }
    }

    @Test
    fun showErrors() {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    "ownerWithoutInn" to jsonObject {}
                }
            )
            registerAddInn(error())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_owner_no_inn_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentOwnerInnScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
            addButton.click()

            waitUntil {
                val text = getResourceString(R.string.yandex_rent_empty_inn)
                innErrorView(text).isCompletelyDisplayed()
            }
            isViewStateMatches(getTestRelatedFilePath("emptyInnErrorContent"))
            innInputView.typeText("123456789012")
            addButton.click()

            waitUntil {
                val text = getResourceString(R.string.yandex_rent_wrong_inn)
                innErrorView(text).isCompletelyDisplayed()
            }
            innInputView.clearText()
            innInputView.typeText("500100732259")
            addButton.click()

            toastView(getResourceString(R.string.error_try_again)).isCompletelyDisplayed()
        }
    }

    private fun DispatcherRegistry.registerAddInn(response: MockResponse) {
        register(
            request {
                path("2.0/rent/user/me")
                method("PATCH")
                jsonBody {
                    "paymentData" to jsonObject {
                        "inn" to "500100732259"
                    }
                }
            },
            response
        )
    }
}
