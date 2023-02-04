package ru.auto.ara.test.reviews

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.reviews.GetUserReviewsDispatcher
import ru.auto.ara.core.dispatchers.reviews.createReviewFromOffer
import ru.auto.ara.core.dispatchers.reviews.getReferenceCatalogSuggest
import ru.auto.ara.core.dispatchers.reviews.getReview
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.reviews.checkReviewDraft
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity


@RunWith(AndroidJUnit4::class)
class ReviewDeeplinksUnauthorizedTest {

    private val OFFER_ID = "1092899026-6e13a2e5"
    private val REVIEW_DRAFT_ID = "1347793407683052901"
    private val TEST_PHONE = "70000000000"
    private val CODE = "0000"


    private val webServerRule = WebServerRule{
        createReviewFromOffer(
            category = "cars",
            offerId = OFFER_ID,
            reviewId = REVIEW_DRAFT_ID
        )
        delegateDispatcher(GetUserReviewsDispatcher.empty())
        userSetup()
        postLoginOrRegisterSuccess()
        getReview(reviewId = REVIEW_DRAFT_ID, type = "draft")
        getReferenceCatalogSuggest("bmw_5er")

    }
    private var activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var rules = baseRuleChain(
        webServerRule,
        GrantPermissionsRule(),
        SetPreferencesRule()
    )

    @Test
    fun shouldOpenFilledCarsReviewDraftAfterLogin() {
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/reviews/add/$OFFER_ID/")
        performLogin {
            waitLogin()
            input(TEST_PHONE)
            waitCode()
            input(CODE)
        }
        checkReviewDraft {
            checkReviewDraftShown()
            checkDraftParameterShown("Марка", "BMW")
            checkDraftParameterShown("Модель", "5 серия")
            checkDraftParameterShown("Год выпуска", "2016")
            checkDraftParameterShown("Поколение", "VI (F10/F11/F07) Рестайлинг")
            checkDraftParameterShown("Кузов", "Седан")
            checkDraftParameterShown("Двигатель", "Бензин")
            checkDraftParameterShown("Привод", "Задний")
            checkDraftParameterShown("Коробка передач", "Механическая")
            checkDraftParameterShown("Модификация", "528i 2.0 MT Бензин (245 л.с.)")
        }

    }

}
