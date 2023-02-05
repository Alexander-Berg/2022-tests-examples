package com.yandex.launcher.allapps

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.yandex.launcher.BaseRobolectricTest
import org.junit.Test

class CategoriesTest : BaseRobolectricTest() {

    @Test
    fun `CategoryInfo include only categories with unique id`() {
        val categoryInfos = Categories.CategoryInfo.getAndCacheValues()
        val uniqueCount = categoryInfos.map { it.id }.toSet().count()

        assertThat(uniqueCount, equalTo(categoryInfos.size))
    }
}