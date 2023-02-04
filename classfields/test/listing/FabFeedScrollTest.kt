package ru.auto.ara.test.listing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.data.models.FormState
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.ara.viewmodel.search.SearchFeedContext
import ru.auto.data.model.search.SearchContext

@RunWith(AndroidJUnit4::class)
class FabFeedScrollTest {

    private val activityTestRule = lazyActivityScenarioRule<SearchFeedActivity>()

    @JvmField
    @Rule
    val rule = baseRuleChain(
        WebServerRule {
            postSearchOffers()
        },
        DisableAdsRule(),
        activityTestRule
    )

    @Before
    fun setUp() {
        activityTestRule.launchFragment<SearchFeedFragment>(
            SearchFeedFragment.createArgs(
                SearchFeedContext(
                    context = SearchContext.DEFAULT,
                    formState = FormState.withDefaultCategory()
                )
            )
        )
        performSearchFeed {
            waitSearchFeed()
        }
    }

    @Test
    fun shouldHideOnScrollUpAndShowAgainOnScrollDown() {
        performSearchFeed().checkResult {
            isFabDisplayed()
        }
        performSearchFeed {
            scrollDownOneScreen()
        }.checkResult {
            isFabHidden()
        }
        performSearchFeed {
            refreshFeed()
        }.checkResult {
            isFabDisplayed()
        }
    }
}
