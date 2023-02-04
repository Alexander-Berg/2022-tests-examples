package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.last_searches.SearchHistory
import ru.auto.ara.core.dispatchers.last_searches.getSearchHistory
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.dispatchers.tags.GetSearchTagsDispatcher
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.transporttab.performTransport
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.pressBack
import ru.auto.data.model.filter.StateGroup

@RunWith(AndroidJUnit4::class)
class SearchTagsTest {

    private val activityRule = ActivityTestRule(MainActivity::class.java, false, true)
    private val webServerRule = WebServerRule {
        getOfferCount()
        postSearchOffers()
        delegateDispatchers(StateGroup.values().map { GetSearchTagsDispatcher(it) })
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule
    )

    @Test
    fun shouldShowSearchTagsOnAllStateGroups() {
        // open filters
        performMain {
            openFilters()
        }

        // check 'all' state
        performFilter {
            scrollToSearchTags()
            clickBySearchTag("Компактный")
            clickBySearchTag("Малый размер")
        }.checkResult {
            isSearchTagsVisible(
                6,
                "Хорошая управляемость" to false,
                "Экономичный" to false,
                "Бездорожье" to false,
                "Большой" to false,
                "Компактный" to true,
                "Малый размер" to true
            )
        }

        // check new state
        performFilter {
            scrollToState()
            selectState(StateGroup.NEW)
            scrollToSearchTags()
        }.checkResult {
            isSearchTagsVisible(
                12,
                "Компактный" to true,
                "Экономичный" to false,
                "Хорошая управляемость" to false,
                "Бездорожье" to false,
                "Большой" to false,
                "Средний размер" to false
            )
        }

        // check used state
        performFilter {
            scrollToState()
            selectState(StateGroup.USED)
            scrollToSearchTags()
        }.checkResult {
            isSearchTagsVisible(
                13,
                "Компактный" to true,
                "Новинка" to false,
                "Хорошая управляемость" to false,
                "Экономичный" to false,
                "Бездорожье" to false,
                "Большой" to false
            )
        }
    }

    @Test
    fun shouldExpandTagsByOtherTagsClick() {
        performMain {
            openFilters()
        }

        performFilter {
            scrollToSearchTags()
            expandSearchTags(6)
        }.checkResult {
            isSearchTagsVisible(
                0,
                "Хорошая управляемость" to false,
                "Экономичный" to false,
                "Бездорожье" to false,
                "Большой" to false,
                "Компактный" to false,
                "Малый размер" to false,
                "Ликвидный" to false,
                "Комфортный" to false,
                "Вместительный багажник" to false,
                "Негабаритный груз" to false,
                "Просторный задний ряд" to false,
                "Проходимый" to false
            )
        }
    }

    @Test
    fun shouldShowSelectedTagsOnFeedAndLastSearch() {
        performMain {
            openFilters()
        }

        // check params selected
        performFilter {
            scrollToSearchTags()
            clickBySearchTag("Экономичный")
            clickBySearchTag("Малый размер")
            clickBySearchTag("Хорошая управляемость")
        }.checkResult {
            isSearchTagsVisible(
                6,
                "Хорошая управляемость" to true,
                "Экономичный" to true,
                "Бездорожье" to false,
                "Большой" to false,
                "Компактный" to false,
                "Малый размер" to true
            )
        }

        // check fab count
        webServerRule.routing { getSearchHistory(SearchHistory.SEARCH_TAGS) }
        performFilter {
            doSearch()
        }

        performSearchFeed {
            waitSearchFeed()
        }.checkResult {
            isFabParameterCount(3)
        }

        // check last search
        pressBack()
        performTransport()
            .checkResult {
                isCorrectSearchSnippet("Все марки автомобилей", "Хорошая управляемость + 3")
            }
    }
}
