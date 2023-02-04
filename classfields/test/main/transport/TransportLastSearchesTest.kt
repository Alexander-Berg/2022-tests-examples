package ru.auto.ara.test.main.transport

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asArray
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertValue
import ru.auto.ara.core.dispatchers.garage.postEmptyGarageListing
import ru.auto.ara.core.dispatchers.last_searches.SearchHistory
import ru.auto.ara.core.dispatchers.last_searches.getSearchHistory
import ru.auto.ara.core.dispatchers.last_searches.putSearchHistory
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.burger.performBurger
import ru.auto.ara.core.robot.othertab.profile.checkProfile
import ru.auto.ara.core.robot.othertab.profile.performProfile
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.transporttab.checkTransport
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.transporttab.performTransport
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.core.utils.waitSomething
import ru.auto.data.model.filter.StateGroup
import ru.auto.data.util.TRUCKS_ID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TransportLastSearchesTest {

    private val webServerRule = WebServerRule { userSetup() }.await()
    private val activityRule = ActivityTestRule(MainActivity::class.java, false, false)

    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule()
    )

    @Test
    fun shouldNotAddLastSearchFromPreset() {
        webServerRule.routing {
            getSearchHistory(SearchHistory.EMPTY)
            putSearchHistory(TRUCKS_ID).watch { checkRequestWasNotCalled() }
        }
        webServerRule.unlock()
        performCommon { login() }
        activityRule.launchActivity(Intent())

        performTransport {
            selectCategory(R.string.category_comm)
            clickPreset(0)
        }

        performSearchFeed {
            waitSearchFeed()
            pressBack()
        }

        checkTransport { isLastSearchNotExists() }
    }

    @Test
    fun shouldAddLastSearchAfterChangeFilter() {
        webServerRule.routing {
            getSearchHistory(SearchHistory.EMPTY)
            putSearchHistory(TRUCKS_ID).watch {
                checkBody {
                    asObject {
                        get("customs_state_group").assertValue("DOESNT_MATTER")
                        get("damage_group").assertValue("ANY")
                        get("has_image").assertValue("true")
                        get("in_stock").assertValue("ANY_STOCK")
                        get("only_nds").assertValue("false")
                        get("state_group").assertValue("NEW")
                        get("with_delivery").assertValue("BOTH")
                        get("catalog_filter").asArray {}
                        get("exclude_catalog_filter").asArray {}
                        get("exclude_offer_id").asArray {}
                        get("trucks_params").asObject { get("trucks_category").assertValue("LCV") }
                    }
                }
            }
        }
        webServerRule.unlock()
        performCommon { login() }
        activityRule.launchActivity(Intent())

        performTransport {
            selectCategory(R.string.category_comm)
            waitPresetIsCompletelyDisplayed(0)
            waitSomething(1, TimeUnit.SECONDS) // avoid click on params button
            clickPreset(0)
        }

        webServerRule.routing { getSearchHistory(SearchHistory.ONE_ITEM) }

        performSearchFeed {
            waitSearchFeed()
            selectState(StateGroup.NEW)
            waitSearchFeed()
            pressBack()
        }

        checkTransport {
            isLastSearchVisible()
            isCorrectSearchSnippet(title = "Все марки лёгкого коммерческого транспорта", description = "Новые + 3")
        }
    }

    @Test
    fun shouldAddLastSearchFromLastSearchSnippet() {
        webServerRule.routing {
            getSearchHistory(SearchHistory.ONE_ITEM)
            putSearchHistory(TRUCKS_ID).watch { checkRequestWasCalled() }
        }
        webServerRule.unlock()
        performCommon { login() }
        activityRule.launchActivity(Intent())
        performTransport { clickLastSearch(0) }
    }

    @Test
    fun shouldSeeCorrectLastSearchesOrder() {
        webServerRule.routing {
            getSearchHistory(SearchHistory.TWO_ITEM)
            putSearchHistory(TRUCKS_ID)
        }
        webServerRule.unlock()
        performCommon { login() }
        activityRule.launchActivity(Intent())
        checkTransport {
            isCorrectPosition(0, "Audi 80")
            isCorrectPosition(1, "BMW 3 серия")
        }
    }

    @Test
    fun shouldNotSeeLastSearchesAfterLogout() {
        webServerRule.routing {
            getSearchHistory(SearchHistory.EMPTY).watch { checkRequestWasCalled() }
            putSearchHistory(TRUCKS_ID)
            postEmptyGarageListing()
        }
        webServerRule.unlock()
        performCommon { login() }
        activityRule.launchActivity(Intent())
        performMain { openBurgerMenu() }
        performBurger { scrollAndClickOnUserItem() }
        checkProfile { isProfileHeaderDisplayed() }
        performProfile { scrollAndClickLogout() }
        performBurger { clickOnClose() }
        performMain { openLowTab(R.string.search) }
        checkTransport { isLastSearchNotExists() }
    }

    @Test
    fun shouldShowCorrectLastSearchMotoItem() {
        webServerRule.routing {
            getSearchHistory(SearchHistory.MOTO)
            putSearchHistory(TRUCKS_ID)
        }
        webServerRule.unlock()
        performCommon { login() }
        activityRule.launchActivity(Intent())
        checkTransport { isCorrectSearchSnippet(title = "Все марки мотоциклов", description = "Новые + 4") }
    }

    @Test
    fun shouldShowCorrectLastSearchCarItem() {
        webServerRule.routing {
            getSearchHistory(SearchHistory.CAR)
            putSearchHistory(TRUCKS_ID)
        }
        webServerRule.unlock()
        performCommon { login() }
        activityRule.launchActivity(Intent())
        checkTransport { isCorrectSearchSnippet(title = "Все марки автомобилей", description = "Новые + 2") }
    }

    @Test
    fun shouldShowCorrectLastSearchBrandItem() {
        webServerRule.routing {
            getSearchHistory(SearchHistory.AUDI)
            putSearchHistory(TRUCKS_ID)
        }
        webServerRule.unlock()
        performCommon { login() }
        activityRule.launchActivity(Intent())
        checkTransport {
            isCorrectSearchSnippet(title = "Audi 80", description = "Год выпуска от 1987, цена до 90 000 \u20BD + 1")
        }
    }

}
