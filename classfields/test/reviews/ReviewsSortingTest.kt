package ru.auto.ara.test.reviews

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.reviews.GetReviewsListingDispatcher
import ru.auto.ara.core.dispatchers.reviews.getReviewsCounter
import ru.auto.ara.core.interaction.transporttab.MainInteractions
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.reviews.checkReviewFeedTab
import ru.auto.ara.core.robot.reviews.checkReviewsFeed
import ru.auto.ara.core.robot.reviews.performReviewFeedTab
import ru.auto.ara.core.robot.reviews.performReviewsFeed
import ru.auto.ara.core.robot.reviews.performScreenshotReviewListing
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.core.utils.waitSomething
import ru.auto.data.model.review.ReviewSort
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ReviewsSortingTest {

    private val reviewsListRequestWatcher = RequestWatcher()
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    private val webServerRule = WebServerRule {
        delegateDispatcher( GetReviewsListingDispatcher.generic(reviewsListRequestWatcher))
        getReviewsCounter("CARS")
    }
    private val timeRule = SetupTimeRule()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        timeRule
    )

    @Before
    fun setUp() {
        timeRule.setTime(time = "00:00")
        activityRule.launchActivity()

        performMain { openMainTab(R.string.reviews) }
        MainInteractions.collapseMainToolbar()
    }

    @Test
    fun shouldChangeSorting() {
        val defaultSort = ReviewSort.RELEVANCE_DESC
        val lastSort = ReviewSort.values().toList().last()
        val sortPairs = ReviewSort.values().toList().zipWithNext() + listOf(Pair(lastSort, defaultSort))

        performReviewFeedTab {
            clickOnMMGItem()
            clickOnShowButton()
        }

        reviewsListRequestWatcher.checkQueryParameters("sort" to defaultSort.param)
        checkReviewsFeed { checkCountItem("50 663 отзыва") }

        sortPairs.forEachIndexed { index, (currentSort, nextSort) ->
            performReviewsFeed { clickOnSortItemUntilPickerListLabelDisplayed(currentSort.label, currentSort.pickerListLabel) }
            checkReviewFeedTab { isSortingSelected(currentSort.pickerListLabel) }
            performReviewFeedTab { selectSorting(nextSort.pickerListLabel) }
            checkReviewsFeed { isSortingTitle(nextSort.label) }
            reviewsListRequestWatcher.checkQueryParameters("sort" to nextSort.param)
            timeRule.setTime(time = "00:0${index + 1}")
        }
    }

    @Test
    fun shouldSaveSortBetweenTabAndListing() {
        //change sort on reviews tab to sort1
        performReviewFeedTab {
            scrollToSortItem(reviewsCount = "30 отзывов", sortText = ReviewSort.RELEVANCE_DESC.label)
            clickOnSortItem(ReviewSort.RELEVANCE_DESC.label)
            selectSorting(ReviewSort.LIKE_DESC.pickerListLabel)
    }
        reviewsListRequestWatcher.checkQueryParameters("sort" to ReviewSort.LIKE_DESC.param)
        waitSomething(1, TimeUnit.SECONDS)
        timeRule.setTime(time = "00:01")

        //open reviews listing and change sort1 to sort2
        performReviewFeedTab {
            clickOnMMGItem()
            clickOnShowButton()
        }
        performScreenshotReviewListing { clickOnSelectedSort(ReviewSort.LIKE_DESC.label) }
        waitSomething(1, TimeUnit.SECONDS)
        timeRule.setTime(time = "00:02")
        performReviewFeedTab { selectSorting(ReviewSort.RATING_ASC.pickerListLabel) }
        reviewsListRequestWatcher.checkQueryParameters("sort" to ReviewSort.RATING_ASC.param)

        //go back and check sort2 displayed
        performCommon { pressBack() }
        checkReviewFeedTab { isSortingTitle(ReviewSort.RATING_ASC.label) }
        reviewsListRequestWatcher.checkQueryParameters("sort" to ReviewSort.RATING_ASC.param)
    }

}
