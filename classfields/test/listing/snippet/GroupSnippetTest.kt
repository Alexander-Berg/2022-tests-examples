package ru.auto.ara.test.listing.snippet

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.MarkModelFiltersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class GroupSnippetTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/new"
    private val groupSnippetWithPriceRangeDispatcher = PostSearchOffersDispatcher("groupcard_with_price_range")
    private val groupSnippetWithSinglePriceDispatcher = PostSearchOffersDispatcher("groupcard_with_single_price")
    private val groupSnippetWithIndividualComplectationDispatcher =
        PostSearchOffersDispatcher("groupcard_with_individual_complectation")

    private val markModelFiltersRequestWatcher: RequestWatcher = RequestWatcher()

    private val fullMarkModelFiltersDispatcher: MarkModelFiltersDispatcher =
        MarkModelFiltersDispatcher.full(markModelFiltersRequestWatcher)
    private val emptyMarkModelFiltersDispatcher: MarkModelFiltersDispatcher =
        MarkModelFiltersDispatcher.empty(markModelFiltersRequestWatcher)

    private val dispatcherHolder = DispatcherHolder()
    private val markModelFiltersDispatcherHolder = DispatcherHolder()

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            CountDispatcher("cars"),
            dispatcherHolder,
            markModelFiltersDispatcherHolder,
            ParseDeeplinkDispatcher.carsNew()
        )
    }
    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun shouldSeeElementsOnGroupSnippetWithRangePrice() {
        testGroupSnippet(
            dispatcher = groupSnippetWithPriceRangeDispatcher,
            markModelFiltersDispatcher = fullMarkModelFiltersDispatcher,
            title = "Audi Q8 I",
            price = "5??060??000???????8??244??592??\u20BD",
            leftTechParams = listOf(
                "????????????, ????????????",
                "3.0 ?? / 249???340 ??.??.",
                "5 ????????????????????????",
            ),
            rightTechParams = listOf(
                "????????????",
                "????????????????????????????",
                "?????????????????????? 5 ????."
            ),
            offersCountTitle = "48 ??????????????????????",
            isMatchApplicationFormVisible = true
        )
    }

    @Test
    fun shouldSeeElementsOnGroupSnippetWithSinglePrice() {
        testGroupSnippet(
            dispatcher = groupSnippetWithSinglePriceDispatcher,
            markModelFiltersDispatcher = emptyMarkModelFiltersDispatcher,
            title = "Audi A8 IV (D5)",
            price = "6 453 700??\u20BD",
            leftTechParams = listOf(
                "????????????",
                "3.0 ?? / 340 ??.??.",
                "55 TFSI quattro tiptronic"
            ),
            rightTechParams = listOf(
                "????????????",
                "????????????????????????????",
                "??????????"
            ),
            offersCountTitle = "1 ??????????????????????",
            isMatchApplicationFormVisible = false
        )
    }

    @Test
    fun shouldSeeElementsOnGroupSnippetWithIndividualComplectation() {
        testGroupSnippet(
            dispatcher = groupSnippetWithIndividualComplectationDispatcher,
            markModelFiltersDispatcher = fullMarkModelFiltersDispatcher,
            title = "Bentley Continental GT II ????????????????????",
            price = "12 900 000 \u20BD",
            leftTechParams = listOf(
                "????????????",
                "4.0 ?? / 528 ??.??."
            ),
            rightTechParams = listOf(
                "????????????",
                "????????????????????????????",
                "????????"),
            offersCountTitle = "1 ??????????????????????",
            isMatchApplicationFormVisible = false
        )
    }

    private fun testGroupSnippet(
        dispatcher: PostSearchOffersDispatcher,
        markModelFiltersDispatcher: MarkModelFiltersDispatcher,
        title: String,
        price: String,
        leftTechParams: List<String>,
        rightTechParams: List<String>,
        offersCountTitle: String,
        isMatchApplicationFormVisible: Boolean,
    ) {
        assertThat("techParams has at least $MIN_TECH_PARAMS params",
            leftTechParams.size + rightTechParams.size >= MIN_TECH_PARAMS)
        dispatcherHolder.innerDispatcher = dispatcher
        markModelFiltersDispatcherHolder.innerDispatcher = markModelFiltersDispatcher
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performListingOffers { scrollToFirstSnippet() }
            .checkResult {
                isTitleDisplayed(title)
                isPriceDisplayed(price)
                isColorBadgesDisplayed()
            }

        markModelFiltersRequestWatcher.checkQueryParameter("search_tag", "match_applications")

        performListingOffers { scrollToFilterPromo() }
            .checkResult {
                isTechParam1Displayed(leftTechParams[0])
                isTechParam2Displayed(leftTechParams[1])
                leftTechParams.getOrNull(index = 2)?.let(::isTechParam3Displayed) ?: isTechParam3NotDisplayed()
                isTechParam4Displayed(rightTechParams[0])
                isTechParam5Displayed(rightTechParams[1])
                isTechParam6Displayed(rightTechParams[2])
                isOfferCountGroupcardButtonDisplayed(offersCountTitle, isMatchApplicationFormVisible.not())
                if (isMatchApplicationFormVisible) {
                    isMatchApplicationButtonDisplayed()
                } else {
                    isMatchApplicationButtonGone()
                }
                isLoanPriceGone()
            }
    }

    companion object {
        private const val MIN_TECH_PARAMS = 5
    }
}
