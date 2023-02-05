package com.edadeal.android.ui.offers

import com.edadeal.android.model.OffersQuery
import com.edadeal.android.model.SortType
import com.edadeal.android.model.entity.Compilation
import com.edadeal.android.model.entity.Entity
import com.edadeal.android.ui.compilations.CompilationsController
import okio.ByteString.Companion.decodeHex
import org.junit.Test
import kotlin.test.assertEquals

class OffersControllerTest {

    @Test
    fun testOffersControllerQuery() {
        val shopId = "0A".decodeHex()
        val retailerId = "0B".decodeHex()
        val compilationId = "0C".decodeHex()
        val subCompilationId = "0D".decodeHex()
        val citySlug = "city"
        val retailerSlug = "retailer"
        val emptyQuery = OffersQuery(
            citySlug = "",
            retailerSlug = "",
            segmentSlug = "",
            compilationSlug = "",
            segmentId = Entity.EMPTY_ID,
            compilationId = Entity.EMPTY_ID,
            updateModCount = 0,
            sortType = SortType.Default,
            groupByRetailer = false,
            isOnlyFavorite = false,
            shopIds = emptySet(),
            retailerIds = emptySet(),
            brandIds = emptySet(),
            segmentIds = emptySet(),
            compilationIds = emptySet(),
            subCompilationId = Entity.EMPTY_ID,
            offerIds = emptySet(),
            relatedSubCompilationIds = emptyList(),
            deeplinkModCount = 0
        )
        val shopQuery = emptyQuery.copy(shopIds = setOf(shopId))
        val retailerQuery = emptyQuery.copy(retailerIds = setOf(retailerId))
        val retailerWithSlugQuery = emptyQuery.copy(citySlug = citySlug, retailerSlug = retailerSlug)
        val compilationsQuery = emptyQuery.copy(
            compilationIds = setOf(compilationId), subCompilationId = subCompilationId, groupByRetailer = true
        )
        assertEquals(shopQuery, OffersController.forShop(shopId).getQuery(emptyQuery))
        assertEquals(retailerQuery, OffersController.forRetailer(retailerId).getQuery(emptyQuery))
        val controllerForRetailerWithSlug = OffersController.forRetailerWithSlug(retailerSlug, citySlug)
        assertEquals(retailerWithSlugQuery, controllerForRetailerWithSlug.getQuery(emptyQuery))
        val compilationsController = CompilationsController().apply { compilationIds = setOf(compilationId) }
        val controllerForCompilation = OffersController.forCompilation(
            compilationsController, Compilation.EMPTY.copy(id = subCompilationId)
        )
        assertEquals(compilationsQuery, controllerForCompilation.getQuery(emptyQuery))
    }
}
