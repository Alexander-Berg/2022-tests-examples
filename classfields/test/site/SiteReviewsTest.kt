package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.SiteReviewsActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.InternetRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.view.TView
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.graphql.schema.type.SortOrder
import com.yandex.mobile.realty.test.BaseTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 14.10.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteReviewsTest : BaseTest() {

    private var siteCardActivityTestRule = SiteCardActivityTestRule(
        siteId = SITE_ID,
        launchActivity = false
    )

    private var reviewsActivityTestRule = SiteReviewsActivityTestRule(
        permalink = SITE_PERMALINK,
        siteName = SITE_NAME,
        launchActivity = false
    )

    private val authorizationRule = AuthorizationRule()
    private val internetRule = InternetRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        siteCardActivityTestRule,
        reviewsActivityTestRule,
        authorizationRule,
        internetRule,
    )

    @Test
    fun showReviewsWithShowAllButtonInSiteCard() {
        configureWebServer {
            registerSiteWithOfferStat()
            registerReviewsSuccess("siteReviewsTotal178.json")
        }

        siteCardActivityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            review(REVIEW_ID_FIRST).waitUntil { listView.contains(this) }
            listView.isItemsStateMatches(
                key = getTestRelatedFilePath("reviews"),
                fromItem = reviewsSectionTitleItem,
                toItem = commButtons
            )
        }
    }

    @Test
    fun showReviewsWithoutShowAllButtonInSiteCard() {
        configureWebServer {
            registerSiteWithOfferStat()
            registerReviewsSuccess("siteReviewsTotal4.json")
        }

        siteCardActivityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            review(REVIEW_ID_FIRST).waitUntil { listView.contains(this) }
            listView.isItemsStateMatches(
                key = getTestRelatedFilePath("reviews"),
                fromItem = reviewsSectionTitleItem,
                toItem = commButtons
            )
        }
    }

    @Test
    fun showEmptyReviewsInSiteCard() {
        configureWebServer {
            registerSiteWithOfferStat()
            registerReviewsSuccess("siteReviewsEmpty.json")
        }

        siteCardActivityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            emptyReviewsSectionTitleItem.waitUntil { listView.contains(this) }
            listView.isItemsStateMatches(
                key = getTestRelatedFilePath("reviews"),
                fromItem = emptyReviewsSectionTitleItem,
                toItem = commButtons
            )
        }
    }

    @Test
    fun showReviewsErrorInSiteCard() {
        configureWebServer {
            registerSiteWithOfferStat()
            registerReviewsError()
            registerReviewsSuccess("siteReviewsTotal178.json")
        }

        siteCardActivityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            reviewsError.waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .retryButton.click()
            review(REVIEW_ID_FIRST).waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showReviewsScreenOnShowAllClicked() {
        configureWebServer {
            registerSiteWithOfferStat()
            registerReviewsSuccess("siteReviewsTotal178.json")
            registerReviewsSuccess("siteReviews20.json")
        }

        siteCardActivityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            appBar.collapse()
            showAllReviewsButton.waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
        }
        onScreen<SiteReviewsScreen> {
            review(REVIEW_ID_FIRST).waitUntil { listView.contains(this) }
            root.isViewStateMatches(getTestRelatedFilePath("reviewsScreen"))
        }
    }

    @Test
    fun showErrorsOnReviewsScreen() {
        configureWebServer {
            registerReviewsError()
            registerReviewsSuccess("siteReviews20.json")
            registerReviewsError()
            registerReviewsSuccess("siteReviews20Page2.json")
        }

        reviewsActivityTestRule.launchActivity()

        onScreen<SiteReviewsScreen> {
            errorView.waitUntil { listView.contains(this) }
                .click()
            review(REVIEW_ID_FIRST).waitUntil { listView.contains(this) }
            listView.scrollTo(review(REVIEW_ID_LAST_FIRST_PAGE))
            errorView.waitUntil { listView.contains(this) }
                .click()
            review(REVIEW_ID_LAST_SECOND_PAGE).waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun expandReviewText() {
        configureWebServer {
            registerReviewsSuccess("siteReviewsExpand.json")
        }

        reviewsActivityTestRule.launchActivity()

        onScreen<SiteReviewsScreen> {
            review(REVIEW_ID_WITH_EXPAND)
                .waitUntil { listView.contains(this) }
                .textView
                .isViewStateMatches(getTestRelatedFilePath("collapsed"))
                .tapOnLinkText(R.string.review_expand_label)
            review(REVIEW_ID_WITH_EXPAND)
                .waitUntil { listView.contains(this) }
                .textView
                .isViewStateMatches(getTestRelatedFilePath("expanded"))
        }
    }

    @Test
    fun showReviewsWithoutRatingAndText() {
        configureWebServer {
            registerReviewsSuccess("siteReviewsWithDifferentKinds.json")
        }

        reviewsActivityTestRule.launchActivity()

        onScreen<SiteReviewsScreen> {
            review(REVIEW_ID_FIRST)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("withTextAndRating"))
            review(REVIEW_ID_WITHOUT_RATING)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("withoutRating"))
            review(REVIEW_ID_WITHOUT_TEXT)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("withoutText"))
        }
    }

    @Test
    fun workWithUserReviewOnSiteReviews() {
        configureWebServer {
            registerReviewsSuccess("siteReviewsEmpty.json")
            registerUserReviewSuccess("userReviewEmpty.json")
            registerAddUserReviewSuccess(
                "userReviewAdd.json",
                RATING_5,
                REVIEW_TEXT_1,
                true
            )
            registerUserReviewSuccess("userReviewAdded.json")
            registerEditUserReviewSuccess(
                "userReviewEdit.json",
                USER_REVIEW_ID,
                RATING_1,
                REVIEW_TEXT_2,
                false
            )
            registerUserReviewSuccess("userReviewEdited.json")
            registerDeleteUserReviewSuccess("userReviewDelete.json", USER_REVIEW_ID)
            registerUserReviewSuccess("userReviewEmpty.json")
        }

        reviewsActivityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SiteReviewsScreen> {
            rateSiteView.waitUntil { listView.contains(this) }
            rateSiteRatingView.setRating(RATING_4)
        }

        onScreen<RateSiteScreen> {
            ratingInputField.waitUntil { listView.contains(this) }
            isViewStateMatches(getTestRelatedFilePath("rateSiteInitial"))
            ratingInputView.setRating(RATING_5)
            textInputView.typeText(REVIEW_TEXT_1)
            anonymousField.click()
            isContentStateMatches(getTestRelatedFilePath("rateSiteFilled"))
            submitButton.click()
            successView.waitUntil { listView.contains(this) }
            isViewStateMatches(getTestRelatedFilePath("rateSiteSuccess"))
            okButton.click()
        }
        onScreen<SiteReviewsScreen> {
            userReviewView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("userReviewAdded"))
            userReviewMoreButton.click()
        }
        onScreen<UserReviewMenuDialogScreen> {
            editButton.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("userMenuDialog"))
            editButton.click()
        }
        onScreen<RateSiteScreen> {
            ratingInputField.waitUntil { listView.contains(this) }
            isContentStateMatches(getTestRelatedFilePath("rateSiteEditing"))
            ratingInputView.setRating(RATING_0)
            textInputView.clearText()
            textInputView.typeText(REVIEW_TEXT_2)
            anonymousField.click()
            isContentStateMatches(getTestRelatedFilePath("rateSiteEdited"))
            submitButton.click()
            successView.waitUntil { listView.contains(this) }
            okButton.click()
        }
        onScreen<SiteReviewsScreen> {
            userReviewView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("userReviewEdited"))
            userReviewMoreButton.click()
        }
        onScreen<UserReviewMenuDialogScreen> {
            deleteButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<ConfirmationDialogScreen> {
            root.isViewStateMatches(getTestRelatedFilePath("deleteConfirmation"))
            confirmButton.click()
        }
        onScreen<SiteReviewsScreen> {
            rateSiteView.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showUserReviewWithoutText() {
        configureWebServer {
            registerReviewsSuccess("siteReviewsEmpty.json")
            registerUserReviewSuccess("userReviewWithoutText.json")
        }
        authorizationRule.setUserAuthorized()
        reviewsActivityTestRule.launchActivity()
        onScreen<SiteReviewsScreen> {
            userReviewView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("userReview"))
        }
    }

    @Test
    fun showUserReviewWithoutRating() {
        configureWebServer {
            registerReviewsSuccess("siteReviewsEmpty.json")
            registerUserReviewSuccess("userReviewWithoutRating.json")
        }
        authorizationRule.setUserAuthorized()
        reviewsActivityTestRule.launchActivity()
        onScreen<SiteReviewsScreen> {
            userReviewView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("userReview"))
        }
    }

    @Test
    fun addUserReviewWithoutText() {
        configureWebServer {
            registerReviewsSuccess("siteReviewsEmpty.json")
            registerUserReviewSuccess("userReviewEmpty.json")
            registerAddUserReviewSuccess("userReviewAdd.json", RATING_5, "", false)
        }
        authorizationRule.setUserAuthorized()
        reviewsActivityTestRule.launchActivity()
        onScreen<SiteReviewsScreen> {
            rateSiteView.waitUntil { listView.contains(this) }
            rateSiteRatingView.setRating(RATING_5)
        }

        onScreen<RateSiteScreen> {
            ratingInputField.waitUntil { listView.contains(this) }
            closeKeyboard()
            submitButton.click()
            successView.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun addUserReviewErrors() {
        configureWebServer {
            registerReviewsSuccess("siteReviewsEmpty.json")
            registerUserReviewSuccess("userReviewEmpty.json")
            registerAddUserReviewSuccess("userReviewAddConflict.json", RATING_5)
            registerAddUserReviewError(RATING_5)
        }
        authorizationRule.setUserAuthorized()
        reviewsActivityTestRule.launchActivity()
        onScreen<SiteReviewsScreen> {
            rateSiteView.waitUntil { listView.contains(this) }
            rateSiteRatingView.setRating(RATING_5)
        }
        onScreen<RateSiteScreen> {
            submitButton.waitUntil { isCompletelyDisplayed() }.click()
            toastView(getResourceString(R.string.error_submit_review_conflict))
                .isCompletelyDisplayed()
            submitButton.click()
            toastView(getResourceString(R.string.error_submit_review))
                .waitUntil { isCompletelyDisplayed() }
            internetRule.turnOff()
            submitButton.click()
            toastView(getResourceString(R.string.error_network_message))
                .waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun editUserReviewErrors() {
        configureWebServer {
            registerReviewsSuccess("siteReviewsEmpty.json")
            registerUserReviewSuccess("userReviewAdded.json")
            registerEditUserReviewError(USER_REVIEW_ID)
        }

        authorizationRule.setUserAuthorized()
        reviewsActivityTestRule.launchActivity()

        onScreen<SiteReviewsScreen> {
            userReviewView.waitUntil { listView.contains(this) }
            userReviewMoreButton.click()
        }
        onScreen<UserReviewMenuDialogScreen> {
            editButton.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("userMenuDialog"))
            editButton.click()
        }
        onScreen<RateSiteScreen> {
            ratingInputField.waitUntil { listView.contains(this) }
            submitButton.click()
            toastView(getResourceString(R.string.error_submit_review))
                .waitUntil { isCompletelyDisplayed() }
            internetRule.turnOff()
            submitButton.click()
            toastView(getResourceString(R.string.error_network_message))
                .waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun deleteUserReviewErrorsOnSiteReviews() {
        configureWebServer {
            registerReviewsSuccess("siteReviewsEmpty.json")
            registerUserReviewSuccess("userReviewAdded.json")
            registerDeleteUserReviewError(USER_REVIEW_ID)
            registerDeleteUserReviewSuccess("userReviewDeleteNotFound.json", USER_REVIEW_ID)
            registerUserReviewSuccess("userReviewEmpty.json")
        }

        authorizationRule.setUserAuthorized()
        reviewsActivityTestRule.launchActivity()

        onScreen<SiteReviewsScreen> {
            userReviewView.waitUntil { listView.contains(this) }
            userReviewMoreButton.click()
        }
        onScreen<UserReviewMenuDialogScreen> {
            deleteButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<ConfirmationDialogScreen> {
            confirmButton.click()
        }
        onScreen<SiteReviewsScreen> {
            toastView(getResourceString(R.string.error_try_again))
                .waitUntil { isCompletelyDisplayed() }
        }

        onScreen<SiteReviewsScreen> {
            userReviewView.waitUntil { listView.contains(this) }
            userReviewMoreButton.click()
        }
        onScreen<UserReviewMenuDialogScreen> {
            deleteButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        internetRule.turnOff()
        onScreen<ConfirmationDialogScreen> {
            confirmButton.click()
        }
        onScreen<SiteReviewsScreen> {
            toastView(getResourceString(R.string.error_network_message))
                .waitUntil { isCompletelyDisplayed() }
        }
        internetRule.turnOn()

        onScreen<SiteReviewsScreen> {
            userReviewView.waitUntil { listView.contains(this) }
            userReviewMoreButton.click()
        }
        onScreen<UserReviewMenuDialogScreen> {
            deleteButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<ConfirmationDialogScreen> {
            confirmButton.click()
        }
        onScreen<SiteReviewsScreen> {
            rateSiteView.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showUserReviewErrorsInSiteReviews() {
        configureWebServer {
            registerReviewsError()
            registerUserReviewError()
            registerReviewsSuccess("siteReviewsEmpty.json")
            registerUserReviewError()
            registerUserReviewSuccess("userReviewEmpty.json")
        }

        reviewsActivityTestRule.launchActivity()
        authorizationRule.setUserAuthorized()

        onScreen<SiteReviewsScreen> {
            errorView.waitUntil { listView.contains(this) }
                .click()
            userReviewError.waitUntil { listView.contains(this) }
                .retryButton.click()
            rateSiteView.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun workWithUserReviewInSiteCard() {
        configureWebServer {
            registerSiteWithOfferStat()
            registerReviewsSuccess("siteReviewsEmpty.json")
            registerUserReviewSuccess("userReviewEmpty.json")
            registerAddUserReviewSuccess(
                "userReviewAdd.json",
                RATING_5,
                REVIEW_TEXT_1,
                true
            )
            registerUserReviewSuccess("userReviewAdded.json")
            registerEditUserReviewSuccess(
                "userReviewEdit.json",
                USER_REVIEW_ID,
                RATING_1,
                REVIEW_TEXT_2,
                false
            )
            registerUserReviewSuccess("userReviewEdited.json")
            registerDeleteUserReviewSuccess("userReviewDelete.json", USER_REVIEW_ID)
            registerUserReviewSuccess("userReviewEmpty.json")
        }

        siteCardActivityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            rateSiteView.waitUntil { listView.contains(this) }
            rateSiteRatingView.setRating(RATING_4)
        }
        onScreen<RateSiteScreen> {
            ratingInputField.waitUntil { listView.contains(this) }
            isViewStateMatches(getTestRelatedFilePath("rateSiteInitial"))
            ratingInputView.setRating(RATING_5)
            textInputView.typeText(REVIEW_TEXT_1)
            anonymousField.click()
            isContentStateMatches(getTestRelatedFilePath("rateSiteFilled"))
            submitButton.click()
            successView.waitUntil { listView.contains(this) }
            isViewStateMatches(getTestRelatedFilePath("rateSiteSuccess"))
            okButton.click()
        }
        onScreen<SiteCardScreen> {
            userReviewView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("userReviewAdded"))
            userReviewMoreButton.click()
        }
        onScreen<UserReviewMenuDialogScreen> {
            editButton.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("userMenuDialog"))
            editButton.click()
        }
        onScreen<RateSiteScreen> {
            ratingInputField.waitUntil { listView.contains(this) }
            isContentStateMatches(getTestRelatedFilePath("rateSiteEditing"))
            ratingInputView.setRating(RATING_0)
            textInputView.clearText()
            textInputView.typeText(REVIEW_TEXT_2)
            anonymousField.click()
            isContentStateMatches(getTestRelatedFilePath("rateSiteEdited"))
            submitButton.click()
            successView.waitUntil { listView.contains(this) }
            okButton.click()
        }
        onScreen<SiteCardScreen> {
            userReviewView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("userReviewEdited"))
            userReviewMoreButton.click()
        }
        onScreen<UserReviewMenuDialogScreen> {
            deleteButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<ConfirmationDialogScreen> {
            root.isViewStateMatches(getTestRelatedFilePath("deleteConfirmation"))
            confirmButton.click()
        }
        onScreen<SiteCardScreen> {
            rateSiteView.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun deleteUserReviewErrorsOnSiteCard() {
        configureWebServer {
            registerSiteWithOfferStat()
            registerReviewsSuccess("siteReviewsEmpty.json")
            registerUserReviewSuccess("userReviewAdded.json")
            registerDeleteUserReviewError(USER_REVIEW_ID)
            registerDeleteUserReviewSuccess("userReviewDeleteNotFound.json", USER_REVIEW_ID)
            registerUserReviewSuccess("userReviewEmpty.json")
        }

        authorizationRule.setUserAuthorized()
        siteCardActivityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            userReviewView.waitUntil { listView.contains(this) }
            userReviewMoreButton.click()
        }
        onScreen<UserReviewMenuDialogScreen> {
            deleteButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<ConfirmationDialogScreen> {
            confirmButton.click()
        }
        onScreen<SiteCardScreen> {
            toastView(getResourceString(R.string.error_try_again))
                .waitUntil { isCompletelyDisplayed() }
            userReviewView.waitUntil { listView.contains(this) }
            userReviewMoreButton.click()
        }
        onScreen<UserReviewMenuDialogScreen> {
            deleteButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        internetRule.turnOff()
        onScreen<ConfirmationDialogScreen> {
            confirmButton.click()
        }
        onScreen<SiteCardScreen> {
            toastView(getResourceString(R.string.error_network_message))
                .waitUntil { isCompletelyDisplayed() }
            internetRule.turnOn()
            userReviewView.waitUntil { listView.contains(this) }
            userReviewMoreButton.click()
        }
        onScreen<UserReviewMenuDialogScreen> {
            deleteButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<ConfirmationDialogScreen> {
            confirmButton.click()
        }
        onScreen<SiteCardScreen> {
            rateSiteView.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun showUserReviewErrorsInSiteCard() {
        configureWebServer {
            registerSiteWithOfferStat()
            registerReviewsError()
            registerUserReviewError()
            registerReviewsSuccess("siteReviewsEmpty.json")
            registerUserReviewError()
            registerUserReviewSuccess("userReviewEmpty.json")
        }

        siteCardActivityTestRule.launchActivity()
        authorizationRule.setUserAuthorized()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            reviewsError.waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .retryButton.click()
            userReviewError.waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .retryButton.click()
            rateSiteView.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun sortNewOnReviewsScreen() {
        testSorting({ newView }, SortOrder.BY_TIME)
    }

    @Test
    fun sortRelevanceOnReviewsScreen() {
        testSorting({ relevanceView }, SortOrder.BY_RELEVANCE_ORG)
    }

    @Test
    fun sortRatingAscOnReviewsScreen() {
        testSorting({ ratingAscView }, SortOrder.BY_RATING_ASC)
    }

    @Test
    fun sortRatingDescOnReviewsScreen() {
        testSorting({ ratingDescView }, SortOrder.BY_RATING_DESC)
    }

    private fun testSorting(
        sortingView: SiteReviewsSortingDialogScreen.() -> TView,
        sorting: SortOrder
    ) {
        configureWebServer {
            registerReviewsSuccess("siteReviews20.json", SortOrder.BY_RELEVANCE_ORG)
            registerReviewsSuccess("siteReviews20Page2.json", sorting)
        }

        reviewsActivityTestRule.launchActivity()

        onScreen<SiteReviewsScreen> {
            review(REVIEW_ID_FIRST).waitUntil { listView.contains(this) }
            listView.scrollTo(sortingButton)
                .click()
            onScreen<SiteReviewsSortingDialogScreen> {
                sortingView
                    .invoke(this)
                    .waitUntil { isCompletelyDisplayed() }
                    .click()
            }
            review(REVIEW_ID_LAST_SECOND_PAGE).waitUntil { listView.contains(this) }
        }
    }

    private fun DispatcherRegistry.registerReviewsSuccess(
        responseFile: String,
        sorting: SortOrder? = null
    ) {
        registerReviews(response { assetBody("SiteReviewsTest/$responseFile") }, sorting)
    }

    private fun DispatcherRegistry.registerReviewsError() {
        registerReviews(error(), null)
    }

    private fun DispatcherRegistry.registerReviews(
        response: MockResponse,
        sorting: SortOrder?
    ) {
        register(
            request {
                method("POST")
                path("2.0/graphql")
                jsonPartialBody {
                    "operationName" to "GetReviews"
                    "variables" to JsonObject().apply {
                        addProperty("id", SITE_PERMALINK)
                        sorting?.let { addProperty("sort", sorting.rawValue) }
                    }
                }
            },
            response
        )
    }

    private fun DispatcherRegistry.registerUserReviewSuccess(responseFile: String) {
        registerUserReview(response { assetBody("SiteReviewsTest/$responseFile") })
    }

    private fun DispatcherRegistry.registerUserReviewError() {
        registerUserReview(error())
    }

    private fun DispatcherRegistry.registerUserReview(response: MockResponse) {
        register(
            request {
                method("POST")
                path("2.0/graphql")
                jsonPartialBody {
                    "operationName" to "GetMyReview"
                    "variables" to JsonObject().apply {
                        addProperty("id", SITE_PERMALINK)
                    }
                }
            },
            response
        )
    }

    private fun DispatcherRegistry.registerAddUserReviewSuccess(
        responseFile: String,
        rating: Int,
        text: String? = null,
        isAnonymous: Boolean? = null,
    ) {
        registerAddUserReview(
            rating,
            text,
            isAnonymous,
            response { assetBody("SiteReviewsTest/$responseFile") }
        )
    }

    private fun DispatcherRegistry.registerAddUserReviewError(
        rating: Int,
        text: String? = null,
        isAnonymous: Boolean? = null
    ) {
        registerAddUserReview(rating, text, isAnonymous, error())
    }

    private fun DispatcherRegistry.registerAddUserReview(
        rating: Int,
        text: String?,
        isAnonymous: Boolean?,
        response: MockResponse
    ) {
        register(
            request {
                method("POST")
                path("2.0/graphql")
                jsonPartialBody {
                    "operationName" to "AddReview"
                    "variables" to JsonObject().apply {
                        addProperty("id", SITE_PERMALINK)
                        addProperty("rating", rating)
                        text?.let { addProperty("text", it) }
                        isAnonymous?.let { addProperty("isAnonymous", it) }
                    }
                }
            },
            response
        )
    }

    private fun DispatcherRegistry.registerEditUserReviewSuccess(
        responseFile: String,
        reviewId: String,
        rating: Int,
        text: String,
        isAnonymous: Boolean
    ) {
        registerEditUserReview(
            reviewId,
            rating,
            text,
            isAnonymous,
            response { assetBody("SiteReviewsTest/$responseFile") }
        )
    }

    private fun DispatcherRegistry.registerEditUserReviewError(
        reviewId: String,
        rating: Int? = null,
        text: String? = null,
        isAnonymous: Boolean? = null,
    ) {
        registerEditUserReview(reviewId, rating, text, isAnonymous, error())
    }

    private fun DispatcherRegistry.registerEditUserReview(
        reviewId: String,
        rating: Int?,
        text: String?,
        isAnonymous: Boolean?,
        response: MockResponse
    ) {
        register(
            request {
                method("POST")
                path("2.0/graphql")
                jsonPartialBody {
                    "operationName" to "EditReview"
                    "variables" to JsonObject().apply {
                        addProperty("id", SITE_PERMALINK)
                        addProperty("reviewId", reviewId)
                        addProperty("rating", rating)
                        addProperty("text", text)
                        addProperty("isAnonymous", isAnonymous)
                    }
                }
            },
            response
        )
    }

    private fun DispatcherRegistry.registerDeleteUserReviewSuccess(
        responseFile: String,
        reviewId: String
    ) {
        registerDeleteUserReview(reviewId, response { assetBody("SiteReviewsTest/$responseFile") })
    }

    private fun DispatcherRegistry.registerDeleteUserReviewError(reviewId: String) {
        registerDeleteUserReview(reviewId, error())
    }

    private fun DispatcherRegistry.registerDeleteUserReview(
        reviewId: String,
        response: MockResponse
    ) {
        register(
            request {
                method("POST")
                path("2.0/graphql")
                jsonPartialBody {
                    "operationName" to "DeleteReview"
                    "variables" to JsonObject().apply {
                        addProperty("id", SITE_PERMALINK)
                        addProperty("reviewId", reviewId)
                    }
                }
            },
            response
        )
    }

    private fun DispatcherRegistry.registerSiteWithOfferStat() {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("SiteReviewsTest/siteWithOfferStat.json")
            }
        )
    }

    companion object {

        private const val SITE_ID = "1"
        private const val REVIEW_ID_FIRST = "B16Urs9-EwxdAtunIhYduI7Eqx6h1Pkb"
        private const val REVIEW_ID_LAST_FIRST_PAGE = "KIKDvwSKYu1n8GuMemTuOnZzbstgO--vg"
        private const val REVIEW_ID_LAST_SECOND_PAGE = "KIKDvwSKYu1n8GuMemTuOnZzbstgO--v1"
        private const val REVIEW_ID_WITH_EXPAND = "oq_lniE3sjEH9MjiKy3hV5klIPcHw98"
        private const val REVIEW_ID_WITHOUT_RATING = "FUxRX9X98rT6O07cFU-I0UsQOHG988"
        private const val REVIEW_ID_WITHOUT_TEXT = "0FzmC1qOg_x2FQBRdK2zEHTPHsBQ2Yx"

        private const val USER_REVIEW_ID = "je_17nWXuogdj_f_Mq36zu_cOZS08z"
        private const val RATING_5 = 5
        private const val RATING_4 = 4
        private const val RATING_1 = 1
        private const val RATING_0 = 0
        private const val REVIEW_TEXT_1 = "site review"
        private const val REVIEW_TEXT_2 = "new review"
        private const val SITE_PERMALINK = "182242396448"
        private const val SITE_NAME = "Name"
    }
}
