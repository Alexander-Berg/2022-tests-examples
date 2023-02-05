package com.yandex.launcher.loaders

import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.allapps.Categories
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsCollectionContaining.hasItem
import org.junit.Test
import kotlin.collections.ArrayList

class CategoriesSetTest : BaseRobolectricTest() {

    private val categoriesSamplesInfo = arrayOf(
            Categories.CategoryInfo.ENTERTAINMENT,
            Categories.CategoryInfo.BUSINESS,
            Categories.CategoryInfo.FOOD_AND_DRINK,
            Categories.CategoryInfo.EDUCATION,
            Categories.CategoryInfo.FINANCE
    )
    private val categoriesSamplesMask = run {
        var mask = 0L
        categoriesSamplesInfo.forEach {
            mask = mask or (1L shl it.id)
        }
        mask
    }

    private val samplesPackageNames = categoriesSamplesInfo.map { it.getName() }.toTypedArray()
    private val otherPackageNames = Categories.ALL_CATEGORIES.filter { !samplesPackageNames.contains(it) }.toTypedArray()

    private val samplesCategoriesSet = CategoriesSet(0, categoriesSamplesMask)

    @Test
    fun `put all categories into constructor, CategoriesSet include all of the categories`() {
        val categoriesSet = CategoriesSet(0, Categories.ALL_CATEGORIES)
        for (info in Categories.CategoryInfo.getAndCacheValues()) {
            assertThat(categoriesSet.contains(info.id), equalTo(true))
            assertThat(categoriesSet, hasItem(info.name))
        }
    }

    @Test
    fun `samplesCategoriesSet mask is equal to categoriesSamplesMask`() {
        assertThat(samplesCategoriesSet.mask, equalTo(categoriesSamplesMask))
    }

    @Test
    fun `samplesCategoriesSet was created using mask constructor, only samples are in it`() {
        assertThat(samplesCategoriesSet, hasItems(*samplesPackageNames))
        assertThat(samplesCategoriesSet, not(hasItems(*otherPackageNames)))
    }

    @Test
    fun `create CategorySet using Array constructor with samples, only samples are in it`() {
        val categoriesSet = CategoriesSet(0, samplesPackageNames)
        assertThat(categoriesSet, hasItems(*samplesPackageNames))
        assertThat(categoriesSet, not(hasItems(*otherPackageNames)))
    }

    @Test
    fun `create CategorySet using List constructor with samples, only samples are in it`() {
        val categoriesSet = CategoriesSet(0, samplesPackageNames.toList())
        assertThat(categoriesSet, hasItems(*samplesPackageNames))
        assertThat(categoriesSet, not(hasItems(*otherPackageNames)))
    }

    @Test
    fun `create their own CategorySet for every category, every CategorySet has the category, it created by`() {
        Categories.CategoryInfo.getAndCacheValues().forEach {
            val thisCategory = it.getName()
            val categoriesSet = CategoriesSet(0, arrayOf(thisCategory))
            val categoryArrayWithoutThisCategory = Categories.ALL_CATEGORIES.filter { category -> category != thisCategory }.toTypedArray()

            assertThat(categoriesSet, hasItem(thisCategory))
            assertThat(categoriesSet, not(hasItems(*categoryArrayWithoutThisCategory)))
        }
    }

    @Test
    fun `put null Array into constructor, no one category is in the CategorySet`() {
        val nullArray: Array<String>? = null
        val categoriesSetBasedOnNullArray = CategoriesSet(0, nullArray)

        for ((index, category) in Categories.ALL_CATEGORIES.withIndex()) {
            assertThat(categoriesSetBasedOnNullArray.contains(index), equalTo(false))
            assertThat(categoriesSetBasedOnNullArray, not(hasItem(category)))
        }
    }

    @Test
    fun `put null List into constructor, no one category is in the CategorySet`() {
        val nullArrayList: ArrayList<String>? = null
        val categoriesSetBasedOnNullArrayList = CategoriesSet(0, nullArrayList)

        for ((index, category) in Categories.ALL_CATEGORIES.withIndex()) {
            assertThat(categoriesSetBasedOnNullArrayList.contains(index), equalTo(false))
            assertThat(categoriesSetBasedOnNullArrayList, not(hasItem(category)))
        }
    }

