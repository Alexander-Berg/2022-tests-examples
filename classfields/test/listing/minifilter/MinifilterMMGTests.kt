package ru.auto.ara.test.listing.minifilter

import android.content.Intent
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.breadcrumbs.getBreadcrumbsAudiModels
import ru.auto.ara.core.dispatchers.breadcrumbs.getBreadcrumbsBmwModels
import ru.auto.ara.core.dispatchers.breadcrumbs.getBreadcrumbsCarMarks
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.filters.performMark
import ru.auto.ara.core.robot.filters.performModel
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.DEFAULT_COMMON_PARAMS
import ru.auto.ara.core.utils.checkCatalogFilter
import ru.auto.ara.ui.helpers.form.util.VehicleSearchToFormStateConverter
import ru.auto.data.model.filter.CarParams
import ru.auto.data.model.filter.CarSearch
import ru.auto.data.model.filter.CatalogFilter
import ru.auto.data.model.search.Generation
import ru.auto.data.model.search.Mark
import ru.auto.data.model.search.Model
import ru.auto.data.model.search.Nameplate

private val SOME_MARK = Mark(
    id = "UAZ",
    name = "УАЗ"
)
private val SOME_MODEL = Model(
    id = "PATRIOT",
    name = "Patriot",
    generations = emptyList(),
    nameplates = emptyList()
)

private val OTHER_MODEL = Model(
    id = "HUNTER",
    name = "Hunter",
    generations = emptyList(),
    nameplates = emptyList()
)

private val SOME_NAMEPLATE = Nameplate(
    id = "123",
    name = "Патриотичный"
)
private val SOME_GENERATION = Generation(
    id = "456",
    name = "Поколение1"
)
private val OTHER_GENERATION = Generation(
    id = "789",
    name = "Поколение2"
)

class MinifilterSingleMarkTest : BaseMinifilterMMGTest(listOf(SOME_MARK)) {
    @Test
    fun shouldShowMarkAndEmptyModelWithoutGeneration() {
        performSearchFeed().checkResult {
            isMarkFilterWithText(SOME_MARK.name)
            isEmptyModelFilterDisplayed()
            isGenerationFilterIsHidden()
        }
        watcher.checkCatalogFilter(CatalogFilter(mark = SOME_MARK.id))
    }
}

class MinifilterMarkModelTest : BaseMinifilterMMGTest(
    listOf(SOME_MARK.copy(models = listOf(SOME_MODEL)))
) {
    @Test
    fun shouldShowMarkAndModelWithEmptyGeneration() {
        performSearchFeed().checkResult {
            isMarkFilterWithText(SOME_MARK.name)
            isModelFilterWithText(SOME_MODEL.name)
            isEmptyGenerationFilterDisplayed()
        }
        watcher.checkCatalogFilter(CatalogFilter(mark = SOME_MARK.id, model = SOME_MODEL.id))
    }
}

class MinifilterMarkModelNameplateTest : BaseMinifilterMMGTest(
    listOf(SOME_MARK.copy(models = listOf(SOME_MODEL.copy(nameplates = listOf(SOME_NAMEPLATE)))))
) {
    @Test
    fun shouldShowMarkAndNameplateWithEmptyGeneration() {
        performSearchFeed().checkResult {
            isMarkFilterWithText(SOME_MARK.name)
            isModelFilterWithText(EXPECTED_MODEL_TEXT)
            isEmptyGenerationFilterDisplayed()
        }
        watcher.checkCatalogFilter(
            CatalogFilter(
                mark = SOME_MARK.id,
                model = SOME_MODEL.id,
                nameplate = SOME_NAMEPLATE.id.toLong()
            )
        )
    }

    companion object {
        private val EXPECTED_MODEL_TEXT = "${SOME_MODEL.name} ${SOME_NAMEPLATE.name}"
    }
}

