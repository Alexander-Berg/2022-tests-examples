package com.yandex.mobile.realty.test.yandexrent.nps

import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.*
import com.yandex.mobile.realty.utils.jsonObject
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 6/16/22.
 */
@LargeTest
class RentNpsTest : BaseTest() {

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
    fun shouldSubmitScoreFromNotification() {
        configureWebServer {
            registerOwnerRentFlat(notification = npsNotification())
            registerSubmitScore(success())
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_nps_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("npsNotification"))
                .invoke { actionButton.click() }
        }

        onScreen<RentNpsScreen> {
            waitUntil { scoreView.isCompletelyDisplayed() }

            root.isViewStateMatches(getTestRelatedFilePath("scoreEmpty"))

            scoreView.scrollTo(scoreItem(1))
            root.isViewStateMatches(getTestRelatedFilePath("screenNegative"))
            scoreView.isViewStateMatches(getTestRelatedFilePath("scoreNegative"))

            scoreView.scrollTo(scoreItem(7))
            root.isViewStateMatches(getTestRelatedFilePath("screenModerate"))
            scoreView.isViewStateMatches(getTestRelatedFilePath("scoreModerate"))

            scoreView.scrollTo(scoreItem(SCORE_POSITIVE))
            root.isViewStateMatches(getTestRelatedFilePath("screenPositive"))
            scoreView.isViewStateMatches(getTestRelatedFilePath("scorePositive"))

            fillComment()

            submitButton.click()

            toastView(getResourceString(R.string.yandex_rent_nps_success_message))
                .waitUntil { isCompletelyDisplayed() }
        }

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
            val title = getResourceString(R.string.yandex_rent_nps_title)
            listView.doesNotContain(notificationItem(title))
        }
    }

    @Test
    fun shouldShowErrors() {
        configureWebServer {
            registerOwnerRentFlat(notification = npsNotification())
            registerSubmitScore(error())
            registerSubmitScore(
                response {
                    setResponseCode(409)
                    jsonBody {
                        "error" to jsonObject {
                            "code" to "CONFLICT"
                            "message" to CONFLICT_MESSAGE
                        }
                    }
                }
            )
            registerSubmitScore(success())
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            val title = getResourceString(R.string.yandex_rent_nps_title)
            notificationItem(title)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }

        onScreen<RentNpsScreen> {
            waitUntil { scoreView.isCompletelyDisplayed() }

            scoreView.scrollTo(scoreItem(SCORE_POSITIVE))
            fillComment()
            submitButton.click()

            toastView(getResourceString(R.string.error_try_again))
                .waitUntil { isCompletelyDisplayed() }

            submitButton.click()

            toastView(CONFLICT_MESSAGE)
                .waitUntil { isCompletelyDisplayed() }

            submitButton.click()
        }

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
        }
    }

    private fun RentNpsScreen.fillComment() {
        commentContainerView.click()

        onScreen<RentCommentScreen> {
            waitUntil { messageView.isCompletelyDisplayed() }

            messageView.replaceText(COMMENT)
            doneButton.click()
        }
    }

    private fun npsNotification(): JsonObject {
        return jsonObject {
            "netPromoterScoreNotification" to jsonObject {}
        }
    }

    private fun DispatcherRegistry.registerSubmitScore(response: MockResponse) {
        register(
            request {
                method("POST")
                path("2.0/rent/user/me/score")
                jsonBody {
                    "score" to SCORE_POSITIVE
                    "comment" to COMMENT
                }
            },
            response
        )
    }

    private companion object {

        const val SCORE_POSITIVE = 10
        const val COMMENT = "Score comment"
        const val CONFLICT_MESSAGE = "some conflict"
    }
}
