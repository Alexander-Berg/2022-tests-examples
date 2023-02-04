package ru.auto.ara.test.filters.mmg

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.actions.step
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.breadcrumbs.BreadcrumbsSuggestDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.filters.checkMultiGeneration
import ru.auto.ara.core.robot.filters.performMark
import ru.auto.ara.core.robot.filters.performModel
import ru.auto.ara.core.robot.filters.performMultiGeneration
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.checkCatalogFilter
import ru.auto.data.model.filter.CatalogFilter


@RunWith(AndroidJUnit4::class)
class MultiGenerationPickerTest {

    private val watcher = RequestWatcher()
    private val breadcrumbsHolder = DispatcherHolder()
    private val suggestCarMarks = BreadcrumbsSuggestDispatcher.carMarks()
    private val suggestBMWModels = BreadcrumbsSuggestDispatcher.carModelsForMarkWithName(SELECTED_MARK.toLowerCase())
    private val suggestGenerationsWithBmwThreeModels = BreadcrumbsSuggestDispatcher.carBmw1ER3ER6ERGeneration()
    private val suggestGenerationsWithBmw3ERModel = BreadcrumbsSuggestDispatcher.carBmw3ERGeneration()

    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars", watcher),
        breadcrumbsHolder
    )

    private val activityRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        activityRule,
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
        selectBmwMarkAndOpenModels(SELECTED_MARK)
    }

    private fun selectBmwMarkAndOpenModels(markName: String) = step(
        "select $markName and open models picker"
    ) {
        performMain { openFilters() }
        performFilter {
            breadcrumbsHolder.innerDispatcher = suggestCarMarks
            chooseMarkModel()
        }
        performMark {
            breadcrumbsHolder.innerDispatcher = suggestBMWModels
            selectMark(markName)
        }
    }

    private fun selectThreeModelsAndOpenGenerationPicker() {
        selectModelsAndOpenGenerationsPicker(THREE_MODELS, suggestGenerationsWithBmwThreeModels)
    }

    private fun select3ERAndOpenGenerationPicker() {
        selectModelsAndOpenGenerationsPicker(listOf(MODEL_3ER), suggestGenerationsWithBmw3ERModel)
    }

    private fun selectModelsAndOpenGenerationsPicker(
        models: Collection<String>,
        generationDispatcher: BreadcrumbsSuggestDispatcher
    ) = step("select models =${models.joinToString()} and open generation picker") {
        performModel {
            models.forEach { tapOnCheckboxUntilItChecked(it) }
            tapOnAcceptButton()
        }
        performFilter {
            breadcrumbsHolder.innerDispatcher = generationDispatcher
            clickOnEmptyGenerationField()
        }
    }

    private fun buildCatalogFilters(
        mark: String,
        model: String,
        generations: Collection<Long>? = null
    ) = when (generations) {
        null -> listOf(CatalogFilter(mark = mark, model = model))
        else -> generations.map { CatalogFilter(mark = mark, model = model, generation = it) }
    }

    @Test
    fun shouldSeeModelsTabsWhenFewModelsSelected() {
        selectThreeModelsAndOpenGenerationPicker()

        checkMultiGeneration { isModelsTabsShown(THREE_MODELS) }
    }

    @Test
    fun shouldNotSeeModelsTabsWhenOneModelSelected() {
        select3ERAndOpenGenerationPicker()

        checkMultiGeneration{ isModelsTabsHidden() }
    }

    @Test
    fun shouldApplyAllGenerationsWhenTapOnGenerationItems() {
        selectThreeModelsAndOpenGenerationPicker()

        val modelName1ER = MODEL_1ER
        val generationsIds1ER = GENERATIONS_1ER_IDS.take(SELECTED_GENERATIONS_COUNT)
        val generationNames1ER = GENERATIONS_1ER.take(SELECTED_GENERATIONS_COUNT)

        val modelName3ER = MODEL_3ER
        val generationNamesIds3ER = GENERATIONS_3ER_IDS.take(SELECTED_GENERATIONS_COUNT)
        val generationNames3ER = GENERATIONS_3ER.take(SELECTED_GENERATIONS_COUNT)

        val modelName6ER = MODEL_6ER
        val generationId6ER = GENERATIONS_6ER_IDS.first()
        val generationName6ER = GENERATIONS_6ER.first()

        performMultiGeneration {
            clickOnModelTab(modelName1ER)
            generationNames1ER.map { name -> clickOnGenerationCheckbox(name) }
            clickOnModelTab(modelName3ER)
            generationNames3ER.map { name -> clickOnGenerationCheckbox(name) }
            clickOnModelTab(modelName6ER)
            clickOnGenerationWithName(generationName6ER)
        }
        checkFilter {
            isGenerationItemSelected(generationNames1ER.plus(generationNames3ER).plus(generationName6ER).joinToString())
        }


        watcher.checkCatalogFilter(
            buildCatalogFilters(SELECTED_MARK, MODEL_1ER_ID, generationsIds1ER) +
                buildCatalogFilters(SELECTED_MARK, MODEL_3ER_ID, generationNamesIds3ER) +
                buildCatalogFilters(SELECTED_MARK, MODEL_6ER_ID, listOf(generationId6ER))
        )
    }

    @Test
    fun shouldSelectGenerationWhenTapOnCheckbox() {
        selectThreeModelsAndOpenGenerationPicker()

        val modelName = MODEL_3ER
        val generationName = GENERATIONS_3ER.first()
        val generationId = GENERATIONS_3ER_IDS.first()

        performMultiGeneration { clickOnModelTab(modelName) }
        checkMultiGeneration {
            isGenerationItemNotSelected(generationName)
            isClearButtonHidden()
        }
        performMultiGeneration { clickOnGenerationCheckbox(generationName) }
        checkMultiGeneration {
            isGenerationItemSelected(generationName)
            isClearButtonShown()
            isModelTabBadgeCount(modelName, 1)
            isToolbarTitleGenerationCount(1)
        }

        watcher.checkCatalogFilter(
            buildCatalogFilters(SELECTED_MARK, MODEL_1ER_ID) +
                buildCatalogFilters(SELECTED_MARK, MODEL_3ER_ID, listOf(generationId)) +
                buildCatalogFilters(SELECTED_MARK, MODEL_6ER_ID)
        )
    }

    @Test
    fun shouldApplyChangesWhenTapOnApplyBtn() {
        selectThreeModelsAndOpenGenerationPicker()

        val modelName = MODEL_3ER
        val generationName = GENERATIONS_3ER.first()
        val generationId = GENERATIONS_3ER_IDS.first()

        performMultiGeneration {
            clickOnModelTab(modelName)
            clickOnGenerationCheckbox(generationName)
            clickOnApplyButton()
        }
        checkFilter { isGenerationItemSelected(generationName) }

        watcher.checkCatalogFilter(
            buildCatalogFilters(SELECTED_MARK, MODEL_1ER_ID) +
                buildCatalogFilters(SELECTED_MARK, MODEL_3ER_ID, listOf(generationId)) +
                buildCatalogFilters(SELECTED_MARK, MODEL_6ER_ID)

        )
    }

    @Test
    fun shouldDropChangesWhenTapOnClearBtn() {
        select3ERAndOpenGenerationPicker()

        val generations = GENERATIONS_3ER.take(SELECTED_GENERATIONS_COUNT)

        performMultiGeneration {
            generations.forEach { clickOnGenerationCheckbox(it) }
        }
        checkMultiGeneration {
            generations.forEach { isGenerationItemSelected(it) }
            isClearButtonShown()
        }

        performMultiGeneration { clickOnClearButton() }
        checkMultiGeneration {
            generations.forEach { isGenerationItemNotSelected(it) }
            isClearButtonHidden()
        }

    }

    companion object {
        private const val SELECTED_GENERATIONS_COUNT = 2
        private const val SELECTED_MARK = "BMW"
        private const val MODEL_1ER = "1 серия"
        private const val MODEL_1ER_ID = "1ER"
        private const val MODEL_3ER = "3 серия"
        private const val MODEL_3ER_ID = "3ER"
        private const val MODEL_6ER = "6 серия"
        private const val MODEL_6ER_ID = "6ER"
        private val THREE_MODELS = listOf(MODEL_1ER, MODEL_3ER, MODEL_6ER)

        private val GENERATIONS_1ER_IDS = listOf(21573418L, 21006990L, 20371753L)
        private val GENERATIONS_1ER = listOf(
            "2019 - 2020 III (F40)",
            "2017 - 2020 II (F20/F21) Рестайлинг 2",
            "2015 - 2017 II (F20/F21) Рестайлинг"
        )
        private val GENERATIONS_3ER_IDS = listOf(3659007L, 7690949L, 3473273L, 3473271L, 3473269L, 3473267L)
        private val GENERATIONS_3ER = listOf(
            "2008 - 2013 V (E90/E91/E92/E93) Рестайлинг",
            "2001 - 2006 IV (E46) Рестайлинг",
            "1998 - 2003 IV (E46)",
            "1990 - 2000 III (E36)",
            "1982 - 1994 II (E30)",
            "1975 - 1983 I (E21)"
        )
        private val GENERATIONS_6ER_IDS = listOf(21021798L, 20353125L, 7145509L)
        private val GENERATIONS_6ER = listOf(
            "2017 - 2020 IV (G32)",
            "2015 - 2017 III (F06/F13/F12) Рестайлинг",
            "2011 - 2015 III (F06/F13/F12)"
        )
    }
}