class MinifilterMultiMarkMultiGenerationTest : BaseMinifilterMMGTest(
    listOf(
        SOME_MARK.copy(
            models = listOf(
                SOME_MODEL.copy(nameplates = listOf(SOME_NAMEPLATE), generations = listOf(SOME_GENERATION)),
                OTHER_MODEL.copy(generations = listOf(OTHER_GENERATION))
            )
        )
    )
) {
    @Test
    fun shouldShowMultiMarkAndMultiGenCommaSeparated() {
        performSearchFeed().checkResult {
            isModelFilterWithText(EXPECTED_MODEL)
            isGenerationFilterWithText(EXPECTED_GENERATION)
        }
        watcher.checkCatalogFilter(
            CatalogFilter(
                mark = SOME_MARK.id,
                model = SOME_MODEL.id,
                nameplate = SOME_NAMEPLATE.id.toLong(),
                generation = SOME_GENERATION.id.toLong()
            ),
            CatalogFilter(
                mark = SOME_MARK.id,
                model = OTHER_MODEL.id,
                generation = OTHER_GENERATION.id.toLong()
            )
        )
    }

    companion object {
        private val EXPECTED_MODEL = "${SOME_MODEL.name} ${SOME_NAMEPLATE.name}, ${OTHER_MODEL.name}"
        private val EXPECTED_GENERATION = "${SOME_GENERATION.name}, ${OTHER_GENERATION.name}"
    }
}


class MinifilterModelEmptyNameplateTest : BaseMinifilterMMGTest(
    listOf(SOME_MARK.copy(models = listOf(SOME_MODEL.copy(nameplates = listOf(Nameplate(id = "-1", name = SOME_MODEL.name))))))
) {
    @Test
    fun shouldShowMarkModelWithoutEmptyNameplateButCountItInCatalogFilters() {
        performSearchFeed().checkResult {
            isMarkFilterWithText(SOME_MARK.name)
            isModelFilterWithText(SOME_MODEL.name)
        }
        watcher.checkCatalogFilter(
            CatalogFilter(
                mark = SOME_MARK.id,
                model = SOME_MODEL.id,
                nameplate = -1
            )
        )
    }
}

class MinifilterMarkModelGenerationTest : BaseMinifilterMMGTest(
    listOf(SOME_MARK.copy(models = listOf(SOME_MODEL.copy(generations = listOf(SOME_GENERATION)))))
) {
    @Test
    fun shouldShowMarkModelGeneration() {
        performSearchFeed().checkResult {
            isMarkFilterWithText(SOME_MARK.name)
            isModelFilterWithText(SOME_MODEL.name)
            isGenerationFilterWithText(SOME_GENERATION.name)
        }
        watcher.checkCatalogFilter(
            CatalogFilter(
                mark = SOME_MARK.id,
                model = SOME_MODEL.id,
                generation = SOME_GENERATION.id.toLong()
            )
        )
    }
}

class MinifilterMMgTestAfterFilterScreen {
    val watcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        userSetup()
        postSearchOffers("listing_offers/extended_availability_on_order.json").watch {
            checkCatalogFilter(CatalogFilter(mark = "BMW"))
        }
        getBreadcrumbsCarMarks()
    }
    private val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    @JvmField
    @Rule
    val rule = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetupAuthRule()
    )
    @Test
    fun shouldChangeMarkFromMinifilterAfterSelectedOneFromFilter() {
        activityTestRule.launchActivity(Intent())
        performMain { openFilters() }
        performFilter { openMarkFilters() }
        webServerRule.routing { getBreadcrumbsAudiModels() }
        performMark { selectMark("Audi") }
        performModel { tapOnAcceptButton() }
        performFilter { doSearch() }
        webServerRule.routing { getBreadcrumbsCarMarks() }
        performSearchFeed {
            clickMarkFieldUntilFiltersOpened()
        }
        webServerRule.routing { getBreadcrumbsBmwModels() }
        performMark { selectMark("BMW") }
        performModel { clickShowResults() }
        checkSearchFeed { isMarkFilterWithText("BMW") }
    }
}

abstract class BaseMinifilterMMGTest(marks: List<Mark>) : BaseMinifilterListingTest(
    VehicleSearchToFormStateConverter.convert(
        CarSearch(carParams = CarParams(), commonParams = DEFAULT_COMMON_PARAMS.copy(marks = marks))
    )
)
