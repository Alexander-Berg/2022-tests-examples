package ru.auto.ara.test.reviews

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.reviews.GetReviewsListingDispatcher
import ru.auto.ara.core.dispatchers.reviews.GetUserReviewsDispatcher
import ru.auto.ara.core.dispatchers.reviews.createReviewFromOffer
import ru.auto.ara.core.dispatchers.reviews.getReferenceCatalogSuggest
import ru.auto.ara.core.dispatchers.reviews.getReview
import ru.auto.ara.core.dispatchers.reviews.getReviewsCounter
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.reviews.ReviewsFeedTabRobotChecker
import ru.auto.ara.core.robot.reviews.checkReview
import ru.auto.ara.core.robot.reviews.checkReviewDraft
import ru.auto.ara.core.robot.reviews.checkReviewFeedTab
import ru.auto.ara.core.robot.reviews.checkReviewsFeed
import ru.auto.ara.core.robot.reviews.performReviewsFeed
import ru.auto.ara.core.robot.transporttab.checkMain
import ru.auto.ara.core.robot.userreviews.checkUserReviewsFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class ReviewDeeplinksTest {
    private val OFFER_ID = "1092899026-6e13a2e5"
    private val OWNING_PERIOD_OFFER_ID = "1097209538-4f3f7bb8"
    private val REVIEW_CARS_DRAFT_ID = "1347793407683052901"
    private val REVIEW_CARS_WITHOUT_OWNING_PERIOD_DRAFT_ID = "2839897610552057722"
    private val REVIEW_MOTO_DRAFT_ID = "1547701473447221963"
    private val REVIEW_CARS_CARD_ID = "4469251119274136739"
    private val REVIEW_MOTO_CARD_ID = "2848775801800201126"
    private val fillByOfferCarsUri = "https://auto.ru/cars/reviews/add/$OFFER_ID/"
    private val fillByOfferWithOwnintPeriodCarsUri = "https://auto.ru/cars/reviews/add/$OWNING_PERIOD_OFFER_ID/"
    private val fillByOfferMotoUri = "https://auto.ru/moto/reviews/add/$OFFER_ID/"

    private val reviewsListRequestWatcher = RequestWatcher()

    private val webServerRule = WebServerRule {
        userSetup()
        getReviewsCounter("moto")
        getReviewsCounter("trucks")
        getReviewsCounter("cars")
        getReview(reviewId = REVIEW_CARS_DRAFT_ID, type = "draft")
        getReview(reviewId = REVIEW_MOTO_DRAFT_ID, type = "draft")
        getReview(reviewId = REVIEW_CARS_WITHOUT_OWNING_PERIOD_DRAFT_ID, type = "draft")
        getReview(reviewId = REVIEW_CARS_CARD_ID, type = "card")
        getReview(reviewId = REVIEW_MOTO_CARD_ID, type = "card")
        delegateDispatchers(
            GetUserReviewsDispatcher.empty(),
            GetReviewsListingDispatcher.generic(reviewsListRequestWatcher)
        )
    }
    private var activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var rules = baseRuleChain(
        webServerRule,
        SetupAuthRule(),
        GrantPermissionsRule(),
        SetPreferencesRule()
    )

    @Test
    fun shouldOpenFilledCarsReviewDraft() {
        webServerRule.routing {
            createReviewFromOffer(
                category = "cars",
                offerId = OFFER_ID,
                reviewId = REVIEW_CARS_DRAFT_ID
            )
            getReferenceCatalogSuggest("bmw_5er")
        }

        activityRule.launchDeepLinkActivity(fillByOfferCarsUri)
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
            checkDraftParameterShown("Срок владения", "Менее 6 месяцев")
        }
    }

    @Test
    fun shouldOpenFilledCarsReviewDraftWithoutOwningPeriod() {
        webServerRule.routing {
            createReviewFromOffer(
                category = "cars",
                offerId = OWNING_PERIOD_OFFER_ID,
                reviewId = REVIEW_CARS_WITHOUT_OWNING_PERIOD_DRAFT_ID
            )
            getReferenceCatalogSuggest("skoda_oktavia")
        }
        activityRule.launchDeepLinkActivity(fillByOfferWithOwnintPeriodCarsUri)
        checkReviewDraft {
            checkReviewDraftShown()
            checkDraftParameterShown("Марка", "Skoda")
            checkDraftParameterShown("Модель", "Octavia")
            checkDraftParameterShown("Год выпуска", "2007")
            checkDraftParameterShown("Поколение", "II II II")
            checkDraftParameterShown("Кузов", "Лифтбек")
            checkDraftParameterShown("Двигатель", "Бензин")
            checkDraftParameterShown("Привод", "Передний")
            checkDraftParameterShown("Коробка передач", "Автомат")
            checkDraftParameterShown("Модификация", "2.0 AT Бензин (150 л.с.)")
            checkDraftParameterNotFilled("Срок владения")
        }
    }

    @Test
    fun shouldOpenFilledMotoReviewDraft() {
        webServerRule.routing {
            createReviewFromOffer(
                category = "moto",
                offerId = OFFER_ID,
                reviewId = REVIEW_MOTO_DRAFT_ID
            )
        }
        activityRule.launchDeepLinkActivity(fillByOfferMotoUri)
        checkReviewDraft {
            checkReviewDraftShown()
            checkDraftParameterShown("Категория", "Снегоходы")
            checkDraftParameterShown("Марка", "Polaris")
            checkDraftParameterShown("Модель", "550 INDY")
            checkDraftParameterShown("Год выпуска", "2014")
        }
    }

    @Test
    fun shouldOpenEmptyReviewDraft() {
        webServerRule.routing {
            createReviewFromOffer(
                category = "cars",
                offerId = OFFER_ID,
                reviewId = null
            )
        }
        activityRule.launchDeepLinkActivity(fillByOfferCarsUri)
        checkReviewDraft { checkIsCarsDraftNotFilled() }
    }

    @Test
    fun shouldOpenNewReviewDraft() {
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/reviews/add/")
        checkReviewDraft { checkIsCarsDraftNotFilled() }
    }

    @Test
    fun shouldOpenMyReviewsList() {
        activityRule.launchDeepLinkActivity("https://auto.ru/my/reviews/")
        checkUserReviewsFeed { isTitleDisplayed() }
    }

    @Test
    fun shouldOpenReviewsTabWithMotoCategory() {
        testForReviewTab("https://media.auto.ru/reviews/moto") {
            checkCategorySegmentSelected(R.string.category_moto)
        }
        reviewsListRequestWatcher.checkQueryParameters(
            "category" to "MOTO",
            "sub_category" to "motorcycle"
        )
        reviewsListRequestWatcher.checkNotQueryParameters("mark", "model", "super_gen")
    }

    @Test
    fun shouldOpenReviewsTabWithTrucksCategory() {
        testForReviewTab("https://media.auto.ru/reviews/trucks") {
            checkCategorySegmentSelected(R.string.category_comm)
        }
        reviewsListRequestWatcher.checkQueryParameters(
            "category" to "TRUCKS",
            "sub_category" to "lcv"
        )
        reviewsListRequestWatcher.checkNotQueryParameters("mark", "model", "super_gen")
    }

    @Test
    fun shouldOpenReviewsTabWithDefaultCategory() {
        testForReviewTab("https://media.auto.ru/reviews") {
            checkCategorySegmentSelected(R.string.category_auto)
        }
        reviewsListRequestWatcher.checkQueryParameters("category" to "CARS")
        reviewsListRequestWatcher.checkNotQueryParameters("sub_category", "mark", "model", "super_gen")
    }

    @Test
    fun shouldOpenReviewsFeedByMMG() {
        activityRule.launchDeepLinkActivity("https://media.auto.ru/reviews/cars/vaz/2103/6257051/")
        checkReviewsFeed { isReviewFeed() }
        waitFirstPage()
        reviewsListRequestWatcher.checkQueryParameters(
            "category" to "CARS",
            "mark" to "vaz",
            "model" to "2103",
            "super_gen" to "6257051"
        )
        reviewsListRequestWatcher.checkNotQueryParameter("sub_category")
    }

    @Test
    fun shouldOpenReviewsFeedByMark() {
        activityRule.launchDeepLinkActivity("https://media.auto.ru/reviews/cars/vaz")
        checkReviewsFeed { isReviewFeed() }
        waitFirstPage()
        reviewsListRequestWatcher.checkQueryParameters(
            "category" to "CARS",
            "mark" to "vaz"
        )
        reviewsListRequestWatcher.checkNotQueryParameters("sub_category", "model", "super_gen")
    }

    @Test
    fun shouldOpenMotoReviewsFeedBySubCategory() {
        activityRule.launchDeepLinkActivity("https://media.auto.ru/reviews/moto/snowmobile")
        checkReviewsFeed { isReviewFeed() }
        waitFirstPage()
        reviewsListRequestWatcher.checkQueryParameters(
            "category" to "MOTO",
            "sub_category" to "snowmobile"
        )
        reviewsListRequestWatcher.checkNotQueryParameters("mark", "model", "super_gen")
    }

    @Test
    fun shouldOpenTrucksReviewsFeedByMark() {
        activityRule.launchDeepLinkActivity("https://media.auto.ru/reviews/trucks/trailer/tonar")
        checkReviewsFeed { isReviewFeed() }
        waitFirstPage()
        reviewsListRequestWatcher.checkQueryParameters(
            "category" to "TRUCKS",
            "sub_category" to "trailer",
            "mark" to "tonar"
        )
        reviewsListRequestWatcher.checkNotQueryParameters("model", "super_gen")
    }

    @Test
    fun shouldOpenTrucksReviewsFeedByMarkModel() {
        activityRule.launchDeepLinkActivity("https://media.auto.ru/reviews/trucks/bus/ford/transit/")
        checkReviewsFeed { isReviewFeed() }
        waitFirstPage()
        reviewsListRequestWatcher.checkQueryParameters(
            "category" to "TRUCKS",
            "sub_category" to "bus",
            "mark" to "ford",
            "model" to "transit"
        )
        reviewsListRequestWatcher.checkNotQueryParameter("super_gen")
    }

    @Test
    fun shouldOpenReviewCardCars() {
        activityRule.launchDeepLinkActivity("https://media.auto.ru/review/cars/vaz/2103/6257051/$REVIEW_CARS_CARD_ID/")
        checkReview { isReviewToolbarDisplayed() }
    }

    @Test
    fun shouldOpenReviewCardMoto() {
        activityRule.launchDeepLinkActivity("https://media.auto.ru/review/moto/snowmobile/yamaha/venture/$REVIEW_MOTO_CARD_ID")
        checkReview { isReviewToolbarDisplayed() }
    }

    private fun waitFirstPage() {
        performReviewsFeed { waitFirstPage(20, 3) }
    }

    private fun testForReviewTab(url: String, checkAction: ReviewsFeedTabRobotChecker.() -> Unit) {
        activityRule.launchDeepLinkActivity(url)
        checkMain { isMainTabSelected(R.string.reviews) }
        checkReviewFeedTab {
            isReviewFeed()
            checkAction()
        }
    }

}
