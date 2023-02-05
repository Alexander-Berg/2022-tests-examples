package com.edadeal.android.model

import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class SearchTest {

    private lateinit var search: Search

    @BeforeTest
    fun prepare() {
        search = Search()
    }

    @Test
    fun `getSynonyms should return list of synonyms for provided text, if it has them`() {
        assertEquals(listOf("lay's", "лэйз", "lays"), search.getSynonyms("lays"))
    }

    @Test
    fun `getSynonyms should return list with provided text, if it has not got synonyms for provided text`() {
        assertEquals(listOf("ololo"), search.getSynonyms("ololo"))
    }
}
