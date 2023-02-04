package ru.auto.ara.test.main.transport

import android.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.reflect.TypeToken
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asArray
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertValue
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.dispatchers.search_offers.getTextSearch
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.searchline.checkSearchline
import ru.auto.ara.core.robot.searchline.performSearchline
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.TestJsonItemRepo
import ru.auto.ara.core.utils.getContextUnderTest
import ru.auto.data.interactor.TextSearchSuggestsInteractor
import ru.auto.data.model.search.SavedSuggestItem
import ru.auto.data.util.BULLET

private const val FORD_FOCUS_ICON = "https://avatars.mds.yandex.net/get-verba/1540742/2a0000016d691c30185172465dd1125ee1a5/logo"

@RunWith(AndroidJUnit4::class)
class SearchlineTest {

    private val webRule = WebServerRule {
        userSetup()
        getOfferCount()
        stub { delegateDispatcher(PostSearchOffersDispatcher.getGenericFeed()) }
    }

    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(getContextUnderTest())
    private val savedItemsRepo = TestJsonItemRepo(
        key = TextSearchSuggestsInteractor.SUGGEST_KEY,
        prefs = prefs,
        typeToken = object : TypeToken<ArrayList<SavedSuggestItem>>() {}
    )

    @JvmField
    @Rule
    val rules = baseRuleChain(
        webRule,
        activityRule,
        SetupAuthRule(),
        SetPreferencesRule()
    )

    @Test
    fun shouldSee5ItemsFromDefaultsOnStart() {
        activityRule.launchActivity()
        performMain { openSearchline() }
        performSearchline { waitScreenLoaded() }
        checkSearchline {
            isDefaultSuggestsTitleDisplayed(0)
            areRandomDefaultsVisible()
        }
    }

    @Test
    fun shouldSaveClickedDefault() {
        activityRule.launchActivity()
        webRule.routing {
            getTextSearch(fordSuggest)
        }
        performMain { openSearchline() }
        performSearchline {
            waitScreenLoaded()
            selectSuggest(1)
        }
        performSearchFeed { waitFirstPage() }
        Espresso.pressBack()
        performMain { openSearchline() }
        performSearchline { waitScreenLoaded() }
        checkSearchline {
            isSavedSuggestsTitleDisplayed(0)
            isSavedDefaultVisible(1)
            isDefaultSuggestsTitleDisplayed(2)
            areRandomDefaultsVisible(offset = 2)
        }
    }

    @Test
    fun shouldResetToDefaultsAfterClear() {
        activityRule.launchActivity()
        webRule.routing {
            getTextSearch(fordSuggest)
        }
        performMain { openSearchline() }
        performSearchline {
            replaceText(QUERY_FORD_FOCUS_2008)
        }
        checkSearchline {
            isSuggestDisplayed(
                suggest = "Ford Focus",
                index = 0,
                subtitle = "Легковые${BULLET}от 2008 г.",
                icon = FORD_FOCUS_ICON
            )
        }
        performSearchline {
            clickOnClear()
            waitScreenLoaded()
        }
        checkSearchline {
            isDefaultSuggestsTitleDisplayed(0)
            areRandomDefaultsVisible()
        }
    }

    @Test
    fun shouldChangeSavedSuggestsAfterClick() {
        savedItemsRepo.save(
            listOf(
                SavedSuggestItem.from(query = "BMW дизель", logo = "ic_car", date = 1),
                SavedSuggestItem.from(query = "Буханка от 2010 года", logo = "ic_comm", date = 0)
            )
        )
        activityRule.launchActivity()
        webRule.routing {
            getTextSearch(fordSuggest)
        }
        performMain { openSearchline() }
        checkSearchline {
            isSavedSuggestsTitleDisplayed(0)
            isSavedSuggestDisplayed("BMW дизель", 1)
            isSavedSuggestDisplayed("Буханка от 2010 года", 2)
        }
        performSearchline {
            selectSuggest(2)
        }
        performSearchFeed { waitFirstPage() }
        Espresso.pressBack()
        performMain { openSearchline() }
        performSearchline { waitScreenLoaded() }
        checkSearchline {
            isSavedSuggestsTitleDisplayed(0)
            isSavedSuggestDisplayed("Буханка от 2010 года", 1)
            isSavedSuggestDisplayed("BMW дизель", 2)
            isDefaultSuggestsTitleDisplayed(3)
            areRandomDefaultsVisible(offset = 3)
        }
    }

