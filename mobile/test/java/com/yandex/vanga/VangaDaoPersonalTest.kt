package com.yandex.vanga

import org.hamcrest.core.Is
import org.hamcrest.core.Is.`is`
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import java.util.Calendar

const val TEST_HOUR_1 = 1
const val TEST_HOUR_3 = 3

class VangaDaoPersonalTest : BaseVangaRobolectricTest()  {

    @Test
    fun `insert 1 item, it's personal count is 1`() {
        var count = dao.getEntryCount()
        Assume.assumeThat(count, Is.`is`(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])

        Assert.assertThat(entity!!.personalCount, `is`(1))
    }

    @Test
    fun `update visits for the item twice, personal is 2`() {
        var count = dao.getEntryCount()
        Assume.assumeThat(count, Is.`is`(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])

        Assert.assertThat(entity!!.personalCount, `is`(2))
    }

    @Test
    fun `update visits for one item in different hour of day, personal for both items is 2`() {
        var count = dao.getEntryCount()
        Assume.assumeThat(count, Is.`is`(0))

        calendar.set(Calendar.HOUR_OF_DAY, TEST_HOUR_1)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar.set(Calendar.HOUR_OF_DAY, TEST_HOUR_3)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        val entries = getAllEntries()

        entries.forEach {
            Assert.assertThat(it.key, `is`(TEST_KEY))
            Assert.assertThat(it.personalCount, `is`(2))
        }
    }

    @Test
    fun `update visits for one item in different day of week, personal for both items is 2`() {
        var count = dao.getEntryCount()
        Assume.assumeThat(count, Is.`is`(0))

        calendar.set(Calendar.DAY_OF_WEEK, TEST_HOUR_1)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar.set(Calendar.DAY_OF_WEEK, TEST_HOUR_3)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        val entries = getAllEntries()

        entries.forEach {
            Assert.assertThat(it.key, `is`(TEST_KEY))
            Assert.assertThat(it.personalCount, `is`(2))
        }
    }

    @Test
    fun `update visits for first item twice, for second item once, max personal is 2`() {
        var count = dao.getEntryCount()
        Assume.assumeThat(count, Is.`is`(0))

        calendar.set(Calendar.DAY_OF_WEEK, TEST_HOUR_1)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar.set(Calendar.DAY_OF_WEEK, TEST_HOUR_3)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        Assert.assertThat(dao.getMaxPersonalCount(), `is`(2))
    }


    @Test
    fun `insert 1 item with 0 visits, it's personal count is 0`() {
        var count = dao.getEntryCount()
        Assume.assumeThat(count, Is.`is`(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 0, visitsBatchInsert)
        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])

        Assert.assertThat(entity!!.personalCount, `is`(0))
    }
}