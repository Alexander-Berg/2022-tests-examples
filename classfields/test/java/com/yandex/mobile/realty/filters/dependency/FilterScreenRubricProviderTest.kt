package com.yandex.mobile.realty.filters.dependency

import androidx.core.util.Pair
import com.yandex.mobile.realty.domain.Rubric
import com.yandex.mobile.realty.entities.filters.ApartmentCategory
import com.yandex.mobile.realty.entities.filters.DealType
import com.yandex.mobile.realty.entities.filters.HouseCategory
import com.yandex.mobile.realty.entities.filters.LotCategory
import com.yandex.mobile.realty.entities.filters.PropertyType
import com.yandex.mobile.realty.filters.screen.FilterScreen.FieldIds.APARTMENT_CATEGORY
import com.yandex.mobile.realty.filters.screen.FilterScreen.FieldIds.DEAL_PROPERTY_TYPE
import com.yandex.mobile.realty.filters.screen.FilterScreen.FieldIds.HOUSE_CATEGORY
import com.yandex.mobile.realty.filters.screen.FilterScreen.FieldIds.LOT_CATEGORY
import com.yandex.mobile.vertical.dynamicscreens.model.BaseScreen
import com.yandex.mobile.vertical.dynamicscreens.model.field.FieldWithValue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * @author rogovalex on 09.05.18.
 */
class FilterScreenRubricProviderTest {

    @Test(expected = NullPointerException::class)
    fun shouldThrowWhenDealPropertyFieldIsMissing() {
        val baseScreen = BaseScreen("test", emptyList())
        val rubricProvider = FilterScreenRubricProvider
        rubricProvider.getRubric(baseScreen)
    }

