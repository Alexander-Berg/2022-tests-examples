package ru.auto.ara.test.filters.mmg

import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.breadcrumbs.BreadcrumbsSuggestDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.filters.checkMark
import ru.auto.ara.core.robot.filters.performMark
import ru.auto.ara.core.robot.filters.performModel
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.checkCatalogFilter
import ru.auto.data.model.filter.CatalogFilter
import ru.auto.data.util.REGEX_ANY

@RunWith(AndroidJUnit4::class)
class MarkTest {
    private val watcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            CountDispatcher("cars", watcher)
        )
    }
    private val activityRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
        webServerRule.routing { delegateDispatcher(BreadcrumbsSuggestDispatcher.carMarks()) }
        performMain { openFilters() }
        performFilter { chooseMarkModel() }
    }

    @Test
    fun shouldSeeModelPickerControls() {
        checkMark {
            isToolbarTitleWithText("Марки")
            isToolbarNavBtnDisplayedWithUpIcon()
            interactions.onToolbarSearchInTabs().checkIsCompletelyDisplayed()
            interactions.onShowClearButtonInTabs().checkIsGone()
        }
    }

    @Test
    fun shouldFilterAndSelectMarkInPicker() {
        val filteredMarks = listOf("Chery", "Chevrolet", "Porsche")
        val selectedMark = "Chevrolet"
        val filterText = "che"

        webServerRule.routing { delegateDispatcher(BreadcrumbsSuggestDispatcher.carMarks(lookup = REGEX_ANY)) }

        performMark { interactions.onToolbarSearch().performClick() }
        checkMark { interactions.onToolbarTextSearch().waitUntilIsCompletelyDisplayed() }
        performMark { interactions.onToolbarTextSearch().perform(typeText(filterText)) }
        checkMark { isExactNumberOfMarksInList(filteredMarks) }
        performMark { interactions.onMarkWithNameInTabs(selectedMark).waitUntilIsCompletelyDisplayed().performClick() }
        watcher.checkCatalogFilter(CatalogFilter(mark = selectedMark.toUpperCase()))
    }

    @Test
    fun shouldSeeVendorListAndSelectVendorWithoutModel() {
        val foreignCarVendorName = "Иномарки"
        val domesticCarVendorName = "Отечественные"
        val selectedVendorName = "Японские"
        val selectedVendorId = "VENDOR7"
        val specificForeignCarVendorNames =
            listOf("Европейские", "Японские", "Корейские", "Американские", "Китайские", "Кроме китайских")

        performMark { scrollToMarkItemWithText("AC") }
        checkMark {
            isExpandable(foreignCarVendorName)
            specificForeignCarVendorNames.map { vendorName -> interactions.onItemWithName(vendorName).checkNotExists() }
            isNotExpandable(domesticCarVendorName)
        }
        performMark { interactions.onIconForMark(foreignCarVendorName).performClickLeftCenter() }
        checkMark { specificForeignCarVendorNames.map { vendorName -> isMarkDisplayedInList(vendorName) } }
        performMark { interactions.onItemWithName(selectedVendorName).performClick() }
        checkFilter { interactions.onSelectMarkModelContainerWithHint(selectedVendorName).waitUntilIsCompletelyDisplayed() }
        watcher.checkCatalogFilter(CatalogFilter(vendor = selectedVendorId))
    }

    @Test
    fun shouldSelectMultipleMarksAndVendor() {
        val selectedMarks = listOf("BMW", "Audi")
        val selectedMarksIds = listOf("BMW", "AUDI")
        val selectedVendor = "Отечественные"
        val selectedVendorId = "VENDOR1"

        selectedMarks.forEach {
            webServerRule.routing { delegateDispatcher(BreadcrumbsSuggestDispatcher.carModelsForMarkWithName("bmw")) }
            performMark { selectMark(it) }
            performModel { tapOnAcceptButton() }
            webServerRule.routing { delegateDispatcher(BreadcrumbsSuggestDispatcher.carMarks()) }
            performFilter { addMarkModel() }
        }
        performMark {
            scrollToMarkItemWithText(selectedVendor)
            selectMark(selectedVendor)
        }
        checkFilter {
            selectedMarks.forEach { isMarkItemSelected(it) }
            interactions.onSelectMarkModelContainerWithHint(selectedVendor).waitUntilIsCompletelyDisplayed()
        }
        watcher.checkCatalogFilter(
            *Array(selectedMarksIds.count()) { i -> CatalogFilter(mark = selectedMarksIds[i]) },
            CatalogFilter(vendor = selectedVendorId)
        )
    }
}
