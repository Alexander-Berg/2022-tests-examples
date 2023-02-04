package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.breadcrumbs.getBreadcrumbsCarMarks
import ru.auto.ara.core.dispatchers.breadcrumbs.getBreadcrumbsCarModelsForMarkWithName
import ru.auto.ara.core.dispatchers.catalog.getAvailableVariantsForCatalogFilters
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.robot.filters.performMark
import ru.auto.ara.core.robot.filters.performModel
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class MutuallyExclusiveFiltersTest {

    private val webServerRule = WebServerRule {
        getOfferCount()
        getBreadcrumbsCarMarks()
    }
    private val activityRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityRule,
    )

    @Before
    fun setup() {
        activityRule.launchActivity()
        performMain { openFilters() }
    }

    @Test
    fun shouldShowEnginesFiltersAvailableOnBmw() {
        checkMultiSelectFieldWhenExistAvailableFilters(
            fieldName = ENGINE_FIELD_NAME,
            scrollFieldName = MILEAGE_FIELD_NAME,
            screenshotPrefix = ENGINE_SCREENSHOT_PREFIX,
        )
    }

    @Test
    fun shouldShowTransmissionFiltersAvailableOnBmw() {
        checkMultiSelectFieldWhenExistAvailableFilters(
            fieldName = TRANSMISSION_FIELD_NAME,
            screenshotPrefix = TRANSMISSION_SCREENSHOT_PREFIX,
        )
    }

    @Test
    fun shouldShowBodyFiltersAvailableOnBmw() {
        checkMultiSelectFieldWhenExistAvailableFilters(
            fieldName = BODY_FIELD_NAME,
            scrollFieldName = MILEAGE_FIELD_NAME,
            screenshotPrefix = BODY_SCREENSHOT_PREFIX,
        )
    }

    @Test
    fun shouldShowGearFiltersAvailableOnBmw() {
        checkMultiSelectFieldWhenExistAvailableFilters(
            fieldName = GEAR_FIELD_NAME,
            scrollFieldName = MILEAGE_FIELD_NAME,
            screenshotPrefix = GEAR_SCREENSHOT_PREFIX,
        )
    }

    @Test
    fun shouldResetSelectedItemsIfTheyAreNotInCatalog() {
        performFilter {
            clickFieldWithOverScroll(TRANSMISSION_FIELD_NAME, TRANSMISSION_FIELD_NAME)
            clickCheckBox(ROBOT_TRANSMISSION_NAME)
            clickCheckBox(VARIATOR_TRANSMISSION_NAME)
            clickCheckBox(MECHANICAL_TRANSMISSION_NAME)
            clickAcceptButton()
        }.checkResult {
            val text = "$ROBOT_TRANSMISSION_NAME, $VARIATOR_TRANSMISSION_NAME, $MECHANICAL_TRANSMISSION_NAME"
            isContainer(TRANSMISSION_FIELD_NAME, text)
        }

        selectMark()

        checkFilter { isContainer(TRANSMISSION_FIELD_NAME, MECHANICAL_TRANSMISSION_NAME) }
    }

    @Test
    fun shouldSelectOnlyAvailableInAutomaticTransmissionGroup() {
        performFilter {
            clickFieldWithOverScroll(TRANSMISSION_FIELD_NAME, TRANSMISSION_FIELD_NAME)
            clickCheckBox(ROBOT_TRANSMISSION_NAME)
            clickAcceptButton()
        }.checkResult {
            isContainer(TRANSMISSION_FIELD_NAME, ROBOT_TRANSMISSION_NAME)
        }

        selectMark(availableVariantsPath = "catalog/bmw_available_variants.json")

        checkFilter { isContainer(TRANSMISSION_FIELD_NAME, ROBOT_TRANSMISSION_NAME) }

        performFilter {
            clickFieldWithOverScroll(TRANSMISSION_FIELD_NAME, TRANSMISSION_FIELD_NAME)
            clickCheckBox(AUTOMATIC_PARENT_TRANSMISSION_NAME)
            clickAcceptButton()
        }.checkResult {
            val text = "$AUTOMATIC_PARENT_TRANSMISSION_NAME, $AUTOMATIC_TRANSMISSION_NAME, $ROBOT_TRANSMISSION_NAME"
            isContainer(TRANSMISSION_FIELD_NAME, text)
        }
    }

    @Test
    fun shouldShowAllFiltersWhenClearAll() {
        selectMark()

        performFilter {
            clickFieldWithOverScroll(TRANSMISSION_FIELD_NAME, TRANSMISSION_FIELD_NAME)
        }.checkResult {
            isParametersBottomSheetScreenshotSame(getScreenshotName(TRANSMISSION_SCREENSHOT_PREFIX))
        }

        performFilter {
            clickCloseIcon()
            clearAll()
            clickYes()
        }

        performFilter {
            clickFieldWithOverScroll(TRANSMISSION_FIELD_NAME, TRANSMISSION_FIELD_NAME)
        }.checkResult {
            isParametersBottomSheetScreenshotSame("transmission_all_filters.png")
        }
    }

    private fun checkMultiSelectFieldWhenExistAvailableFilters(
        fieldName: String,
        scrollFieldName: String = fieldName,
        screenshotPrefix: String,
    ) {
        selectMark()

        performFilter {
            clickFieldWithOverScroll(scrollFieldName, fieldName)
        }.checkResult {
            isParametersBottomSheetScreenshotSame(getScreenshotName(screenshotPrefix))
        }
    }

    private fun selectMark(
        mark: String = "BMW",
        availableVariantsPath: String = "catalog/bmw_x3_available_variants.json",
    ) {
        performFilter { chooseMarkModel() }
        webServerRule.routing {
            getBreadcrumbsCarModelsForMarkWithName(markName = mark.lowercase())
            getAvailableVariantsForCatalogFilters(filePath = availableVariantsPath)
        }
        performMark { selectMark(mark) }
        performModel { tapOnAcceptButton() }
    }

    companion object {
        private fun getScreenshotName(screenshotPrefix: String) = "${screenshotPrefix}_bmw_filters.png"

        private const val TRANSMISSION_FIELD_NAME = "Коробка"
        private const val GEAR_FIELD_NAME = "Привод"
        private const val BODY_FIELD_NAME = "Кузов"
        private const val ENGINE_FIELD_NAME = "Двигатель"
        private const val MILEAGE_FIELD_NAME = "Пробег, км"

        private const val MECHANICAL_TRANSMISSION_NAME = "Механика"
        private const val AUTOMATIC_PARENT_TRANSMISSION_NAME = "Автомат"
        private const val AUTOMATIC_TRANSMISSION_NAME = "Автоматическая"
        private const val ROBOT_TRANSMISSION_NAME = "Робот"
        private const val VARIATOR_TRANSMISSION_NAME = "Вариатор"

        private const val ENGINE_SCREENSHOT_PREFIX = "engine"
        private const val BODY_SCREENSHOT_PREFIX = "body"
        private const val TRANSMISSION_SCREENSHOT_PREFIX = "transmission"
        private const val GEAR_SCREENSHOT_PREFIX = "gear"
    }
}
