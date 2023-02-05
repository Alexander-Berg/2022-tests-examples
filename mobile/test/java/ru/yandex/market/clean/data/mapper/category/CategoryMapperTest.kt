package ru.yandex.market.clean.data.mapper.category

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.data.mapper.ImageMapper
import ru.yandex.market.clean.domain.model.Category
import ru.yandex.market.clean.domain.model.NavigationNode
import ru.yandex.market.clean.domain.model.navigationNodeTestInstance
import ru.yandex.market.images.ImageUrlFormatter
import ru.yandex.market.data.media.image.avatars.AvatarsUrlFormatter
import ru.yandex.market.data.media.image.avatars.AvatarsUrlParser
import ru.yandex.market.data.media.image.mapper.ImageReferenceMapper
import ru.yandex.market.data.category.Category as CategoryDto

class CategoryMapperTest {

    private val mapper = CategoryMapper(
        ImageMapper(
            ImageReferenceMapper(AvatarsUrlParser(), listOf()),
            ImageUrlFormatter(AvatarsUrlFormatter())
        )
    )

    @Test
    fun `Test navigation nodes mapping 'empty list'`() {
        val emptyNavNodes = emptyList<NavigationNode>()
        val resultCategories = mapper.map(emptyNavNodes)
        assertThat(resultCategories).isEmpty()
    }

    @Test
    fun `Test CategoryDto mapping`() {
        val navNode = navigationNodeTestInstance()
        val navNodes = listOf(navNode)
        val resultCategories = mapper.map(navNodes)
        assertThat(resultCategories).hasAtLeastOneElementOfType(Category::class.java)
        val category = resultCategories[0]
        assertThat(category.id).isEqualTo(navNode.categoryId)
        assertThat(category.nid).isEqualTo(navNode.nid)
        assertThat(category.name).isEqualTo(navNode.name)
    }

    @Test
    fun `Test navigation node mapping`() {
        val categoryDto = CategoryDto.testInstance()
        categoryDto.name = "test name"
        categoryDto.nid = "test nid"
        categoryDto.children = List(2) {
            val childCategoryDto = ru.yandex.market.data.category.Category.testInstance()
            childCategoryDto.name = "test name $it"
            childCategoryDto.nid = "test nid $it"
            childCategoryDto
        }
        val resultCategory = mapper.map(categoryDto)
        assertThat(resultCategory).isNotNull
        assertThat(resultCategory?.id).isEqualTo(categoryDto.id)
        assertThat(resultCategory?.nid).isEqualTo(categoryDto.nid)
        assertThat(resultCategory?.name).isEqualTo(categoryDto.name)
        assertThat(resultCategory?.children).hasAtLeastOneElementOfType(Category::class.java)
        val categoryChildren = resultCategory?.children!!
        categoryDto.children.forEachIndexed { index, childDto ->
            val childCategory = categoryChildren[index]
            assertThat(resultCategory).isNotNull
            assertThat(childCategory.id).isEqualTo(childDto.id)
            assertThat(childCategory.nid).isEqualTo(childDto.nid)
            assertThat(childCategory.name).isEqualTo(childDto.name)
        }
    }

}