package ru.auto.ara.test.favorites

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.favorites.getFavorites
import ru.auto.ara.core.dispatchers.favorites.getFavoritesSubscriptions
import ru.auto.ara.core.dispatchers.favorites.getFavoritesSubscriptionsMapped
import ru.auto.ara.core.dispatchers.search_offers.getSavedSearchCountDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.favorites.checkFavorites
import ru.auto.ara.core.robot.favorites.checkSavedFilters
import ru.auto.ara.core.robot.favorites.performFavorites
import ru.auto.ara.core.robot.favorites.performSavedFilters
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.transporttab.checkMain
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FavoritesTest {
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
    fun shouldNotShowDotWhenNoUpdates() {
        startActivity()
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, false)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITHOUT_DOT_SCREENSHOT_NAME)
        }
    }

    @Test
    fun shouldNotShowCounterWhenNoUpdates() {
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        checkFavorites {
            isNoCounter()
        }
    }

    @Test
    fun shouldShowDotWhenGotFavoriteOffersPriceChanges() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/oneFavoriteItem.json",
                mapper = { copy(offers_with_new_price_count = 1) }
            )
        }
        startActivity()
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, true)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITH_DOT_SCREENSHOT_NAME)
        }
    }

    @Test
    fun shouldShowCounterWhenGotFavoriteOffersPriceChanges() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/oneFavoriteItem.json",
                mapper = { copy(offers_with_new_price_count = 1) }
            )
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        checkFavorites {
            isCounterDisplayedWithText("Пока вас не было, у 1 объявления изменилась цена")
        }
    }

    @Test
    fun shouldHideDotWhenVisitTabOnOfferChange() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/oneFavoriteItem.json",
                mapper = { copy(offers_with_new_price_count = 1) }
            )
        }
        startActivity()
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, true)
        }
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/oneFavoriteItem.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
        }
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, false)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME)
        }
    }

    @Test
    fun shouldNotHideDotWhenVisitOtherTabOnOfferChange() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/oneFavoriteItem.json",
                mapper = { copy(offers_with_new_price_count = 1) }
            )
        }
        activityTestRule.launchActivity()
        performMain { openLowTab(FAVORITES_TAB_TEXT) }
        performFavorites { switchToFavoritesTab(SEARCHES_TAB_TEXT) }

        performMain { openLowTab(ADD_OFFER_TAB_TEXT) }
        performOffers { clickLogin() }
        webServerRule.routing {
            userSetup()
            postLoginOrRegisterSuccess()
        }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, true)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITH_DOT_SCREENSHOT_NAME)
        }
        performMain { openLowTab(FAVORITES_TAB_TEXT) }
        checkFavorites {
            isFavoriteTabWithNotificationDot(OFFERS_TAB_TEXT, true)
            isTabScreenshotTheSame(OFFERS_TAB_TEXT, OFFERS_TAB_WITH_DOT_SCREENSHOT_NAME)
        }
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/oneFavoriteItem.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
        }
        performFavorites { switchToFavoritesTab(OFFERS_TAB_TEXT) }
        checkFavorites {
            isFavoriteTabWithNotificationDot(OFFERS_TAB_TEXT, false)
            isTabScreenshotTheSame(OFFERS_TAB_TEXT, OFFERS_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME)
        }
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, false)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME)
        }
    }

    @Test
    fun shouldShowDotWhenHaveOffersInactivated() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/inactiveFavoriteOffer.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
        }
        startActivity()
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, true)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITH_DOT_SCREENSHOT_NAME)
        }
    }

    @Test
    fun shouldShowCounterWhenHaveOffersInactivated() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/inactiveFavoriteOffer.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        checkFavorites {
            isCounterDisplayedWithText("Пока вас не было, 1 объявление было снято с продажи")
        }
    }

    @Test
    fun shouldHideCounterOnPullToRefreshNewPriceCount() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/oneFavoriteItem.json",
                mapper = { copy(offers_with_new_price_count = 1) }
            )
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        webServerRule.routing {
            oneOff {
                getFavorites(
                    assetPath = "favorites/oneFavoriteItem.json",
                    mapper = { copy(offers_with_new_price_count = 0) }
                )
            }
        }
        performFavorites {
            waitOffersLoaded()
            expandAppBar()
            pullToRefresh()
        }
        checkFavorites {
            isNoCounter()
        }
    }

    @Test
    fun shouldShowCounterWhenHaveOffersInactivatedAndPriceChanges() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/inactiveFavoriteOffer.json",
                mapper = { copy(offers_with_new_price_count = 1) }
            )
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        checkFavorites {
            isCounterDisplayedWithText("Пока вас не было, 1 объявление было снято с продажи. У 1 объявления изменилась цена")
        }
    }

    @Test
    fun shouldHideCountersOnCrossClick() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/inactiveFavoriteOffer.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        performFavorites {
            clickCounterClose()
        }
        checkFavorites {
            isNoCounter()
        }
    }

    @Test
    fun shouldHideCounterOnPullToRefresh() {
        webServerRule.routing {
                        getFavorites(
                assetPath = "favorites/inactiveFavoriteOffer.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        performFavorites {
            pullToRefresh()
        }
        checkFavorites {
            isNoCounter()
        }
    }

    @Test
    fun shouldShowCounterAfterRefresh() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/oneFavoriteItem.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        webServerRule.routing {
            oneOff {
                getFavorites(
                    assetPath = "favorites/oneFavoriteItem.json",
                    mapper = { copy(offers_with_new_price_count = 1) }
                )
            }
        }
        performFavorites {
            pullToRefresh()
        }
        checkFavorites {
            isCounterDisplayedWithText("Пока вас не было, у 1 объявления изменилась цена")
        }
    }

    @Test
    fun shouldHideDotWhenHaveOffersInactivatedAndVisitFavoritesTab() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/inactiveFavoriteOffer.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        waitSomething(3, TimeUnit.SECONDS) // wait for dot disappearance
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, false)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME)
        }
    }


    @Test
    fun shouldShowDotWhenOfferGoesInactivated() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/oneFavoriteItem.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/inactiveFavoriteOffer.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
        }
        performFavorites { pullToRefresh() }
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, true)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITH_DOT_SELECTED_SCREENSHOT_NAME)
        }
    }

    @Test
    fun shouldNotShowDotWhenSeenInactivatedOffer() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/oneFavoriteItem.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
            openLowTab(SEARCH_LOWER_TAB_TEXT)
        }
        performCommon { logout() }
        performCommon { login() }
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, false)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITHOUT_DOT_SCREENSHOT_NAME)
        }
    }

    @Test
    fun shouldShowDotWhenHaveSavedSearches() {
        webServerRule.routing {
            getFavoritesSubscriptions()
            getSavedSearchCountDispatcher("4b4268992ec1952292d9f1c2ccf115d89ab8f900")
        }
        startActivity()
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, true)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITH_DOT_SCREENSHOT_NAME)
        }
        performMain { openLowTab(FAVORITES_TAB_TEXT) }
        performFavorites { switchToFavoritesTab(SEARCHES_TAB_TEXT) }
        checkFavorites {
            isFavoriteTabWithNotificationDot(SEARCHES_TAB_TEXT, true)
            isTabScreenshotTheSame(SEARCHES_TAB_TEXT, SEARCHES_TAB_WITH_DOT_SELECTED_SCREENSHOT_NAME)
        }
    }

    @Test
    fun shouldSeeOneSavedSearch() {
        webServerRule.routing {
            getFavoritesSubscriptions()
            getSavedSearchCountDispatcher("4b4268992ec1952292d9f1c2ccf115d89ab8f900")
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        performFavorites {
            switchToFavoritesTab(SEARCHES_TAB_TEXT)
        }
        checkSavedFilters {
            isSavedSearchSnippetDisplayed(title = "Все марки автомобилей", description = "1 параметр")
        }
    }


    @Test
    fun shouldHideDotAfterVisitingSearch() {
        webServerRule.routing {
            getFavoritesSubscriptions()
            oneOff {
                getSavedSearchCountDispatcher("4b4268992ec1952292d9f1c2ccf115d89ab8f900", 1000)
                getSavedSearchCountDispatcher("4b4268992ec1952292d9f1c2ccf115d89ab8f900", 1000)
            }
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        performFavorites {
            switchToFavoritesTab(SEARCHES_TAB_TEXT)
        }
        performSavedFilters {
            clickSavedSearchSnippet(title = "Все марки автомобилей", description = "1 параметр")
        }
        webServerRule.routing {
            oneOff {
                getSavedSearchCountDispatcher("4b4268992ec1952292d9f1c2ccf115d89ab8f900", 0)
            }
        }
        pressBack()
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, false)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME)
        }
        checkFavorites {
            isFavoriteTabWithNotificationDot(SEARCHES_TAB_TEXT, false)
            isTabScreenshotTheSame(SEARCHES_TAB_TEXT, SEARCHES_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME)
        }
    }

    @Test
    fun shouldNotHideDotWhenVisitTabOnOfferChangeAndHaveSearch() {
        webServerRule.routing {
            getFavorites(
                assetPath = "favorites/oneFavoriteItem.json",
                mapper = { copy(offers_with_new_price_count = 0) }
            )
            getSavedSearchCountDispatcher("4b4268992ec1952292d9f1c2ccf115d89ab8f900")
            getFavoritesSubscriptions()
        }
        startActivity()
        performMain { openLowTab(FAVORITES_TAB_TEXT) }
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, true)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITH_DOT_SELECTED_SCREENSHOT_NAME)
        }
        performFavorites { switchToFavoritesTab(SEARCHES_TAB_TEXT) }
        webServerRule.routing {
            getFavoritesSubscriptionsMapped { this.copy(saved_searches = emptyList()) }
        }
        performSavedFilters {
            swipeToDeleteSearch(title = "Все марки автомобилей", description = "1 параметр")
            confirmSearchDeletion()
        }
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, false)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME)
        }
    }

    @Test
    fun shouldHideDotAfterDeletingSearch() {
        webServerRule.routing {
            getFavoritesSubscriptions()
            getSavedSearchCountDispatcher("4b4268992ec1952292d9f1c2ccf115d89ab8f900")
        }
        startActivity()
        performMain {
            openLowTab(FAVORITES_TAB_TEXT)
        }
        performFavorites {
            switchToFavoritesTab(SEARCHES_TAB_TEXT)
        }
        webServerRule.routing {
            getFavoritesSubscriptionsMapped { this.copy(saved_searches = emptyList()) }
        }
        performSavedFilters {
            swipeToDeleteSearch(title = "Все марки автомобилей", description = "1 параметр")
            confirmSearchDeletion()
        }
        checkMain {
            isLowTabWithNotificationDot(FAVORITES_TAB_TEXT, false)
            isLowTabTheSame(FAVORITES_TAB_TEXT, FAV_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME)
        }
        checkFavorites {
            isFavoriteTabWithNotificationDot(SEARCHES_TAB_TEXT, false)
            isTabScreenshotTheSame(SEARCHES_TAB_TEXT, SEARCHES_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME)
        }
    }

    private fun startActivity() {
        activityTestRule.launchActivity()
        waitSomething(1, TimeUnit.SECONDS)
        webServerRule.routing { userSetup() }
        performCommon { login() }
    }

    companion object {
        private const val FAVORITES_TAB_TEXT = R.string.favorites
        private const val SEARCHES_TAB_TEXT = R.string.saved_search_title_empty
        private const val OFFERS_TAB_TEXT = R.string.offers
        private const val ADD_OFFER_TAB_TEXT = R.string.add_offer
        private const val SEARCH_LOWER_TAB_TEXT = R.string.search
        private const val PHONE = "+7 (000) 000-00-00"
        private const val CODE = "0000"
        private const val FAV_TAB_WITH_DOT_SCREENSHOT_NAME = "main/tabs/fav_tab_with_dot.png"
        private const val FAV_TAB_WITH_DOT_SELECTED_SCREENSHOT_NAME = "main/tabs/fav_tab_with_dot_selected.png"
        private const val FAV_TAB_WITHOUT_DOT_SCREENSHOT_NAME = "main/tabs/fav_tab_without_dot.png"
        private const val FAV_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME = "main/tabs/fav_tab_without_dot_selected.png"
        private const val OFFERS_TAB_WITH_DOT_SCREENSHOT_NAME = "favorites/tabs/offers_tab_with_dot.png"
        private const val OFFERS_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME = "favorites/tabs/offers_tab_without_dot_selected.png"
        private const val SEARCHES_TAB_WITH_DOT_SELECTED_SCREENSHOT_NAME = "favorites/tabs/searches_tab_with_dot_selected.png"
        private const val SEARCHES_TAB_WITHOUT_DOT_SELECTED_SCREENSHOT_NAME =
            "favorites/tabs/searches_tab_without_dot_selected.png"
    }
}
