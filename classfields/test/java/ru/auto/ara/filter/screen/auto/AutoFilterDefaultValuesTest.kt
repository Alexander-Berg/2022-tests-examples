package ru.auto.ara.filter.screen.auto

import com.yandex.mobile.vertical.dynamicscreens.model.field.FieldWithValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.consts.Filters
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.filter.fields.GeoField
import ru.auto.ara.filter.fields.MultiGeoField
import ru.auto.ara.filter.screen.MultiGeoValue
import ru.auto.ara.network.State
import ru.auto.ara.network.StateBeaten
import ru.auto.ara.network.Wheel
import ru.auto.ara.util.SerializablePair
import ru.auto.core_ui.util.Consts
import ru.auto.data.model.geo.SuggestGeoItem
import ru.auto.test.runner.AllureRobolectricRunner


@RunWith(AllureRobolectricRunner::class)
class AutoFilterDefaultValuesTest : AutoFilterTest() {

    @Test
    fun emptyFilter_getSearchParams_defaultAutoParams() {
        val params = testedScreen.searchParams
        assertThat(params).isNotNull
            .containsExactlyInAnyOrder(SerializablePair(Filters.STATE_FIELD, State.ALL),
                SerializablePair(Filters.WHEEL_FIELD, Wheel.ANY),
                SerializablePair(Filters.SELLER_FIELD, "3"),
                SerializablePair(Filters.BEATEN_FIELD, StateBeaten.NOT_BEATEN),
                SerializablePair(Filters.CATEGORY_FIELD, "15"),
                SerializablePair(Filters.PHOTO_FIELD, "1"),
                SerializablePair(Filters.CUSTOM_FIELD, "1"),
                SerializablePair(Filters.WITHOUT_DELIVERY_FIELD, "BOTH"),
                SerializablePair(Filters.WITH_DISCOUNT_FIELD, "true"),
            )
    }

    @Test
    fun setUsedState_getSearchParams_containsOnlyUsed() {
        testedScreen.getValueFieldById<String>(Filters.STATE_FIELD).value = State.USED

        val params = testedScreen.searchParams
        assertThat(params).isNotNull
            .containsExactlyInAnyOrder(SerializablePair(Filters.STATE_FIELD, State.USED),
                SerializablePair(Filters.WHEEL_FIELD, Wheel.ANY),
                SerializablePair(Filters.SELLER_FIELD, "3"),
                SerializablePair(Filters.BEATEN_FIELD, StateBeaten.NOT_BEATEN),
                SerializablePair(Filters.CATEGORY_FIELD, "15"),
                SerializablePair(Filters.PHOTO_FIELD, "1"),
                SerializablePair(Filters.CUSTOM_FIELD, "1"),
                SerializablePair(Filters.WITHOUT_DELIVERY_FIELD, "BOTH"),
                SerializablePair(Filters.WITH_DISCOUNT_FIELD, "true"),
            )

    }

    @Test
    fun setNewState_getSearchParams_containsOnlyNew() {
        testedScreen.getValueFieldById<String>(Filters.STATE_FIELD).value = State.NEW

        val params = testedScreen.searchParams

        assertThat(params).isNotNull
            .contains(SerializablePair(Filters.STATE_FIELD, State.NEW))
            .doesNotContain(SerializablePair(Filters.BEATEN_FIELD, StateBeaten.BEATEN))
    }

    @Test
    fun setBeaten_getSearchParams_stateBeaten() {
        testedScreen.getValueFieldById<Option>(Filters.BEATEN_FIELD).value = Option(StateBeaten.BEATEN, "Битые")

        val params = testedScreen.searchParams
        assertThat(params).isNotNull
            .containsExactlyInAnyOrder(SerializablePair(Filters.BEATEN_FIELD, StateBeaten.BEATEN),
                SerializablePair(Filters.WHEEL_FIELD, Wheel.ANY),
                SerializablePair(Filters.SELLER_FIELD, "3"),
                SerializablePair(Filters.CATEGORY_FIELD, "15"),
                SerializablePair(Filters.PHOTO_FIELD, "1"),
                SerializablePair(Filters.CUSTOM_FIELD, "1"),
                SerializablePair(Filters.WITHOUT_DELIVERY_FIELD, "BOTH"),
                SerializablePair(Filters.WITH_DISCOUNT_FIELD, "true"),
            )

    }

    @Test
    fun setNewThanBeaten_getSearchParams_stateBeaten() {
        testedScreen.getValueFieldById<String>(Filters.STATE_FIELD).value = State.NEW
        testedScreen.getValueFieldById<Option>(Filters.BEATEN_FIELD).value = Option(StateBeaten.BEATEN, "Битые")

        val params = testedScreen.searchParams
        assertThat(params).isNotNull
            .containsExactlyInAnyOrder(SerializablePair(Filters.BEATEN_FIELD, StateBeaten.BEATEN),
                SerializablePair(Filters.WHEEL_FIELD, Wheel.ANY),
                SerializablePair(Filters.SELLER_FIELD, "3"),
                SerializablePair(Filters.CATEGORY_FIELD, "15"),
                SerializablePair(Filters.PHOTO_FIELD, "1"),
                SerializablePair(Filters.CUSTOM_FIELD, "1"),
                SerializablePair(Filters.WITHOUT_DELIVERY_FIELD, "BOTH"),
                SerializablePair(Filters.WITH_DISCOUNT_FIELD, "true"),
            )
    }

