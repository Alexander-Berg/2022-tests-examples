package com.yandex.vanga

import com.yandex.vanga.entity.VangaTestEntity
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assume
import org.junit.Test
import java.util.Calendar

class VangaDaoRecentsTest : BaseVangaRobolectricTest() {

    @Test
    fun `insert 1 item, item inserted`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, TEST_HOUR_OF_DAY, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val entity = get(TEST_KEY, TEST_HOUR_OF_DAY, TEST_DAY_OF_WEEK)

        assertEntityPrimaryInfoEquals(entity)
    }

    @Test
    fun `insert 1 item, recent for it is 0`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, TEST_HOUR_OF_DAY, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val entity = get(TEST_KEY, TEST_HOUR_OF_DAY, TEST_DAY_OF_WEEK)

        assertThat(entity!!.recent, equalTo(0))
    }

    @Test
    fun `insert 2 items, recent for first is 1`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, 0, 1, 1, visitsBatchInsert)
        dao.updateVisitsInfo("second key", 0, 1, 1, visitsBatchInsert)

        val entity = get(TEST_KEY, TEST_HOUR_OF_DAY, TEST_DAY_OF_WEEK)

        assertThat(entity!!.recent, equalTo(1))
    }

    @Test
    fun `insert 2 items, recent for second is 0`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, TEST_HOUR_OF_DAY, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, TEST_HOUR_OF_DAY, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)

        val entity = get(TEST_KEY_2, TEST_HOUR_OF_DAY, TEST_DAY_OF_WEEK)

        assertThat(entity!!.recent, equalTo(0))
    }

    @Test
    fun `insert 2 items, then update the first, for first item recent is 0`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])
        assertThat(entity!!.recent, equalTo(0))
    }

    @Test
    fun `insert 2 items, then update the first, for second item recent is 1`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val entity = get(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])
        assertThat(entity!!.recent, equalTo(1))
    }

    @Test
    fun `insert 2 items, then update the second, for first item recent is 2`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])
        assertThat(entity!!.recent, equalTo(2))
    }

    @Test
    fun `insert 2 items, then update the second, for second item recent is 0`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val entity = get(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])
        assertThat(entity!!.recent, equalTo(0))
    }

    @Test
    fun `insert 3 items, for first item recent is 2`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo("some key 3", calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])
        assertThat(entity!!.recent, equalTo(2))
    }

    @Test
    fun `insert 3 items, for second item recent is 1`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo("some key 3", calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val entity = get(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])
        assertThat(entity!!.recent, equalTo(1))
    }

    @Test
    fun `insert 3 items, for last item recent is 0`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo("some key 3", calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val entity = get("some key 3", calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])
        assertThat(entity!!.recent, equalTo(0))
    }

    @Test
    fun `insert 2 items, launch first item in different hour recent is 0 for first item with origin hour`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        calendar[Calendar.HOUR_OF_DAY] = 1
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar[Calendar.HOUR_OF_DAY] = 2
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val result = get(TEST_KEY, 1, calendar[Calendar.DAY_OF_WEEK])
        assertThat(result!!.recent, equalTo(0))
    }

    @Test
    fun `insert 2 items, launch first item in different hour recent is 1 for second item with origin hour`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        calendar[Calendar.HOUR_OF_DAY] = 1
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar[Calendar.HOUR_OF_DAY] = 2
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val result = get(TEST_KEY_2, 1, calendar[Calendar.DAY_OF_WEEK])
        assertThat(result!!.recent, equalTo(1))
    }

    @Test
    fun `insert 2 items, launch first item in different day recent is 0 for first item with origin day`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        calendar[Calendar.DAY_OF_WEEK] = 1
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar[Calendar.DAY_OF_WEEK] = 2
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val result = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], 1)
        assertThat(result!!.recent, equalTo(0))
    }

    @Test
    fun `insert 2 items, launch first item in different day recent is 1 for second item with origin day`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        calendar[Calendar.DAY_OF_WEEK] = 1
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar[Calendar.DAY_OF_WEEK] = 2
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val result = get(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], 1)
        assertThat(result!!.recent, equalTo(1))
    }

    @Test
    fun `insert 2 items, launch first item in different day and hour recent is 0 for first item with origin day and hour`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        calendar[Calendar.HOUR_OF_DAY] = 1
        calendar[Calendar.DAY_OF_WEEK] = 1
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar[Calendar.HOUR_OF_DAY] = 2
        calendar[Calendar.DAY_OF_WEEK] = 2
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val result = get(TEST_KEY, 1, 1)
        assertThat(result!!.recent, equalTo(0))
    }

    @Test
    fun `insert 2 items, launch first item in different day and hour recent is 1 for second item with origin day and hour`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        calendar[Calendar.HOUR_OF_DAY] = 1
        calendar[Calendar.DAY_OF_WEEK] = 1
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_2, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar[Calendar.HOUR_OF_DAY] = 2
        calendar[Calendar.DAY_OF_WEEK] = 2
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val result = get(TEST_KEY_2, 2, 2)
        assertThat(result!!.recent, equalTo(1))
    }

    @Test
    fun `just installed item, query recent for that item, recent is 0`() {
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert, true)

        val vangaItemId = dao.getVangaItem(TEST_KEY)!!.rowId!!

        assertThat(dao.getRecentVisitsInfo(vangaItemId)!!.visitsValue, equalTo(0))
    }

    private fun assertEntityPrimaryInfoEquals(
        actualResult: VangaTestEntity?
    ) {
        assertThat(actualResult!!.key, equalTo(TEST_KEY))
        assertThat(actualResult.hourOfDay, equalTo(TEST_HOUR_OF_DAY))
        assertThat(actualResult.dayOfWeek, equalTo(TEST_DAY_OF_WEEK))
    }


}