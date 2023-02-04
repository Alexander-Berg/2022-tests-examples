package ru.auto.ara.test.filters.mmg

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.breadcrumbs.BreadcrumbsSuggestDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.filters.ModelNamplatePcikerRobotChecker.Companion.closeKeyboard
import ru.auto.ara.core.robot.filters.checkModel
import ru.auto.ara.core.robot.filters.performMark
import ru.auto.ara.core.robot.filters.performModel
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.ara.core.utils.checkCatalogFilter
import ru.auto.data.model.filter.CatalogFilter

@RunWith(AndroidJUnit4::class)
class MultiModelNameplatePickerTest {

    private val watcher = RequestWatcher()

    private val webServerRule = WebServerRule {
        delegateDispatcher(CountDispatcher("cars", watcher))
    }
    private val activityRule = activityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        activityRule,
    )

    @Before
    fun setUp() {
        webServerRule.routing { stub { delegateDispatcher(BreadcrumbsSuggestDispatcher.carMarks()) } }
        performMain { openFilters() }
        performFilter {
            chooseMarkModel()
        }
        webServerRule.routing {
            delegateDispatcher(BreadcrumbsSuggestDispatcher.carModelsForMarkWithName(SELECTED_MARK.toLowerCase()))
        }
        performMark {
            selectMark(SELECTED_MARK)
        }
        performModel { waitItemIsDisplayed("Популярные модели") }
    }

    @Test
    fun shouldSeeCorrectToolbarStateAfterPickerOpened() {
        performModel {
            waitToolbarIsDisplayed()
            waitAcceptButtonIsDisplayed()
        }
        checkModel {
            isToolbarTitleWithText("Модели $SELECTED_MARK")
            isToolbarNavBtnDisplayedWithUpIcon()
            isClearButtonNotVisible()
        }
    }

    @Test
    fun shouldSeeClearButtonAfterTapOnAllModelsItem() {
        performModel {
            tapOnSelectAllUntilChecked()
        }
        checkModel {
            isClearButtonVisible()
        }
    }

    @Test
    fun shouldFilterAndSelectModelWithExpandNameplates() {
        val filteredModelAndNameplates = listOf("2 серия Grand Tourer", "214", "216", "218", "220", "225", "M235i")
        val filterText = "Grand Tourer"
        val selectedModelName = "2 серия Grand Tourer"
        val selectedModelId = "2GRANDTOURER"

        performModel {
            clickToolbarSearch()
            filterModelsByText(filterText)
        }
        closeKeyboard()
        checkModel { isExactNumberOfModelsInList(filteredModelAndNameplates) }
        performModel { tapOnModelCheckbox(selectedModelName) }
        closeKeyboard()
        performModel {
            clickToolbarClear()
            filterModelsByText(filterText)
        }
        checkModel {
            isExactNumberOfModelsInList(filteredModelAndNameplates)
            filteredModelAndNameplates.forEach { modelName ->
                isCheckBoxCheckedForNameplateSublist(
                    modelName
                )
            }
        }
        watcher.checkCatalogFilter(
            CatalogFilter(
                mark = SELECTED_MARK,
                model = selectedModelId
            )
        )
    }

    @Test
    fun shouldFilterAndTapOnModelThenCheckModelApplied() {
        val filterText = "Grand Tourer"
        val selectedModelName = "2 серия Grand Tourer"
        val selectedModelId = "2GRANDTOURER"

        performModel {
            clickToolbarSearch()
            filterModelsByText(filterText)
        }
        closeKeyboard()
        performModel { tapOnModelWithName(selectedModelName) }
        checkFilter {
            isMarkItemSelected(SELECTED_MARK)
            isModelItemSelected(selectedModelName)
        }

        watcher.checkCatalogFilter(
            CatalogFilter(
                mark = SELECTED_MARK,
                model = selectedModelId
            )
        )
    }

    @Test
    fun shouldFilterAndTapOnEmptyNameplateThenCheckNameplatesApplied() {
        performModel {
            clickToolbarSearch()
            filterModelsByText("Grand Tourer")
        }
        closeKeyboard()
        performModel {
            tapOnNamePlateWithName("2 серия Grand Tourer")
        }
        checkFilter {
            isMarkItemSelected(SELECTED_MARK)
            isModelItemSelected("2 серия Grand Tourer")
        }

        watcher.checkCatalogFilter(
            CatalogFilter(
                mark = SELECTED_MARK,
                model = "2GRANDTOURER",
                nameplate = -1
            )
        )
    }

    @Test
    fun shouldFilterAndTapOnNamePlatesThenCheckNameplatesApplied() {
        performModel {
            clickToolbarSearch()
            filterModelsByText("Grand Tourer")
        }
        closeKeyboard()
        performModel {
            tapOnNameplateCheckbox("2 серия Grand Tourer")
            tapOnNamePlateWithName("218")
        }
        checkFilter {
            isMarkItemSelected(SELECTED_MARK)
            isModelItemSelected("2 серия Grand Tourer: 2 серия Grand Tourer, 218")
        }

        watcher.checkCatalogFilter(
            CatalogFilter(
                mark = SELECTED_MARK,
                model = "2GRANDTOURER",
                nameplate = -1
            ),
            CatalogFilter(
                mark = SELECTED_MARK,
                model = "2GRANDTOURER",
                nameplate = 11303094
            )
        )
    }

    @Test
    fun shouldHaveNoEmptyNameplate() {
        val modelWithoutEmptyNameplate = "8 серия"
        performModel {
            scrollToModelAndExpand(modelWithoutEmptyNameplate)
        }
        checkModel {
            isNameplateNotDisplayed(modelWithoutEmptyNameplate)
        }
    }

    @Test
    fun shouldFilterAndSelectByNameplate() {
        val filteredModelAndNameplates = listOf("3 серия", "330", "335", "6 серия", "633", "7 серия", "733")
        val filterText = "33"
        val selectedModelId = "3ER"
        val selectedNameplateName = "335"
        val selectedNameplateId = 9264875L

        performModel {
            clickToolbarSearch()
            filterModelsByText(filterText)
        }
        closeKeyboard()
        checkModel { isExactNumberOfModelsInList(filteredModelAndNameplates) }
        performModel { tapOnCheckbox(selectedNameplateName) }
        closeKeyboard()
        checkModel { isCheckBoxCheckedForNameplate(selectedNameplateName) }
        watcher.checkCatalogFilter(
            CatalogFilter(
                mark = SELECTED_MARK,
                model = selectedModelId,
                nameplate = selectedNameplateId
            )
        )
    }

    @Test
    fun shouldNameplateContainsModelName() {
        val filterText = "100500"
        val filteredModelAndNameplates = listOf(
            "X100500",
            "X100500 30ddd",
            "X100500 40iii",
            "X100500 50iii",
            "M50ddd"
        )

        performModel {
            clickToolbarSearch()
            filterModelsByText(filterText)
        }
        closeKeyboard()
        checkModel { isExactNumberOfModelsInList(filteredModelAndNameplates) }
    }

    @Test
    fun shouldDisplayExpandArrowOnlyForModelsWithNameplates() {
        val modelWithoutNameplates = "i3"
        val nameplateList = listOf("214", "216", "218", "220", "225", "M235i")
        val modelWithNameplates = "2 серия Grand Tourer"

        performModel { scrollToModelItemWithText("2000 C/CS") }
        checkModel {
            isExpandable(modelWithNameplates)
            nameplateList.map { nameplateName -> isItemNotDisplayed(nameplateName) }
        }
        performModel { scrollToModelAndExpand(modelWithNameplates) }
        checkModel { nameplateList.map { nameplateName -> isNamePlateDisplayedInList(nameplateName) } }
        performModel { scrollToModelItemWithText(modelWithoutNameplates) }
        checkModel { isNotExpandable(modelWithoutNameplates) }
    }


    @Test
    fun shouldSelectWholeModelWhenSelectingAllNameplates() {
        val modelWithNameplates = "X4 M"
        val modelId = "X4_M"

        performModel { scrollToModelAndExpand(modelWithNameplates)
            scrollToNameplateAndTapOnCheckbox("Competition")
        }.checkResult {
            isCheckBoxCheckedForModelWithName(modelWithNameplates)
        }

        watcher.checkCatalogFilter(
            CatalogFilter(
                mark = SELECTED_MARK,
                model = modelId
            )
        )
    }

    @Test
    fun shouldSelectMultipleModelsAndNameplates() {
        val GRAND_TOURER_NAME = "2 серия Grand Tourer"
        val I3_NAME = "i3"
        val selectedModelsIds = listOf("2GRANDTOURER", "I3")
        val modelWithNameplates = "8 серия"
        val modelWithNameplatesId = "8ER"
        val selectedNameplates = listOf("840", "M850")
        val selectedNameplatesIds = listOf(12504007L, 12504005L)

        performModel {
            scrollToModelAndExpand(modelWithNameplates)
            selectedNameplates.map { nameplateName -> scrollToNameplateAndTapOnCheckbox(nameplateName) }
            scrollToModelAndTapOnCheckbox(GRAND_TOURER_NAME)
            scrollToNameplateAndTapOnCheckbox(I3_NAME)
        }
        checkModel {
            selectedNameplates.map { nameplateName -> isCheckBoxCheckedForNameplate(nameplateName) }
            isCheckBoxCheckedForModelWithName(GRAND_TOURER_NAME)
            isCheckBoxCheckedForNameplate(I3_NAME)
        }

        watcher.checkCatalogFilter(
            *Array(selectedNameplatesIds.count()) { i ->
                CatalogFilter(
                    mark = SELECTED_MARK,
                    model = modelWithNameplatesId,
                    nameplate = selectedNameplatesIds[i]
                )
            },
            *Array(selectedModelsIds.count()) { i ->
                CatalogFilter(
                    mark = SELECTED_MARK,
                    model = selectedModelsIds[i]
                )
            }
        )
    }

    companion object {
        private const val SELECTED_MARK = "BMW"
        private const val SUBTITLE_TEXT = "1 выбрана / 50,826 предложений"
    }
}
