package ru.auto.ara.test.reviews

import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.reviews.GetReviewsListingDispatcher
import ru.auto.ara.core.dispatchers.reviews.GetTopCommentedReviewsDispatcher
import ru.auto.ara.core.dispatchers.reviews.GetTopLikedReviewsDispatcher
import ru.auto.ara.core.dispatchers.reviews.GetTopOfThWeekReviewDispatcher
import ru.auto.ara.core.dispatchers.reviews.getReviewsCounter
import ru.auto.ara.core.dispatchers.reviews.getReviewsListing
import ru.auto.ara.core.interaction.transporttab.MainInteractions
import ru.auto.ara.core.robot.reviews.checkReviewFeedTab
import ru.auto.ara.core.robot.reviews.performReviewFeedTab
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.data.model.review.ReviewSort

@RunWith(AndroidJUnit4::class)
class ReviewsTest {

    private val reviewsListRequestWatcher = RequestWatcher()

    private val topOfTheWeekReviewHolder = DispatcherHolder()
    private val topLikedReviewHolder = DispatcherHolder()
    private val topCommentedReviewHolder = DispatcherHolder()

    private val topOfTheWeekRequestWatcher = RequestWatcher()
    private val topLikedRequestWatcher = RequestWatcher()
    private val topCommentedRequestWatcher = RequestWatcher()

    private val activityRule = ActivityTestRule(MainActivity::class.java, false, false)
    private val webServerRule = WebServerRule {
        getReviewsCounter("CARS")
        delegateDispatchers(
            GetReviewsListingDispatcher.generic(reviewsListRequestWatcher),
            topOfTheWeekReviewHolder,
            topLikedReviewHolder,
            topCommentedReviewHolder
        )
    }