    @Test
    fun shouldNotSaveSameSearchTwice() {
        activityRule.launchActivity()
        webRule.routing {
            getTextSearch(fordSuggest)
        }
        performMain { openSearchline() }
        performSearchline {
            replaceText(QUERY_FORD_FOCUS_2008)
        }
        checkSearchline {
            isSuggestDisplayed(
                suggest = "Ford Focus",
                index = 0,
                subtitle = "Легковые${BULLET}от 2008 г.",
                icon = FORD_FOCUS_ICON
            )
        }
        performSearchline { selectSuggest("Ford Focus") }
        performSearchFeed { waitFirstPage() }
        Espresso.pressBack()
        performMain { openSearchline() }
        performSearchline {
            replaceText("Ford Focus")
        }
        checkSearchline {
            isSuggestDisplayed(
                suggest = "Ford Focus",
                index = 0,
                subtitle = "Легковые${BULLET}от 2008 г.",
                icon = FORD_FOCUS_ICON
            )
        }
        performSearchline { selectSuggest("Ford Focus") }
        performSearchFeed { waitFirstPage() }
        Espresso.pressBack()
        performMain { openSearchline() }
        checkSearchline {
            isSavedSuggestsTitleDisplayed(0)
            isSuggestDisplayed(
                suggest = "Ford Focus",
                index = 1,
                subtitle = "Легковые${BULLET}от 2008 г.",
                icon = R.drawable.ic_history.toString()
            )
            isDefaultSuggestsTitleDisplayed(2)
            areRandomDefaultsVisible(offset = 2)
        }
    }

    @Test
    fun shouldOpenFeedFromTextSearch() {
        activityRule.launchActivity()
        webRule.routing {
            getTextSearch(fordSuggest)
        }
        setupSearchWatcher()
        performMain { openSearchline() }
        performSearchline {
            doSearch(QUERY_FORD_FOCUS_2008)
        }
        performSearchFeed {
            waitSearchFeed()
            waitFirstPage()
        }
    }

    @Test
    fun shouldOpenFeedFromSavedTextSearch() {
        activityRule.launchActivity()
        webRule.routing {
            getTextSearch(fordSuggest)
        }
        setupSearchWatcher()
        performMain { openSearchline() }
        performSearchline {
            doSearch(QUERY_FORD_FOCUS_2008)
        }
        performSearchFeed {
            waitFirstPage()
        }
        Espresso.pressBack()
        performMain {
            openSearchline()
        }
        checkSearchline {
            isSavedSuggestDisplayed(QUERY_FORD_FOCUS_2008, 1)
        }
        performSearchline { selectSuggest(QUERY_FORD_FOCUS_2008) }
        performSearchFeed {
            waitSearchFeed()
            waitFirstPage()
        }
    }

    @Test
    fun shouldShowErrorOnTextSearchFailure() {
        activityRule.launchActivity()
        webRule.routing {
            getTextSearch(failedSuggest)
        }
        performMain {
            openSearchline()
        }
        performSearchline {
            doSearch(QUERY_NOT_FOUND)
        }
        checkSearchline {
            isSuggestErrorDisplayed()
        }
    }

    private fun setupSearchWatcher() {
        webRule.routing {
            delegateDispatcher(PostSearchOffersDispatcher.getGenericFeed())
        }.watch {
            checkBody {
                asObject {
                    getValue("catalog_filter").asArray {
                        first().asObject {
                            getValue("mark").assertValue("FORD")
                            getValue("model").assertValue("FOCUS")
                        }
                    }
                    getValue("year_from").assertValue("2008")
                }
            }
        }
    }
}

private const val QUERY_FORD_FOCUS_2008 = "форд фокус от 2008"
private const val QUERY_NOT_FOUND = "аарьобобоб"
private const val fordSuggest = "suggest_ford"
private const val failedSuggest = "suggest_failure"