    @Test(expected = NullPointerException::class)
    fun shouldThrowWhenDealPropertyValueIsNull() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(null)

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        rubricProvider.getRubric(baseScreen)
    }

    @Test
    fun testRubricProviderForBuyRoom() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.ROOM))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.ROOMS_SELL, rubricProvider.getRubric(baseScreen))
    }

    @Test(expected = NullPointerException::class)
    fun shouldThrowWhenHouseCategoryFieldIsMissing() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.HOUSE))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        rubricProvider.getRubric(baseScreen)
    }

    @Test
    fun testRubricProviderForBuyAnyHouse() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.HOUSE))

        val houseCategoryField = mock(FieldWithValue::class.java)
        `when`(houseCategoryField.id).thenReturn(HOUSE_CATEGORY)
        `when`(houseCategoryField.value).thenReturn(HouseCategory.ANY)

        val baseScreen = BaseScreen("test", listOf(dealPropertyField, houseCategoryField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.HOUSE_SELL, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForBuySecondaryHouse() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.HOUSE))

        val houseCategoryField = mock(FieldWithValue::class.java)
        `when`(houseCategoryField.id).thenReturn(HOUSE_CATEGORY)
        `when`(houseCategoryField.value).thenReturn(HouseCategory.SECONDARY)

        val baseScreen = BaseScreen("test", listOf(dealPropertyField, houseCategoryField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.HOUSE_SELL, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForBuyVillageHouse() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.HOUSE))

        val houseCategoryField = mock(FieldWithValue::class.java)
        `when`(houseCategoryField.id).thenReturn(HOUSE_CATEGORY)
        `when`(houseCategoryField.value).thenReturn(HouseCategory.VILLAGE)

        val baseScreen = BaseScreen("test", listOf(dealPropertyField, houseCategoryField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.HOUSE_SELL_VILLAGE, rubricProvider.getRubric(baseScreen))
    }

    @Test(expected = NullPointerException::class)
    fun shouldThrowWhenLotCategoryFieldIsMissing() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.AREA))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        rubricProvider.getRubric(baseScreen)
    }

    @Test
    fun testRubricProviderForBuyAnyLot() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.AREA))

        val lotCategoryField = mock(FieldWithValue::class.java)
        `when`(lotCategoryField.id).thenReturn(LOT_CATEGORY)
        `when`(lotCategoryField.value).thenReturn(LotCategory.ANY)

        val baseScreen = BaseScreen("test", listOf(dealPropertyField, lotCategoryField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.LOT_SELL, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForBuySecondaryLot() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.AREA))

        val lotCategoryField = mock(FieldWithValue::class.java)
        `when`(lotCategoryField.id).thenReturn(LOT_CATEGORY)
        `when`(lotCategoryField.value).thenReturn(LotCategory.SECONDARY)

        val baseScreen = BaseScreen("test", listOf(dealPropertyField, lotCategoryField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.LOT_SELL, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForBuyVillageLot() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.AREA))

        val lotCategoryField = mock(FieldWithValue::class.java)
        `when`(lotCategoryField.id).thenReturn(LOT_CATEGORY)
        `when`(lotCategoryField.value).thenReturn(LotCategory.VILLAGE)

        val baseScreen = BaseScreen("test", listOf(dealPropertyField, lotCategoryField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.LOT_SELL_VILLAGE, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForBuyCommercial() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.COMMERCIAL))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.COMMERCIAL_SELL, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForBuyGarage() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.GARAGE))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.GARAGE_SELL, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForRentApartment() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.RENT, PropertyType.APARTMENT))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.APARTMENT_RENT, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForRentRoom() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.RENT, PropertyType.ROOM))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.ROOMS_RENT, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForRentHouse() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.RENT, PropertyType.HOUSE))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.HOUSE_RENT, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForRentCommercial() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.RENT, PropertyType.COMMERCIAL))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.COMMERCIAL_RENT, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForRentGarage() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.RENT, PropertyType.GARAGE))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.GARAGE_RENT, rubricProvider.getRubric(baseScreen))
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowWhenUnknownDealPropertyCombination() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.RENT, PropertyType.AREA))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        rubricProvider.getRubric(baseScreen)
    }

    @Test(expected = NullPointerException::class)
    fun shouldThrowWhenApartmentCategoryFieldIsMissing() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.APARTMENT))

        val baseScreen = BaseScreen("test", listOf(dealPropertyField))
        val rubricProvider = FilterScreenRubricProvider
        rubricProvider.getRubric(baseScreen)
    }

    @Test(expected = NullPointerException::class)
    fun shouldThrowWhenApartmentCategoryValueIsNull() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.APARTMENT))

        val apartmentCategoryField = mock(FieldWithValue::class.java)
        `when`(apartmentCategoryField.id).thenReturn(APARTMENT_CATEGORY)
        `when`(apartmentCategoryField.value).thenReturn(null)

        val baseScreen = BaseScreen("test", listOf(dealPropertyField, apartmentCategoryField))
        val rubricProvider = FilterScreenRubricProvider
        rubricProvider.getRubric(baseScreen)
    }

    @Test
    fun testRubricProviderForBuyAnyApartment() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.APARTMENT))

        val apartmentCategoryField = mock(FieldWithValue::class.java)
        `when`(apartmentCategoryField.id).thenReturn(APARTMENT_CATEGORY)
        `when`(apartmentCategoryField.value).thenReturn(ApartmentCategory.ANY)

        val baseScreen = BaseScreen("test", listOf(dealPropertyField, apartmentCategoryField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.APARTMENT_SELL, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForBuyOldApartment() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.APARTMENT))

        val apartmentCategoryField = mock(FieldWithValue::class.java)
        `when`(apartmentCategoryField.id).thenReturn(APARTMENT_CATEGORY)
        `when`(apartmentCategoryField.value).thenReturn(ApartmentCategory.OLD)

        val baseScreen = BaseScreen("test", listOf(dealPropertyField, apartmentCategoryField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.APARTMENT_SELL, rubricProvider.getRubric(baseScreen))
    }

    @Test
    fun testRubricProviderForBuyNewBuildingApartment() {
        val dealPropertyField = mock(FieldWithValue::class.java)
        `when`(dealPropertyField.id).thenReturn(DEAL_PROPERTY_TYPE)
        `when`(dealPropertyField.value).thenReturn(Pair(DealType.BUY, PropertyType.APARTMENT))

        val apartmentCategoryField = mock(FieldWithValue::class.java)
        `when`(apartmentCategoryField.id).thenReturn(APARTMENT_CATEGORY)
        `when`(apartmentCategoryField.value).thenReturn(ApartmentCategory.NEW_BUILDING)

        val baseScreen = BaseScreen("test", listOf(dealPropertyField, apartmentCategoryField))
        val rubricProvider = FilterScreenRubricProvider
        assertEquals(Rubric.APARTMENT_SELL_NEWBUILDING, rubricProvider.getRubric(baseScreen))
    }
}
