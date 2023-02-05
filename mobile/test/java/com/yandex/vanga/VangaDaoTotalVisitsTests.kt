package com.yandex.vanga

import com.yandex.vanga.entity.TOTAL_VISITS_UNKNOWN_VALUE
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import java.util.Calendar

class VangaDaoTotalVisitsTests : BaseVangaRobolectricTest() {

    @Test
    fun `should set unknown total visit count for new item`() {
        Assume.assumeThat(dao.getEntryCount(), Is.`is`(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert = visitsBatchInsert)

        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])

        Assert.assertThat(entity!!.totalCount, Is.`is`(TOTAL_VISITS_UNKNOWN_VALUE))
    }
}