    private val timeRule = SetupTimeRule()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule(),
        DisableAdsRule(),
        timeRule
    )

    @Before
    fun setup() {
        timeRule.setTime(time = "00:00")
        activityRule.launchActivity(null)
        setSpecialReviewDispatchers(CATEGORY_AUTO)
        performMain { openMainTab(R.string.reviews) }
        MainInteractions.collapseMainToolbar()
    }

    @Test
    fun shouldCorrectRequestAndScreenStateAfterChangeCategory() {
        changeCategoryAndCheckRequests(categoryName = R.string.category_moto, category = CATEGORY_MOTO)
        checkReviewFeedTab {
            checkPresetsDisplayed("Мотоциклы")
            checkMarkModelItemNotExists()
        }
        timeRule.setTime(time = "00:01")
        changeCategoryAndCheckRequests(categoryName = R.string.category_comm, category = CATEGORY_TRUCKS)
        checkReviewFeedTab {
            checkPresetsDisplayed("Лёгкий коммерческий")
            checkMarkModelItemNotExists()
        }
        timeRule.setTime(time = "00:02")
        changeCategoryAndCheckRequests(categoryName = R.string.category_auto, category = CATEGORY_AUTO)
        checkReviewFeedTab { checkMarkModelItemDisplayed() }
    }

    @Test
    fun shouldDisplayCorrectSpecialReviewsBlock() {
        checkSpecialReviewItem(
            badgeName = TOP_OF_THE_WEEK,
            badgeColor = R.attr.colorSecondary,
            title = "Уверенная проходимость",
            subtitle = "Chevrolet Niva I Рестайлинг",
            rating = 4.0f
        )
        checkSpecialReviewItem(
            badgeName = TOP_COMMENTED,
            badgeColor = R.attr.colorPrimary,
            title = "Впервые в России",
            subtitle = "Changan CS35PLUS",
            rating = 5.0f
        )
        checkSpecialReviewItem(
            badgeName = TOP_LIKED,
            badgeColor = R.attr.colorPrimaryAnalogous,
            title = "Технологическое совершенство и не высочайшим уровнем ...",
            subtitle = "Audi Q7 II",
            rating = 0.0f
        )
    }

    @Test
    fun shouldCorrectSortStateAndRequestAfterChangeSort() {
        val defaultSort = ReviewSort.RELEVANCE_DESC
        val lastSort = ReviewSort.values().toList().last()
        val sortPairs = ReviewSort.values().toList().zipWithNext() + listOf(Pair(lastSort, defaultSort))

        reviewsListRequestWatcher.checkQueryParameters("sort" to defaultSort.param)
        performReviewFeedTab { scrollToSortItem(reviewsCount = "30 отзывов", sortText = defaultSort.label) }

        sortPairs.forEachIndexed { index, (currentSort, nextSort) ->
            performReviewFeedTab { clickOnSortItem(currentSort.label) }
            checkReviewFeedTab { isSortingSelected(currentSort.pickerListLabel) }
            performReviewFeedTab { selectSorting(nextSort.pickerListLabel) }
            checkReviewFeedTab {
                isSortingDialogDisappeared()
                isSortingTitle(nextSort.label)
            }
            reviewsListRequestWatcher.checkQueryParameters("sort" to nextSort.param)
            timeRule.setTime(time = "00:0${index + 1}")
        }
    }

    @Test
    fun shouldCorrectDisplayReviewItemWithoutImage() {
        val title = "БМВ - бензиновый"
        performReviewFeedTab { scrollToReviewItem(title) }
        checkReviewFeedTab {
            checkReviewItem(
                title = title,
                subtitle = "BMW X3 II (F25)",
                message = "В гараже стоят два х3 f25 - 2.0D и 3.5 бензин." +
                    " Могу сравнивать каждый день и почему-то бензиновая намного лучше во всём." +
                    " Бензиновый - это настоящий бмв,а дизель тарахтит и из-за этого опускается на ур",
                rating = "5.0",
                updateDate = "18 сентября 2019",
                commentsCount = "45"
            )
            checkReviewItemImageIsGone(title = title)
        }
    }

    @Test
    fun shouldCorrectDisplayReviewItemWithImage() {
        val title = "Технологическое совершенство и не высочайшим уровнем комфорта. "
        performReviewFeedTab { scrollToReviewItem(title) }
        checkReviewFeedTab {
            checkReviewItem(
                title = title,
                subtitle = "Audi Q7 II",
                message = "Автомобиль имеет отличные ездовые и динамические характеристики." +
                    " Рулится как легковой, габариты чувствуются прекрасно." +
                    " Радиус разворота меньше чем у Туарег NF - 2,5 узких московских ряда запросто. ",
                rating = "3.0",
                updateDate = "Обновлён 20 сентября 2019",
                commentsCount = "99"
            )
            checkReviewItemImageIsDisplayed(title)
        }
    }

    @Test
    fun shouldLoadSecondPageInReviewListing() {
        val title = "Отзыв владельца про Audi A4 2012"
        webServerRule.routing { getReviewsListing(page = 2).watch { checkRequestWasCalled() } }
        performReviewFeedTab {
            waitLoadingIsHidden()
            scrollToReviewListingBottom()
            waitSecondPageLoaded(30)
            scrollToReviewItem(title)
        }.checkResult {
            checkReviewItemImageIsDisplayed(title)
        }
    }

    private fun checkSpecialReviewItem(
        badgeName: String,
        @AttrRes badgeColor: Int,
        title: String,
        subtitle: String,
        rating: Float
    ) {
        performReviewFeedTab { scrollToSpecialReviewItemOnPosition(badgeName) }
        checkReviewFeedTab { checkSpecialReviewItem(badgeName, badgeColor, title, subtitle, rating) }
    }

    private fun changeCategoryAndCheckRequests(@StringRes categoryName: Int, category: String) {
        setSpecialReviewDispatchers(category)
        performReviewFeedTab { clickOnCategorySegment(categoryName) }

        reviewsListRequestWatcher.checkQueryParameters(CATEGORY_KEY to category)
        topOfTheWeekRequestWatcher.checkRequestWasCalled()
        topOfTheWeekRequestWatcher.clearRequestWatcher()
        topLikedRequestWatcher.checkRequestWasCalled()
        topLikedRequestWatcher.clearRequestWatcher()
        topCommentedRequestWatcher.checkRequestWasCalled()
        topCommentedRequestWatcher.clearRequestWatcher()
    }

    private fun setSpecialReviewDispatchers(category: String) {
        topOfTheWeekReviewHolder.innerDispatcher = GetTopOfThWeekReviewDispatcher.oneReview(category, topOfTheWeekRequestWatcher)
        topLikedReviewHolder.innerDispatcher = GetTopLikedReviewsDispatcher.oneReview(category, topLikedRequestWatcher)
        topCommentedReviewHolder.innerDispatcher =
            GetTopCommentedReviewsDispatcher.oneReview(category, topCommentedRequestWatcher)
    }

    companion object {
        private const val CATEGORY_KEY = "category"
        private const val CATEGORY_AUTO = "CARS"
        private const val CATEGORY_MOTO = "MOTO"
        private const val CATEGORY_TRUCKS = "TRUCKS"

        private const val TOP_OF_THE_WEEK = "Отзыв недели"
        private const val TOP_COMMENTED = "Обсуждаемый"
        private const val TOP_LIKED = "Популярный"
    }
}
