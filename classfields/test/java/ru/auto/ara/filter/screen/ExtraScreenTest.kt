package ru.auto.ara.filter.screen

import com.yandex.mobile.verticalcore.utils.AppHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import ru.auto.ara.RobolectricTest
import ru.auto.ara.consts.Filters
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.filter.viewcontrollers.values.InlineMultiSelectValue
import ru.auto.ara.screens.mapper.FieldWithValueToFieldStateMapper
import ru.auto.ara.screens.mapper.IScreenToFormStateMapper
import ru.auto.ara.screens.mapper.ScreenToFormStateMapper
import ru.auto.ara.util.android.AndroidOptionsProvider
import ru.auto.ara.util.android.StringsProvider
import ru.auto.ara.util.stubIt
import ru.auto.core_ui.util.Consts.AUTO_CATEGORY_ID
import ru.auto.data.model.draft.equipments.EquipmentField
import ru.auto.data.storage.assets.AssetStorage
import ru.auto.test.runner.AllureRobolectricRunner

/**
 * @author aleien on 05.04.17.
 */
@RunWith(AllureRobolectricRunner::class) class ExtraScreenTest : RobolectricTest() {
    lateinit var testedScreen: FilterScreen
    private lateinit var strings: StringsProvider
    private lateinit var options: AndroidOptionsProvider
    private lateinit var dummyOptions: List<Option>
    private lateinit var screenMapper: IScreenToFormStateMapper

    //Attention! to use json asset in test you must put its copy to app/src/test/resources/assets/
    private fun getResourcePath(): String =
        this.javaClass.getResource("/assets/equipment/${Filters.COMPLECTATION_FILTER_CARS}.json")!!.path

    //TODO fix this damn test! (spent 4 hours)
    private val equipmentList by lazy {
        /*
        object: EquipmentRepo(AssetStorage(AppHelper.appContext()), Filters.COMPLECTATION_FILTER_CARS) {
            override fun getEquipmentPath(assetName: String) = getResourcePath("/assets/equipment/$assetName.json")
        }.observeEquipments().toBlocking().value()*/
    }

    @Before
    fun setup() {
        strings = Mockito.mock(StringsProvider::class.java)

        strings.stubIt()

        dummyOptions = listOf(Option("", "Неважно"),
                Option("1", "Вариант 1"),
                Option("2", "Вариант 2"),
                Option("3", "Вариант 4"))

        options = Mockito.mock(AndroidOptionsProvider::class.java)
        Mockito.`when`(options.get(anyString())).thenReturn(dummyOptions)

        screenMapper = ScreenToFormStateMapper(FieldWithValueToFieldStateMapper())

        val equipmentList = AssetStorage(AppHelper.appContext()).readJsonAsset<List<EquipmentField>>(getResourcePath())

        testedScreen = ExtraScreen.Builder(
                categoryId = AUTO_CATEGORY_ID,
                equipmentList = equipmentList,
                strings = strings,
                options = options,
                screenToFormStateMapper = screenMapper
        ).build("extra")
    }

    @Ignore
    @Test
    fun emptyFilter_isDefault_true() {
        assertThat(testedScreen.isDefault).isTrue()
    }

    @Ignore
    @Test
    fun modifiedFilter_isDefault_false() {
        val select = InlineMultiSelectValue(true, 1, false)
        testedScreen.getValueFieldById<InlineMultiSelectValue>("upholstery_group").value = select
        assertThat(testedScreen.isDefault).isFalse()
    }

    @Ignore
    @Test
    fun emptyFilter_getSearchParams_null() {
        assertThat(testedScreen.searchParams).isEmpty()
    }
}
