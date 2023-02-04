package ru.auto.ara.filter.screen

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import ru.auto.ara.RobolectricTest
import ru.auto.ara.consts.Filters
import ru.auto.ara.data.entities.form.ColorItem
import ru.auto.ara.data.entities.form.Field
import ru.auto.ara.data.models.form.state.FieldState
import ru.auto.ara.data.models.form.state.SimpleState
import ru.auto.ara.filter.communication.FilterTag
import ru.auto.ara.util.DummyOptions
import ru.auto.ara.util.SerializablePair
import ru.auto.ara.util.android.AndroidOptionsProvider
import ru.auto.ara.util.android.ColorOptionsProvider
import ru.auto.ara.util.android.OptionsProvider
import ru.auto.ara.util.android.StringsProvider
import ru.auto.ara.util.stubIt
import ru.auto.core_ui.util.Consts
import ru.auto.data.model.geo.SuggestGeoItem
import ru.auto.test.runner.AllureRobolectricRunner

/**
 * @author aleien on 15.05.17.
 */
@RunWith(AllureRobolectricRunner::class)
class AutoExtraScreenTest : RobolectricTest() {
    private lateinit var testedScreen: FilterScreen
    private lateinit var stringProvider: StringsProvider
    private lateinit var optionsProvider: AndroidOptionsProvider
    private lateinit var colorsProvider: OptionsProvider<ColorItem>

    private var dummyGeoState: SerializablePair<SuggestGeoItem, Int>? = null



    @Before
    fun setUp() {
        stringProvider = Mockito.mock(StringsProvider::class.java)
        optionsProvider = Mockito.mock(AndroidOptionsProvider::class.java)
        colorsProvider = Mockito.mock(ColorOptionsProvider::class.java)
        stringProvider.stubIt()

        dummyGeoState = SerializablePair(SuggestGeoItem("123", "Moskovia", "Rossia", true), 200)

        DummyOptions.applyDummyOptions(optionsProvider)

        testedScreen = AutoFilterScreen.Builder(
            rootCategory = Consts.AUTO_CATEGORY_ID,
            strings = stringProvider,
            options = optionsProvider,
            colors = colorsProvider,
        ).build(FilterTag.SEARCH_AUTO)
    }

    @Test
    fun `extra field should convert to list of params`() {
        val extras = mapOf(
            "extras_upholstery" to SimpleState(Field.TYPES.checkbox).apply {
                fieldName = "extras_upholstery"
                value = "1"
            },
            "extras_climate" to SimpleState(Field.TYPES.checkbox).apply {
                fieldName = "extras_climate"
                value = "1"
            },
            Filters.EXTRAS_ABS to SimpleState(Field.TYPES.checkbox).apply {
                fieldName = Filters.EXTRAS_ABS
                value = "1"
            }
        )

        testedScreen.getValueFieldById<Map<String, FieldState>>(Filters.EXTRAS_FIELD).value = extras

        val params = testedScreen.searchParams
        Assertions.assertThat(params).containsOnlyOnce(
            SerializablePair(Filters.CATALOG_EQUIPMENT, "extras_upholstery"),
            SerializablePair(Filters.CATALOG_EQUIPMENT, "extras_climate"),
            SerializablePair(Filters.CATALOG_EQUIPMENT, Filters.EXTRAS_ABS)
        )
    }

}
