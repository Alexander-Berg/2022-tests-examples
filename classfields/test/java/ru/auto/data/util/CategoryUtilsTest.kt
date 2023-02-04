package ru.auto.data.util

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author dumchev on 09.02.2018.
 */
@RunWith(AllureRunner::class) class CategoryUtilsTest {


    @Test
    fun oldIdToNew() {
        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(AUTO_ID)) == AUTO_ID)

        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(TRUCKS_ID)) == TRUCKS_ID)
        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(LIGHT_COMMERCIAL_SUBCATEGORY_ID)) == LIGHT_COMMERCIAL_SUBCATEGORY_ID)
        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(TRUCK_SUBCATEGORY_ID)) == TRUCK_SUBCATEGORY_ID)
        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(TRACTOR_SUBCATEGORY_ID)) == TRACTOR_SUBCATEGORY_ID)
        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(BUS_SUBCATEGORY_ID)) == BUS_SUBCATEGORY_ID)
        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(TRAILER_SUBCATEGORY_ID)) == TRAILER_SUBCATEGORY_ID)

        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(MOTO_ID)) == MOTO_ID)
        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(MOTO_SUBCATEGORY_ID)) == MOTO_SUBCATEGORY_ID)
        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(SCOOTERS_SUBCATEGORY_ID)) == SCOOTERS_SUBCATEGORY_ID)
        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(ATVS_SUBCATEGORY_ID)) == ATVS_SUBCATEGORY_ID)
        check(CategoryUtils.oldIdToNew(CategoryUtils.newIdToOld(SNOWMOBILES_SUBCATEGORY_ID)) == SNOWMOBILES_SUBCATEGORY_ID)
    }

    @Test
    fun oldIdToNewParentCategory() {
        check(CategoryUtils.oldIdToParentCategory(AUTO_CATEGORY_OLD_ID) == AUTO_ID)

        check(CategoryUtils.oldIdToParentCategory(COMMERCIAL_CATEGORY_OLD_ID) == TRUCKS_ID)
        check(CategoryUtils.oldIdToParentCategory(LIGHT_COMMERCIAL_SUB_CATEGORY_OLD_ID) == TRUCKS_ID)
        check(CategoryUtils.oldIdToParentCategory(TRUCK_SUB_CATEGORY_OLD_ID) == TRUCKS_ID)
        check(CategoryUtils.oldIdToParentCategory(TRUCK_TRACTOR_SUB_CATEGORY_OLD_ID) == TRUCKS_ID)
        check(CategoryUtils.oldIdToParentCategory(BUS_SUB_CATEGORY_OLD_ID) == TRUCKS_ID)
        check(CategoryUtils.oldIdToParentCategory(TRAILER_SUB_CATEGORY_OLD_ID) == TRUCKS_ID)

        check(CategoryUtils.oldIdToParentCategory(MOTO_CATEGORY_OLD_ID) == MOTO_ID)
        check(CategoryUtils.oldIdToParentCategory(MOTO_SUB_CATEGORY_OLD_ID) == MOTO_ID)
        check(CategoryUtils.oldIdToParentCategory(SCOOTERS_SUB_CATEGORY_OLD_ID) == MOTO_ID)
        check(CategoryUtils.oldIdToParentCategory(ATVS_SUB_CATEGORY_OLD_ID) == MOTO_ID)
        check(CategoryUtils.oldIdToParentCategory(SNOWMOBILES_SUB_CATEGORY_OLD_ID) == MOTO_ID)
    }
}
