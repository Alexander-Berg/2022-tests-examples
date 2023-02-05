package com.edadeal.android.model

import com.edadeal.android.data.ContentRepository
import com.edadeal.android.data.room.dao.misc.CatalogViewDao
import com.edadeal.android.data.room.dao.misc.RetailerViewDao
import com.edadeal.android.data.room.entity.misc.RetailerViewDb
import com.edadeal.android.model.catalogvisit.CatalogVisitMigrationDelegate
import com.edadeal.android.model.catalogvisit.CatalogVisitRepository
import com.edadeal.android.model.entity.Point
import com.edadeal.android.model.entity.Retailer
import com.edadeal.android.model.home.NearestShopInfo
import com.edadeal.android.model.home.RetailerInfo
import com.nhaarman.mockito_kotlin.whenever
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mock
import org.mockito.MockitoAnnotations.initMocks
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(Parameterized::class)
class CatalogBadgeUseCaseTest(
    private val retailerViewsDbs: List<RetailerViewDb>,
    private val retailerInfo: RetailerInfo,
    private val expected: Boolean
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Any> = listOf(
            arrayOf(emptyList<RetailerViewDb>(), getRetailerInfo(), false),
            arrayOf(listOf(getRetailerViewDb(1)), getRetailerInfo(shopLastUpdated = 2,
                shopIsFavorite = true), true),
            arrayOf(listOf(getRetailerViewDb(1, shopUuid = "")), getRetailerInfo(retailerLastUpdated = 2), true),
            arrayOf(listOf(getRetailerViewDb(1)), getRetailerInfo(retailerLastUpdated = 1), false),
            arrayOf(listOf(getRetailerViewDb(2, shopUuid = "")), getRetailerInfo(retailerLastUpdated = 1), false),
            arrayOf(listOf(getRetailerViewDb(1)), getRetailerInfo(retailerLastUpdated = 1,
                shopLastUpdated = 1, shopIsFavorite = true), false),
            arrayOf(listOf(getRetailerViewDb(1)), getRetailerInfo(shopIsFavorite = true, shopLastUpdated = 2), true),
            arrayOf(listOf(getRetailerViewDb(2)), getRetailerInfo(shopIsFavorite = true, shopLastUpdated = 1), false)
        )

        private fun getRetailerInfo(retailerLastUpdated: Long? = null, shopLastUpdated: Long? = null,
                                    shopIsFavorite: Boolean = false) = RetailerInfo(
            retailer = Retailer.EMPTY,
            offersLastUpdated = retailerLastUpdated,
            deepLink = null,
            promoLabel = null,
            isFavorite = false,
            catalogCoverUrl = null,
            hasCatalogsWithOffers = false,
            nearestShop = NearestShopInfo(
                id = ByteString.EMPTY,
                position = Point(0.0, 0.0),
                distance = 0.0,
                isFavorite = shopIsFavorite,
                isDistanceReal = false,
                offersLastUpdated = shopLastUpdated
            )
        )

        private fun getRetailerViewDb(timestamp: Long, shopUuid: String = ByteString.EMPTY.toUuidString()) = RetailerViewDb(
            uuid = ByteString.EMPTY.toUuidString(),
            timestamp = timestamp,
            shopUuid = shopUuid
        )
    }

    @Mock
    lateinit var contentRepository: ContentRepository
    @Mock
    lateinit var catalogViewDao: CatalogViewDao
    @Mock
    lateinit var retailerViewDao: RetailerViewDao
    @Mock
    lateinit var catalogVisitRepository: CatalogVisitRepository
    @Mock
    lateinit var catalogVisitMigrationDelegate: CatalogVisitMigrationDelegate
    private lateinit var time: Time
    private lateinit var catalogBadgeUseCase: CatalogBadgeUseCase

    @BeforeTest
    fun setUp() {
        initMocks(this)
        time = Time()
        catalogBadgeUseCase = CatalogBadgeUseCase(contentRepository, catalogViewDao, retailerViewDao, time, catalogVisitRepository, catalogVisitMigrationDelegate)
    }

    @Test
    fun `assert badge delegate work correctly`() {
        whenever(retailerViewDao.getAll()).thenReturn(retailerViewsDbs)
        val delegate = catalogBadgeUseCase.getBadgeDelegate()
        val showBadge = delegate(retailerInfo)
        assertEquals(expected, showBadge)
    }
}
