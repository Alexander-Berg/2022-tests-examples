package ru.yandex.market.feature.data.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import ru.yandex.market.feature.data.model.search.SearchParamsConverter
import ru.yandex.market.feature.data.model.shared.QueryParam
import ru.yandex.market.feature.ui.search.result.SearchSort

class SearchParamsConverterTest {

    companion object {

        private const val SEARCH_TEXT = "Some text"
        private const val SOME_HID = 123
        private const val SOME_NID = 123

        private val onStockFilter = QueryParam("onstock", "1")
        private val sortFilter = QueryParam("how", "ddate")
        private val searchFilters = listOf(onStockFilter, sortFilter)

    }

    private val searchParamsConverter = SearchParamsConverter()

    @Test
    fun testOnlyTextSearchParams() {
        val params = searchParamsConverter.createSearchParams(SEARCH_TEXT)
        assertNotNull(params)
        assertEquals(SEARCH_TEXT, params?.searchText)
        assertNull(params?.category)
    }

    @Test
    fun testOnlyCategorySearchParams() {
        val params = searchParamsConverter.createSearchParams(hid = SOME_HID, nid = SOME_NID)
        assertNotNull(params)
        assertNotNull(params?.category)
        assertNull(params?.searchText)
        assertEquals(SOME_HID, params?.category?.hid)
        assertEquals(SOME_NID, params?.category?.nid)
    }

    @Test
    fun testFilters() {
        val params = searchParamsConverter.createSearchParams(SEARCH_TEXT, filters = searchFilters)
        assertNotNull(params)
        assertEquals(searchFilters, params?.filterParams)
        assertEquals(SearchSort(id = sortFilter.value), params?.sort)
    }

    @Test
    fun testEmptySearchParams() {
        val params = searchParamsConverter.createSearchParams()
        assertNull(params)
    }

}