    @Test
    fun setBeatenThanNew_getSearchParams_stateNew() {
        testedScreen.getValueFieldById<Option>(Filters.BEATEN_FIELD).value = Option(StateBeaten.BEATEN, "Битые")
        testedScreen.getValueFieldById<String>(Filters.STATE_FIELD).value = State.NEW

        val params = testedScreen.searchParams

        assertThat(params).isNotNull
            .contains(SerializablePair(Filters.STATE_FIELD, State.NEW))
            .doesNotContain(SerializablePair(Filters.BEATEN_FIELD, StateBeaten.BEATEN))
    }

    @Test
    fun setNotBeaten_getSearchParams_previousStateRestored() {
        testedScreen.getValueFieldById<Option>(Filters.BEATEN_FIELD).value = Option(StateBeaten.NOT_BEATEN, "Кроме битых")

        val params = testedScreen.searchParams
        assertThat(params).isNotNull
            .containsExactlyInAnyOrder(
                SerializablePair(Filters.STATE_FIELD, State.ALL),
                SerializablePair(Filters.CATEGORY_FIELD, "15"),
                SerializablePair(Filters.WHEEL_FIELD, Wheel.ANY),
                SerializablePair(Filters.SELLER_FIELD, "3"),
                SerializablePair(Filters.BEATEN_FIELD, StateBeaten.NOT_BEATEN),
                SerializablePair(Filters.PHOTO_FIELD, "1"),
                SerializablePair(Filters.CUSTOM_FIELD, "1"),
                SerializablePair(Filters.WITHOUT_DELIVERY_FIELD, "BOTH"),
                SerializablePair(Filters.WITH_DISCOUNT_FIELD, "true"),
            )
    }

    @Test
    fun emptyScreen_isDefault_returnTrue() {
        assertThat(testedScreen.isDefault).isTrue()
    }

    @Test
    fun modifiedScreen_isDefault_returnFalse() {
        testedScreen.getValueFieldById<String>(Filters.STATE_FIELD).value = State.USED
        assertThat(testedScreen.isDefault).isFalse()
    }

    @Test
    fun modifyNonCleanableFields_isDefault_returnTrue() {
        //val geoItem = SuggestGeoItem(GeoItem("213", "Москва", GeoItem.Type.CITY, true))
        val field: FieldWithValue<MultiGeoValue> = testedScreen.getValueFieldById<MultiGeoValue>(Filters.GEO_FIELD)
        field.value = MultiGeoField.DEFAULT_VALUE //SerializablePair(geoItem, Consts.DEFAULT_RADIUS_KM)

        assertThat(testedScreen.isDefault).isTrue()
    }

    @Test
    fun cleanModifiedFilter_isDefault_returnTrue() {
        testedScreen.getValueFieldById<String>(Filters.STATE_FIELD).value = State.USED
        testedScreen.clear()

        assertThat(testedScreen.isDefault).isTrue()
    }

    @Test
    fun cleanModifiedNonCleanableFields_getFieldValue_valueNotCleaned() {
        val geoItem = SerializablePair(SuggestGeoItem("213", "Москва", null, true), Consts.DEFAULT_RADIUS_KM)
        testedScreen.getValueFieldById<SerializablePair<SuggestGeoItem, Int>>(Filters.GEO_FIELD).value = geoItem

        testedScreen.clear()
        assertThat(testedScreen.getValueFieldById<SerializablePair<SuggestGeoItem, Int>>(Filters.GEO_FIELD).value)
            .isNotNull()
            .isEqualTo(geoItem)
    }

    @Test
    fun emptyScreen_getNotDefaultFields_returnZero() {
        assertThat(testedScreen.nonDefaultFieldsNumber).isEqualTo(0)
    }

    @Test
    fun modifiedScreen_getNotDefaultFields_returnOne() {
        testedScreen.getValueFieldById<String>(Filters.STATE_FIELD).value = State.USED
        assertThat(testedScreen.nonDefaultFieldsNumber).isEqualTo(1)
    }

    @Test
    fun modifyNonCleanableFields_getNotDefaultFields_returnZero() {
        //val geoItem = SuggestGeoItem(GeoItem("213", "Москва", GeoItem.Type.CITY, true))
        testedScreen.getValueFieldById<SerializablePair<SuggestGeoItem, Int>>(Filters.GEO_FIELD)
            .value = GeoField.DEFAULT_ITEM //SerializablePair(geoItem, Consts.DEFAULT_RADIUS_KM)

        assertThat(testedScreen.nonDefaultFieldsNumber).isEqualTo(0)
    }
}
