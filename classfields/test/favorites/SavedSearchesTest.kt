package ru.auto.ara.test.favorites

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.favorites.getFavoritesSubscriptions
import ru.auto.ara.core.dispatchers.search_offers.getSavedSearchCountDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.favorites.checkSavedFilters
import ru.auto.ara.core.robot.favorites.performFavorites
import ru.auto.ara.core.robot.favorites.performSavedFilters
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SavedSearchesTest {
    private val activityTestRule = lazyActivityScenarioRule<MainActivity>()
    private val webServerRule = WebServerRule()

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun shouldShowDialogConfirmationWhenDeletingSearchBySwipe() {
        webServerRule.routing {
            getFavoritesSubscriptions()
            getSavedSearchCountDispatcher("4b4268992ec1952292d9f1c2ccf115d89ab8f900")
        }
        startActivityAndOpenSavedSearchesTab()
        performSavedFilters {
            swipeToDeleteSearch(title = "Все марки автомобилей", description = "1 параметр")
        }
        checkSavedFilters {
            isConfirmationDialogDisplayed()
        }
    }

    private fun startActivityAndOpenSavedSearchesTab() {
        activityTestRule.launchActivity()
        waitSomething(1, TimeUnit.SECONDS)
        webServerRule.routing { userSetup() }
        performCommon { login() }
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        performFavorites {
            switchToFavoritesTab(SEARCHES_TAB_TEXT)
        }
    }

    companion object {
        private const val FAVORITES_TAB_TEXT = R.string.favorites
        private const val SEARCHES_TAB_TEXT = R.string.saved_search_title_empty
    }
}
