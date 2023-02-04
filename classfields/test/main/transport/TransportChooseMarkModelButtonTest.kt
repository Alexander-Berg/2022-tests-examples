package ru.auto.ara.test.main.transport

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.breadcrumbs.BreadcrumbsSuggestDispatcher.Companion.bmwModels
import ru.auto.ara.core.dispatchers.breadcrumbs.BreadcrumbsSuggestDispatcher.Companion.carMarks
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.filters.checkMark
import ru.auto.ara.core.robot.filters.checkModel
import ru.auto.ara.core.robot.filters.performMark
import ru.auto.ara.core.robot.filters.performModel
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.checkCatalogFilter
import ru.auto.data.model.filter.CatalogFilter

@RunWith(AndroidJUnit4::class)
class TransportChooseMarkModelButtonTest {
    private val counterWatcher = RequestWatcher()
    private val feedWatcher = RequestWatcher()
    private val breadcrumbsDispatcherHolder = DispatcherHolder().apply { innerDispatcher = carMarks() }
    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars", counterWatcher),
        PostSearchOffersDispatcher.getGenericFeed(feedWatcher),
        breadcrumbsDispatcherHolder
    )

    private val activityRule = ActivityTestRule(MainActivity::class.java, true, false)

    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchActivity(Intent())
        performMain {
            openMarkFilters()
        }
    }

    @Test
    fun shouldSearchForAllModelsWhenTapSearchFromModelPicker() {
        checkMark {
            isToolbarNavBtnDisplayedWithUpIcon()
            isToolbarTitleWithText("Марки")
            interactions.onToolbarSearch().checkIsCompletelyDisplayed()
            interactions.onPopularMarksTitle().waitUntilIsDisplayed()
        }
        performMark {
            scrollToMarkItemWithText("Все марки")
        }.checkResult {
            interactions.onAllMarkTitle().waitUntilIsDisplayed()
        }
        performMark {
            interactions.onShowResultsButton().performClick()
        }
        performSearchFeed {
            waitFirstPage()
        }.checkResult {
            isEmptyMMNGFilterDisplayed()
            feedWatcher.checkCatalogFilter()
        }
    }

    @Test
    fun shouldSearchByMarkWhenTapSearchOnModelPickerWithoutSelectModel() {
        val markName = "BMW"

        breadcrumbsDispatcherHolder.innerDispatcher = bmwModels()
        performMark { interactions.onMarkWithNameInTabs(markName).waitUntilIsCompletelyDisplayed().performClick() }
        counterWatcher.checkCatalogFilter(CatalogFilter(mark = markName))
        checkModel {
            isToolbarNavBtnDisplayedWithUpIcon()
            isToolbarTitleWithText("Модели $markName")
            interactions.onToolbarSearch().checkIsCompletelyDisplayed()
            interactions.onPopularModelsTitle().waitUntilIsDisplayed()
        }
        performModel {
            scrollToModelItemWithText("Все модели")
        }.checkResult {
            interactions.onAllModelTitle().waitUntilIsDisplayed()
        }
        performModel { clickShowResults() }
        performSearchFeed {
            waitFirstPage()
        }.checkResult {
            isMarkFilterWithText(markName)
            feedWatcher.checkCatalogFilter(CatalogFilter(mark = markName))
        }
    }

    @Test
    fun shouldSearchByMarkAndModelIfSelectedBothAndTabSearch() {
        val markName = "BMW"
        val modelName = "3 серия"
        val modelId = "3ER"
        breadcrumbsDispatcherHolder.innerDispatcher = bmwModels()
        performMark { interactions.onMarkWithNameInTabs(markName).waitUntilIsCompletelyDisplayed().performClick() }
        performModel {
            interactions.onModelWithName(modelName).waitUntilIsCompletelyDisplayed().performClick()
        }
        counterWatcher.checkCatalogFilter(CatalogFilter(mark = markName, model = modelId))
        performSearchFeed {
            waitFirstPage()
        }.checkResult {
            isMarkFilterWithText(markName)
            isModelFilterWithText(modelName)
            feedWatcher.checkCatalogFilter(CatalogFilter(mark = markName, model = modelId))
        }
    }
}