    @Test
    fun `create new empty CategorySet using constructor without mask, no one category is in it`() {
        val categoriesSetWithoutMaskInConstructor = CategoriesSet(0)

        for ((index, category) in Categories.ALL_CATEGORIES.withIndex()) {
            assertThat(categoriesSetWithoutMaskInConstructor.contains(index), equalTo(false))
            assertThat(categoriesSetWithoutMaskInConstructor, not(hasItem(category)))
        }
    }

    @Test
    fun `create new empty CategorySet using constructor with zero mask and, no one category is in it`() {
        val categoriesSetBasedOnZeroMask = CategoriesSet(0, 0)

        for ((index, category) in Categories.ALL_CATEGORIES.withIndex()) {
            assertThat(categoriesSetBasedOnZeroMask.contains(index), equalTo(false))
            assertThat(categoriesSetBasedOnZeroMask, not(hasItem(category)))
        }
    }

    @Test
    fun `create new empty CategorySet using constructor with Array of Categories putting empty Array as parameter, no one category is in it`() {
        val categoriesSetBasedOnEmptyArray = CategoriesSet(0, arrayOf())

        for ((index, category) in Categories.ALL_CATEGORIES.withIndex()) {
            assertThat(categoriesSetBasedOnEmptyArray.contains(index), equalTo(false))
            assertThat(categoriesSetBasedOnEmptyArray, not(hasItem(category)))
        }
    }

    @Test
    fun `create new empty CategorySet using constructor with List of Categories putting empty List as parameter, no one category is in it`() {
        val categoriesSetBasedOnEmptyArrayList = CategoriesSet(0, ArrayList())

        for ((index, category) in Categories.ALL_CATEGORIES.withIndex()) {
            assertThat(categoriesSetBasedOnEmptyArrayList.contains(index), equalTo(false))
            assertThat(categoriesSetBasedOnEmptyArrayList, not(hasItem(category)))
        }
    }

    @Test(expected = RuntimeException::class)
    fun `call contains with nonexistent category, cause exception`() {
        samplesCategoriesSet.contains(Categories.NONEXISTENT_CATEGORY_NAME)
    }

    @Test
    fun `same CategoriesSet are equals`() {
        val categoriesSet2 = CategoriesSet(0, categoriesSamplesMask)
        assertThat(categoriesSet2, equalTo(samplesCategoriesSet))
    }

    @Test
    fun `same Categories have equals hashCode's`() {
        val categoriesSet2 = CategoriesSet(0, categoriesSamplesMask)
        assertThat(categoriesSet2, equalTo(samplesCategoriesSet))
    }

    @Test
    fun `different CategoriesSet are not equals`() {
        val categoriesSet2 = CategoriesSet(0, otherPackageNames)
        assertThat(categoriesSet2, not(equalTo(samplesCategoriesSet)))
    }

    @Test
    fun `different CategoriesSet have different hashCode's`() {
        val categoriesSet2 = CategoriesSet(0, otherPackageNames)
        assertThat(categoriesSet2.hashCode(), not(equalTo(samplesCategoriesSet.hashCode())))
    }

    @Test
    fun `iterator on sample CategorySet iterates over all samples`() {
        val iterator = samplesCategoriesSet.iterator()
        var count = 0

        while (iterator.hasNext()) {
            count++
            assertThat(samplesPackageNames.contains(iterator.next()), equalTo(true))
        }
        assertThat(count, equalTo(categoriesSamplesInfo.size))
    }

    @Test
    fun `iterator on empty CategoriesSet doesn't iterates`() {
        val iterator = CategoriesSet(0, 0).iterator()
        var count = 0

        while (iterator.hasNext()) {
            count++
        }

        assertThat(count, equalTo(0))
    }

    @Test(expected = NoSuchElementException::class)
    fun `iterator on CategoriesSet filled with samples throws NoSuchElementException after all of the elements have been passed`() {
        val iterator = samplesCategoriesSet.iterator()
        while (true) {
            iterator.next()
        }
    }

    @Test(expected = NoSuchElementException::class)
    fun `iterator on empty CategoriesSet throws NoSuchElementException`() {
        val iterator = CategoriesSet(0, 0).iterator()
        iterator.next()
    }
}
