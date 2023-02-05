package com.yandex.vanga

import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import java.util.Calendar

class VangaDaoWeeklyVisitsTests : BaseVangaRobolectricTest() {

    @Test
    fun `insert one item, weekly visits is 1`() {
        var count = dao.getEntryCount()
        Assume.assumeThat(count, Is.`is`(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])

        Assert.assertThat(entity!!.personalDailyVisits, Is.`is`(1))
    }

    @Test
    fun `update item 2 times in one day, weekly visits is 2`() {
        var count = dao.getEntryCount()
        Assume.assumeThat(count, Is.`is`(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])

        Assert.assertThat(entity!!.personalDailyVisits, Is.`is`(2))
    }

    @Test
    fun `update item 2 times in different days, weekly visits is 1 for both entries`() {
        var count = dao.getEntryCount()
        Assume.assumeThat(count, Is.`is`(0))

        calendar.set(Calendar.DAY_OF_WEEK, TEST_HOUR_1)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar.set(Calendar.DAY_OF_WEEK, TEST_HOUR_3)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        val entity1 = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], TEST_HOUR_1)
        val entity2 = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], TEST_HOUR_3)

        Assert.assertThat(entity1!!.personalDailyVisits, Is.`is`(1))
        Assert.assertThat(entity2!!.personalDailyVisits, Is.`is`(1))
    }

    @Test
    fun `update item twice in one day but different hours, weekly visits is 2 for both entries`() {
        var count = dao.getEntryCount()
        Assume.assumeThat(count, Is.`is`(0))

        calendar.set(Calendar.HOUR_OF_DAY, TEST_HOUR_1)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar.set(Calendar.HOUR_OF_DAY, TEST_HOUR_3)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        val entity1 = get(TEST_KEY, TEST_HOUR_1, calendar[Calendar.DAY_OF_WEEK])
        val entity2 = get(TEST_KEY, TEST_HOUR_3, calendar[Calendar.DAY_OF_WEEK])

        Assert.assertThat(entity1!!.personalDailyVisits, Is.`is`(2))
        Assert.assertThat(entity2!!.personalDailyVisits, Is.`is`(2))
    }

    @Test
    fun `insert one item with 0 visits, weekly visits is 0`() {
        var count = dao.getEntryCount()
        Assume.assumeThat(count, Is.`is`(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 0, visitsBatchInsert)
        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])

        Assert.assertThat(entity!!.personalDailyVisits, Is.`is`(0))
    }
}