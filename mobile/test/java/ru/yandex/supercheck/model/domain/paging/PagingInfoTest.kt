package ru.yandex.supercheck.model.domain.paging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PagingInfoTest {

    private val singlePageInfo = PagingInfo(
        pageNumber = 1,
        pageSize = 1,
        totalPagesCount = 1,
        totalItemsCount = 1
    )

    private val twoPagesInfo = PagingInfo(
        pageNumber = 1,
        pageSize = 20,
        totalPagesCount = 2,
        totalItemsCount = 36
    )

    @Test
    fun getNextPageNumber() {
        assertNull(PagingInfo.empty.nextPageNumber)
        assertNull(singlePageInfo.nextPageNumber)

        assertEquals(2, twoPagesInfo.nextPageNumber)
    }

    @Test
    fun getNextPageNumberWithNewBase() {

        assertNull(PagingInfo.empty.getNextPageNumber(1))
        assertNull(singlePageInfo.getNextPageNumber(3))

        assertEquals(5, twoPagesInfo.getNextPageNumber(5))
        assertNull(twoPagesInfo.copy(pageNumber = 2).getNextPageNumber(10))

        assertEquals(1, twoPagesInfo.getNextPageNumber(30))
        assertEquals(1, twoPagesInfo.getNextPageNumber(60))
    }
}