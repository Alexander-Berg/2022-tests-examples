package com.yandex.mobile.realty.filters.dependency

import com.yandex.mobile.realty.domain.Rubric
import com.yandex.mobile.realty.domain.model.geo.GeoIntent
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import com.yandex.mobile.realty.ui.filter.model.GeoFieldValue
import com.yandex.mobile.vertical.dynamicscreens.model.BaseScreen
import com.yandex.mobile.vertical.dynamicscreens.model.field.FieldWithValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * @author rogovalex on 09.05.18.
 */
class RegionDependencyTest {

    @Test
    fun testIfAbsentHideStrategy() {
        val rubricProvider = mock(RubricProvider::class.java)
        val strategy = RegionDependency.IfAbsent(rubricProvider, "id1", "id2")
        assertFalse(strategy.shouldHideField(setOf("id1")))
        assertFalse(strategy.shouldHideField(setOf("id2")))
        assertFalse(strategy.shouldHideField(setOf("id1", "id2")))
        assertTrue(strategy.shouldHideField(emptySet()))
    }

    @Test
    fun testIfAnyAbsentHideStrategy() {
        val rubricProvider = mock(RubricProvider::class.java)
        val strategy = RegionDependency.IfAnyAbsent(rubricProvider, "id1", "id2")
        assertTrue(strategy.shouldHideField(setOf("id1")))
        assertTrue(strategy.shouldHideField(setOf("id2")))
        assertFalse(strategy.shouldHideField(setOf("id1", "id2")))
        assertTrue(strategy.shouldHideField(emptySet()))
    }

    @Test
    fun testDividerHideStrategy() {
        val rubricProvider = mock(RubricProvider::class.java)
        val strategy = RegionDependency.Divider(rubricProvider, "idBefore", "idAfter1", "idAfter2")
        assertTrue(strategy.shouldHideField(setOf("idAfter1", "idAfter2")))
        assertTrue(strategy.shouldHideField(setOf("idAfter1")))
        assertTrue(strategy.shouldHideField(setOf("idAfter2")))
        assertTrue(strategy.shouldHideField(setOf("idBefore")))
        assertFalse(strategy.shouldHideField(setOf("idBefore", "idAfter1", "idAfter2")))
        assertFalse(strategy.shouldHideField(setOf("idBefore", "idAfter2")))
        assertFalse(strategy.shouldHideField(setOf("idBefore", "idAfter1")))
        assertTrue(strategy.shouldHideField(emptySet()))
    }

    @Test
    fun testRegionDependency() {
        val geoFieldValue = GeoFieldValue(
            GeoIntent.Objects.valueOf(GeoRegion.DEFAULT),
            GeoRegion.DEFAULT,
            RegionParams(
                0,
                0,
                "",
                emptyMap(),
                mapOf(Rubric.APARTMENT_SELL to setOf("id")),
                null,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                0,
                null
            ),
            null
        )

        val geoField = mock(FieldWithValue::class.java)
        `when`(geoField.id).thenReturn("geo")
        `when`(geoField.value).thenReturn(geoFieldValue)

        val baseScreen = BaseScreen("test", listOf(geoField))

        val rubricProvider = object : RubricProvider {
            override fun getRubric(screen: BaseScreen) = Rubric.APARTMENT_SELL
        }

        val dependency = RegionDependency(
            "geo",
            mapOf(
                "dependentFieldId" to RegionDependency.IfAbsent(rubricProvider, "id"),
                "otherDependentFieldId" to RegionDependency.IfAbsent(rubricProvider, "otherId")
            )
        )

        assertFalse(dependency.shouldHideField(baseScreen, "dependentFieldId"))
        assertTrue(dependency.shouldHideField(baseScreen, "otherDependentFieldId"))
    }
}
