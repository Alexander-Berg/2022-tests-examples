package ru.auto.ara.filter.screen.commercial

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.consts.Filters
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.filter.fields.CategoryField
import ru.auto.ara.filter.fields.GlobalCategoryField
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AllureRunner::class) class CommercialCategoryTest : CommercialFilterTest() {

    @Test
    fun `sub_category_field_should_always_be_default`() {
        defaultTestedScreen.getValueFieldById<Option>(Filters.CATEGORY_FIELD).value = Option("34", "Автобусы")
        assertTrue(defaultTestedScreen.isDefault)
    }

    @Test
    fun `screen_should_have_at_least_one_sub_category`() {
        assertFalse { categoryProvider.get("commercial_categories").isEmpty() }
    }

    @Test
    fun `sub_category_field_should_be_changed`() {
        val category = Option("34", "Автобусы")
        val field = defaultTestedScreen.getValueFieldById<Option>(Filters.CATEGORY_FIELD)
        field.value = category
        assertEquals(category.key, field.value.key)
    }

    @Test
    fun `fake_category_field_should_not_have_any_query_params`() {
        val fakeCategoryField = defaultTestedScreen.fields.filterIsInstance<GlobalCategoryField>().firstOrNull()
        assertNotNull(fakeCategoryField)
        assertNotEquals(Filters.CATEGORY_FIELD, fakeCategoryField.id)
        assertNull(fakeCategoryField.queryParam)
    }

    @Test
    fun `sub_category_field_should_has_category_id_query_param`() {
        val subCategoryField = CategoryField::class.javaObjectType.cast(defaultTestedScreen.getFieldById(Filters.CATEGORY_FIELD))
        assertEquals(1, subCategoryField!!.queryParam?.filter { it -> "category_id" == it.first }?.size)
    }
}
