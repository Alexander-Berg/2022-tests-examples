package com.yandex.mobile.realty.ui.presenter

import com.yandex.mobile.realty.RobolectricTest
import com.yandex.mobile.realty.domain.model.site.HousingType
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * @author sorokinandrei on 3/30/21.
 */
@RunWith(RobolectricTestRunner::class)
class HousingTypeTest : RobolectricTest() {

    @Test
    fun testLabelRes() {
        assertThat(context.getString(null.labelRes), equalTo("Квартиры"))
        assertThat(context.getString(HousingType.FLATS.labelRes), equalTo("Квартиры"))
        assertThat(
            context.getString(HousingType.APARTMENTS_AND_FLATS.labelRes),
            equalTo("Квартиры и апартаменты")
        )
        assertThat(context.getString(HousingType.APARTMENTS.labelRes), equalTo("Апартаменты"))
    }

    @Test
    fun testPrimaryOffersLabelRes() {
        assertThat(context.getString(null.primaryOffersLabelRes), containsString("квартир"))
        assertThat(
            context.getString(HousingType.FLATS.primaryOffersLabelRes),
            containsString("квартир")
        )
        assertThat(
            context.getString(HousingType.APARTMENTS.primaryOffersLabelRes),
            containsString("апартаментов")
        )
        assertThat(
            context.getString(HousingType.APARTMENTS_AND_FLATS.primaryOffersLabelRes),
            containsString("квартир и апартаментов")
        )
    }

    @Test
    fun pluralsRes() {
        val resources = context.resources
        assertThat(resources.getQuantityString(null.pluralsRes, 1, 1), containsString("квартир"))
        assertThat(resources.getQuantityString(null.pluralsRes, 10, 10), containsString("квартир"))

        assertThat(
            resources.getQuantityString(HousingType.FLATS.pluralsRes, 1, 1),
            containsString("квартир")
        )
        assertThat(
            resources.getQuantityString(HousingType.FLATS.pluralsRes, 10, 10),
            containsString("квартир")
        )

        assertThat(
            resources.getQuantityString(HousingType.APARTMENTS.pluralsRes, 1, 1),
            containsString("апартамент")
        )
        assertThat(
            resources.getQuantityString(HousingType.APARTMENTS.pluralsRes, 10, 10),
            containsString("апартамент")
        )

        assertThat(
            resources.getQuantityString(HousingType.APARTMENTS_AND_FLATS.pluralsRes, 1, 1),
            containsString("предложени")
        )
        assertThat(
            resources.getQuantityString(HousingType.APARTMENTS_AND_FLATS.pluralsRes, 10, 10),
            containsString("предложени")
        )
    }

    @Test
    fun testTotalLabelRes() {
        assertThat(context.getString(null.totalLabelRes), containsString("квартир"))
        assertThat(context.getString(HousingType.FLATS.totalLabelRes), containsString("квартир"))
        assertThat(
            context.getString(HousingType.APARTMENTS_AND_FLATS.totalLabelRes),
            equalTo("Квартиры и апартаменты")
        )
        assertThat(
            context.getString(HousingType.APARTMENTS.totalLabelRes),
            containsString("апартаментов")
        )
    }

    @Test
    fun testOffersFromDeveloperLabelRes() {
        assertThat(context.getString(null.offersFromDeveloperLabelRes), containsString("квартиры"))
        assertThat(
            context.getString(HousingType.FLATS.offersFromDeveloperLabelRes),
            containsString("квартиры")
        )
        assertThat(
            context.getString(HousingType.APARTMENTS_AND_FLATS.offersFromDeveloperLabelRes),
            containsString("предложения")
        )
        assertThat(
            context.getString(HousingType.APARTMENTS.offersFromDeveloperLabelRes),
            containsString("апартаменты")
        )
    }
}
