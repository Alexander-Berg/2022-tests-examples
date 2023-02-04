package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.tags.GetSearchTagsDispatcher
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.filter.StateGroup

@RunWith(AndroidJUnit4::class)
class SearchTagsDeeplinkTest {

    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars"),
        PostSearchOffersDispatcher.getGenericFeed(),
        ParseDeeplinkDispatcher("cars_all_search_tag", null)
    )
        .plus(StateGroup.values().map { GetSearchTagsDispatcher(it) })

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchDeepLinkActivity(
            "https://auto.ru/moskva/cars/all/?sort=fresh_relevance_1-desc&search_tag=new_car_old_price"
        )
    }

    @Test
    @Ignore("There is some race condition")
    fun shouldShowSelectedTagsOnFeedOnlyWhereItIsAllowed() {
        performSearchFeed {
            waitSearchFeed()
        }.checkResult {
            isFabParameterCount(1)
        }

        performSearchFeed {
            selectState(StateGroup.USED)
            waitSearchFeed()
        }.checkResult {
            isFabParameterCount(0)
        }
    }
}
