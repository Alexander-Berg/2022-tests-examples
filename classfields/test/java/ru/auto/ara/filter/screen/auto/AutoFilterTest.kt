package ru.auto.ara.filter.screen.auto

import org.assertj.core.api.Assertions
import org.junit.Before
import org.mockito.Mockito
import ru.auto.ara.RobolectricTest
import ru.auto.ara.data.entities.form.ColorItem
import ru.auto.ara.filter.communication.FilterTag
import ru.auto.ara.filter.screen.AutoFilterScreen
import ru.auto.ara.filter.screen.FilterScreen
import ru.auto.ara.util.DummyOptions
import ru.auto.ara.util.SerializablePair
import ru.auto.ara.util.android.AndroidOptionsProvider
import ru.auto.ara.util.android.ColorOptionsProvider
import ru.auto.ara.util.android.OptionsProvider
import ru.auto.ara.util.android.StringsProvider
import ru.auto.ara.util.stubIt
import ru.auto.core_ui.util.Consts

/**
 * @author aleien on 15.05.17.
 */
abstract class AutoFilterTest : RobolectricTest() {
    protected lateinit var testedScreen: FilterScreen
    protected lateinit var stringProvider: StringsProvider
    protected lateinit var optionsProvider: AndroidOptionsProvider
    protected lateinit var colorsProvider: OptionsProvider<ColorItem>

    @Before
    fun setUp() {
        stringProvider = Mockito.mock(StringsProvider::class.java)
        optionsProvider = Mockito.mock(AndroidOptionsProvider::class.java)
        colorsProvider = Mockito.mock(ColorOptionsProvider::class.java)
        stringProvider.stubIt()

        DummyOptions.applyDummyOptions(optionsProvider)

        testedScreen = AutoFilterScreen.Builder(
            rootCategory = Consts.AUTO_CATEGORY_ID,
            strings = stringProvider,
            options = optionsProvider,
            colors = colorsProvider,
        ).build(FilterTag.SEARCH_AUTO)
    }

    fun assertMultiSelectParams(params: List<SerializablePair<String, String>>, fieldName: String, value: Set<String>) {
        assert(value.size <= params.filter { fieldName == it.first }.size) //<= because of children
        value.forEach {
            Assertions.assertThat(params).containsOnlyOnce(SerializablePair(fieldName, it))
        }
    }
}
