package com.edadeal.android.model.ads

import com.edadeal.android.dto.Promo
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdConditionsTest {

    @Test
    fun testAdsMinMaxItemsCount() {
        assertTrue(itemsCountCondition(min = 0, max = 0).isItemsCountOk(itemsCount = 1))
        assertTrue(itemsCountCondition(min = 2, max = 0).isItemsCountOk(itemsCount = 2))
        assertFalse(itemsCountCondition(min = 2, max = 0).isItemsCountOk(itemsCount = 1))
        assertTrue(itemsCountCondition(min = 2, max = 2).isItemsCountOk(itemsCount = 2))
        assertTrue(itemsCountCondition(min = 0, max = 2).isItemsCountOk(itemsCount = 2))
        assertFalse(itemsCountCondition(min = 0, max = 2).isItemsCountOk(itemsCount = 3))
        assertFalse(itemsCountCondition(min = 2, max = 2).isItemsCountOk(itemsCount = 3))
        assertTrue(itemsCountCondition(min = 2, max = 3).isItemsCountOk(itemsCount = 3))
        assertFalse(itemsCountCondition(min = 3, max = 2).isItemsCountOk(itemsCount = 2))
    }

    private fun itemsCountCondition(min: Int, max: Int): AdConditions {
        val itemsCount = Promo.Having.ItemsCount(min = min, max = max)
        val condition = Promo.Conditions(having = Promo.Having(itemsCount = itemsCount))
        return AdConditions(Promo.Banner(conditions = condition))
    }
}
