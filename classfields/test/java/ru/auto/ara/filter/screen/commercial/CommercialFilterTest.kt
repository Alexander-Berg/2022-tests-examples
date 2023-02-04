package ru.auto.ara.filter.screen.commercial

import org.assertj.core.api.Assertions
import org.junit.Before
import org.mockito.Mockito
import ru.auto.ara.data.entities.Category
import ru.auto.ara.data.entities.form.ColorItem
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.filter.communication.FilterTag
import ru.auto.ara.filter.screen.FilterScreen
import ru.auto.ara.util.DummyOptions
import ru.auto.ara.util.SerializablePair
import ru.auto.ara.util.android.AndroidMultiOptionsProvider
import ru.auto.ara.util.android.CategoryOptionsProvider
import ru.auto.ara.util.android.ColorOptionsProvider
import ru.auto.ara.util.android.OptionsProvider
import ru.auto.ara.util.android.StringsProvider
import ru.auto.ara.util.stubIt
import ru.auto.core_ui.util.Consts.BUS_SUB_CATEGORY_ID
import ru.auto.core_ui.util.Consts.LIGHT_COMMERCIAL_SUB_CATEGORY_ID
import ru.auto.core_ui.util.Consts.TRAILER_SUB_CATEGORY_ID
import ru.auto.core_ui.util.Consts.TRUCK_SUB_CATEGORY_ID
import ru.auto.core_ui.util.Consts.TRUCK_TRACTOR_SUB_CATEGORY_ID
import ru.auto.data.model.geo.SuggestGeoItem
import ru.auto.data.util.AGRICULTURAL_SUBCATEGORY_ID
import ru.auto.data.util.AUTOLOADER_SUBCATEGORY_ID
import ru.auto.data.util.BULLDOZERS_SUBCATEGORY_ID
import ru.auto.data.util.CONSTRUCTION_SUBCATEGORY_ID
import ru.auto.data.util.CRANE_SUBCATEGORY_ID
import ru.auto.data.util.DREDGE_SUBCATEGORY_ID
import ru.auto.data.util.MUNICIPAL_SUBCATEGORY_ID
import kotlin.test.assertEquals

open class CommercialFilterTest {
    protected lateinit var defaultTestedScreen: FilterScreen
    protected lateinit var stringProvider: StringsProvider
    protected lateinit var optionsProvider: AndroidMultiOptionsProvider
    protected lateinit var colorsProvider: OptionsProvider<ColorItem>
    protected lateinit var categoryProvider: CategoryOptionsProvider
    protected lateinit var dummyCategories: List<Category>

    protected var dummyGeoState: SerializablePair<SuggestGeoItem, Int>? = null

    @Before
    fun setUp() {
        stringProvider = Mockito.mock(StringsProvider::class.java)
        optionsProvider = Mockito.mock(AndroidMultiOptionsProvider::class.java)
        colorsProvider = Mockito.mock(ColorOptionsProvider::class.java)
        categoryProvider = Mockito.mock(CategoryOptionsProvider::class.java)

        stringProvider.stubIt()

        dummyGeoState = SerializablePair(SuggestGeoItem("123", "Moskovia", "Rossia", true), 200)

        dummyCategories = listOf(
                category("16", "Прицепы"),
                category("31", "Лёгкие коммерческие"),
                category("32", "Грузовики"),
                category("33", "Седельные тягачи"),
                category("34", "Автобусы"),
                category("36", "Сельскохозяйственная"),
                category("37", "Строительная"),
                category("38", "Автопогрузчики"),
                category("43", "Автокраны"),
                category("44", "Экскаваторы"),
                category("45", "Бульдозеры"),
                category("53", "Самопогрузчики"),
                category("54", "Коммунальная")
        )

        DummyOptions.applyDummyOptions(optionsProvider)

        Mockito.`when`(categoryProvider["commercial_categories"]).thenReturn(dummyCategories)

        defaultTestedScreen = screen(LIGHT_COMMERCIAL_SUB_CATEGORY_ID)
    }

    fun category(id: String, name: String): Category {
        val category = Category(id)
        category.name = name
        return category
    }

    private inline fun <T> buildWithArgsCommon(
            subcategory: String,
            constructor: (
                    category: String,
                    filterTag: String,
                    categories: List<Category>,
                    strings: StringsProvider,
                    options: OptionsProvider<Option>,
                    colors: OptionsProvider<ColorItem>
            ) -> T
    ) = constructor(
            subcategory,
            FilterTag.SEARCH_COM,
            categoryProvider["commercial_categories"],
            stringProvider,
            optionsProvider,
            colorsProvider
    )

    private inline fun <T> buildWithArgs(
        constructor: (
            filterTag: String,
            categories: List<Category>,
            strings: StringsProvider,
            options: OptionsProvider<Option>,
            colors: OptionsProvider<ColorItem>
        ) -> T
    ) = constructor(
        FilterTag.SEARCH_COM,
        categoryProvider["commercial_categories"],
        stringProvider,
        optionsProvider,
        colorsProvider
    )

    fun screen(subcategory: String): FilterScreen {
        return when (subcategory) {
            LIGHT_COMMERCIAL_SUB_CATEGORY_ID -> buildWithArgs(CommercialFilterScreen::lightCommercials)
            BUS_SUB_CATEGORY_ID -> buildWithArgs(CommercialFilterScreen::buses)
            TRUCK_SUB_CATEGORY_ID -> buildWithArgs(CommercialFilterScreen::trucks)
            TRUCK_TRACTOR_SUB_CATEGORY_ID -> buildWithArgs(CommercialFilterScreen::truckTractors)
            TRAILER_SUB_CATEGORY_ID -> buildWithArgs(CommercialFilterScreen::trailers)
            AGRICULTURAL_SUBCATEGORY_ID -> buildWithArgs(CommercialFilterScreen::agriculturalVehicles)
            CONSTRUCTION_SUBCATEGORY_ID -> buildWithArgs(CommercialFilterScreen::constructionVehicles)
            DREDGE_SUBCATEGORY_ID -> buildWithArgs(CommercialFilterScreen::dredges)
            CRANE_SUBCATEGORY_ID -> buildWithArgs(CommercialFilterScreen::cranes)
            AUTOLOADER_SUBCATEGORY_ID -> buildWithArgs(CommercialFilterScreen::autoloaders)
            BULLDOZERS_SUBCATEGORY_ID -> buildWithArgs(CommercialFilterScreen::bulldozers)
            MUNICIPAL_SUBCATEGORY_ID -> buildWithArgs(CommercialFilterScreen::municipalVehicles)
            else -> buildWithArgsCommon(subcategory, CommercialFilterScreen::common)
        }
    }

    fun assertMultiSelectParams(params: List<SerializablePair<String, String>>, fieldName: String, value: Set<String>) {
        assertEquals(value.size, params.filter { fieldName == it.first }.size)
        value.forEach {
            Assertions.assertThat(params).containsOnlyOnce(SerializablePair(fieldName, it))
        }
    }
}
