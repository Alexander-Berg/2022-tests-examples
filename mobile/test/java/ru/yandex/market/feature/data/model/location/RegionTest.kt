package ru.yandex.market.feature.data.model.location

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.yandex.market.feature.data.repository.geo.GeoConstants

class RegionTest {

    @Test
    fun getDescription() {
        assertEquals(
            "Хмельницкая область, Украина",
            Region(
                id = "27713", name = "Красилов",
                fullName = "Красилов (Хмельницкая область, Украина)",
                countryId = GeoConstants.CountryId.UKRAINE
            ).description
        )

        assertEquals(
            null,
            Region(
                id = "27713", name = "Красилов",
                fullName = "Красилов",
                countryId = GeoConstants.CountryId.UKRAINE
            ).description
        )

        assertEquals(
            null,
            Region(
                id = "27713", name = "Красилов",
                fullName = null,
                countryId = GeoConstants.CountryId.UKRAINE
            ).description
        )
    }
}