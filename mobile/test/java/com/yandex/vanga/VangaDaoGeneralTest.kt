package com.yandex.vanga

import androidx.collection.ArrayMap
import com.yandex.vanga.db.runInTransactionInlined
import com.yandex.vanga.entity.PERSONAL_DAILY_VISITS_TYPE
import com.yandex.vanga.entity.PERSONAL_HOURLY_VISITS_TYPE
import com.yandex.vanga.entity.PERSONAL_VISITS_TYPE
import com.yandex.vanga.entity.RECENT_VISITS_TYPE
import com.yandex.vanga.entity.TOTAL_DAILY_VISITS_TYPE
import com.yandex.vanga.entity.TOTAL_HOURLY_VISITS_TYPE
import com.yandex.vanga.entity.TOTAL_VISITS_TYPE
import com.yandex.vanga.entity.TOTAL_VISITS_UNKNOWN_VALUE
import com.yandex.vanga.entity.VangaEntity
import com.yandex.vanga.entity.VisitsEntity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsNull.notNullValue
import org.hamcrest.core.IsNull.nullValue
import org.junit.Assume
import org.junit.Test
import java.util.Calendar

typealias VisitsMap = ArrayMap<Int, Int>

class VangaDaoGeneralTest : BaseVangaRobolectricTest() {

    @Test
    fun `insert item, correct key is saved`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])

        assertThat(entity!!.key, equalTo(TEST_KEY))
    }

    @Test
    fun `insert item, correct hour is saved`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])

        assertThat(entity!!.hourOfDay, equalTo(calendar[Calendar.HOUR_OF_DAY]))
    }

    @Test
    fun `insert item, correct day is saved`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        val entity = get(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])

        assertThat(entity!!.dayOfWeek, equalTo(calendar[Calendar.DAY_OF_WEEK]))
    }

    @Test
    fun `insert 3 items with different personal count, max count is found`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo("some key 2", calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo("some key 2", calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo("some key 2", calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo("some key 2", calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo("some key 3", calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo("some key 3", calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo("some key 3", calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)

        val maxPersonalVisits = dao.getMaxPersonalCount()
        assertThat(maxPersonalVisits, equalTo(6))
    }

    @Test
    fun `insert 2 items, entry count is 2`() {
        val count = dao.getEntryCount()
        Assume.assumeThat(count, equalTo(0))

        dao.insertVangaItem(VangaEntity(TEST_KEY))
        dao.insertVangaItem(VangaEntity(TEST_KEY_2))

        assertThat(dao.getEntryCount(), equalTo(2))
    }

    @Test
    fun `clear db from old personal values by vangaItemId, not related values aren't removed`() {
        Assume.assumeThat(dao.getEntryCount(), equalTo(0))

        val vangaItemId1 = dao.insertVangaItem(VangaEntity(TEST_KEY))
        val vangaItemId2 = dao.insertVangaItem(VangaEntity(TEST_KEY_2))

        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 1, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 3, 1, vangaItemId1))

        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 1, 1, vangaItemId2))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 2, 1, vangaItemId2))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 2, 1, vangaItemId2))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 3, 1, vangaItemId2))

        val hours = VisitsMap()
        hours[1] = 12
        val days = VisitsMap()
        days[2] = 12
        dao.clearDbFromOldValues(listOf(vangaItemId1))

        assertThat(dao.getVisitsInfo(vangaItemId1, PERSONAL_HOURLY_VISITS_TYPE, 2), nullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, PERSONAL_DAILY_VISITS_TYPE, 3), nullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, PERSONAL_DAILY_VISITS_TYPE, 2), nullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, PERSONAL_HOURLY_VISITS_TYPE, 1), nullValue())

        assertThat(dao.getVisitsInfo(vangaItemId2, PERSONAL_HOURLY_VISITS_TYPE, 1), notNullValue())
        assertThat(dao.getVisitsInfo(vangaItemId2, PERSONAL_HOURLY_VISITS_TYPE, 2), notNullValue())
        assertThat(dao.getVisitsInfo(vangaItemId2, PERSONAL_DAILY_VISITS_TYPE, 2), notNullValue())
        assertThat(dao.getVisitsInfo(vangaItemId2, PERSONAL_DAILY_VISITS_TYPE, 3), notNullValue())
    }

    @Test
    fun `clear db from old total values by vangaItemId, not related values aren't removed`() {
        Assume.assumeThat(dao.getEntryCount(), equalTo(0))

        val vangaItemId1 = dao.insertVangaItem(VangaEntity(TEST_KEY))
        val vangaItemId2 = dao.insertVangaItem(VangaEntity(TEST_KEY_2))

        dao.insertVisitsInfo(VisitsEntity(TOTAL_HOURLY_VISITS_TYPE, 1, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_DAILY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_HOURLY_VISITS_TYPE, 5, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_DAILY_VISITS_TYPE, 4, 1, vangaItemId1))

        dao.insertVisitsInfo(VisitsEntity(TOTAL_HOURLY_VISITS_TYPE, 1, 1, vangaItemId2))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_HOURLY_VISITS_TYPE, 2, 1, vangaItemId2))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_DAILY_VISITS_TYPE, 2, 1, vangaItemId2))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_DAILY_VISITS_TYPE, 3, 1, vangaItemId2))

        val totalHoursMap = VisitsMap()
        totalHoursMap[5] = 5
        val totalDaysMap = VisitsMap()
        totalDaysMap[4] = 5
        dao.clearDbFromOldValues(listOf(vangaItemId1))

        assertThat(dao.getVisitsInfo(vangaItemId1, TOTAL_HOURLY_VISITS_TYPE, 1), nullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, TOTAL_DAILY_VISITS_TYPE, 2), nullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, TOTAL_HOURLY_VISITS_TYPE, 5), nullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, TOTAL_DAILY_VISITS_TYPE, 4), nullValue())

        assertThat(dao.getVisitsInfo(vangaItemId2, TOTAL_HOURLY_VISITS_TYPE, 1), notNullValue())
        assertThat(dao.getVisitsInfo(vangaItemId2, TOTAL_HOURLY_VISITS_TYPE, 2), notNullValue())
        assertThat(dao.getVisitsInfo(vangaItemId2, TOTAL_DAILY_VISITS_TYPE, 2), notNullValue())
        assertThat(dao.getVisitsInfo(vangaItemId2, TOTAL_DAILY_VISITS_TYPE, 3), notNullValue())
    }

    @Test
    fun `updateCounters removes old items`() {
        Assume.assumeThat(dao.getEntryCount(), equalTo(0))

        val vangaItemId1 = dao.insertVangaItem(VangaEntity(TEST_KEY))

        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 1, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 3, 1, vangaItemId1))

        dao.insertVisitsInfo(VisitsEntity(TOTAL_HOURLY_VISITS_TYPE, 1, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_HOURLY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_DAILY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_DAILY_VISITS_TYPE, 3, 1, vangaItemId1))

        val hours = VisitsMap()
        hours[1] = 12
        val days = VisitsMap()
        days[2] = 12
        val totalHours = VisitsMap()
        totalHours[5] = 10
        val totalDays = VisitsMap()
        totalDays[6] = 11

        val clientItem = ClientVangaItem(TEST_KEY, 12, hours, days, 0, totalHours, totalDays)
        dao.updateCounters(listOf(clientItem), null)

        assertThat(dao.getVisitsInfo(vangaItemId1, PERSONAL_HOURLY_VISITS_TYPE, 2), nullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, PERSONAL_DAILY_VISITS_TYPE, 3), nullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, PERSONAL_HOURLY_VISITS_TYPE, 1), notNullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, PERSONAL_DAILY_VISITS_TYPE, 2), notNullValue())

        assertThat(dao.getVisitsInfo(vangaItemId1, TOTAL_HOURLY_VISITS_TYPE, 2), nullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, TOTAL_DAILY_VISITS_TYPE, 3), nullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, TOTAL_HOURLY_VISITS_TYPE, 5), notNullValue())
        assertThat(dao.getVisitsInfo(vangaItemId1, TOTAL_DAILY_VISITS_TYPE, 6), notNullValue())
    }

    @Test
    fun `updateCounters updates existing`() {
        Assume.assumeThat(dao.getEntryCount(), equalTo(0))

        val vangaItemId1 = dao.insertVangaItem(VangaEntity(TEST_KEY))

        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 1, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 3, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_VISITS_TYPE, null, 2, vangaItemId1))

        dao.insertVisitsInfo(VisitsEntity(TOTAL_HOURLY_VISITS_TYPE, 5, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_HOURLY_VISITS_TYPE, 6, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_DAILY_VISITS_TYPE, 5, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_DAILY_VISITS_TYPE, 6, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_VISITS_TYPE, null, 2, vangaItemId1))

        val hours = VisitsMap()
        hours[1] = 12
        val days = VisitsMap()
        days[2] = 12
        val totalHours = VisitsMap()
        totalHours[1] = 10
        val totalDays = VisitsMap()
        totalDays[2] = 11
        val clientItem = ClientVangaItem(TEST_KEY, 12, hours, days, 11, totalHours, totalDays)
        dao.updateCounters(listOf(clientItem), null)

        val entity = get(TEST_KEY, 1, 2)!!
        assertThat(entity.key, equalTo(TEST_KEY))
        assertThat(entity.personalDailyVisits, equalTo(12))
        assertThat(entity.personalHourlyVisits, equalTo(12))
        assertThat(entity.personalCount, equalTo(12))
        assertThat(entity.totalDailyVisits, equalTo(11))
        assertThat(entity.totalHourlyVisits, equalTo(10))
        assertThat(entity.totalCount, equalTo(11))
    }

    @Test
    fun `updateCounters missing keys reset 'unknown total visit count' status`() {
        Assume.assumeThat(dao.getEntryCount(), equalTo(0))

        val vangaItemId1 = dao.insertVangaItem(VangaEntity(TEST_KEY))

        dao.insertVisitsInfo(VisitsEntity(TOTAL_VISITS_TYPE, null, -1, vangaItemId1))

        dao.insertVisitsInfo(VisitsEntity(PERSONAL_VISITS_TYPE, null, 100, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 20, 44, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 21, 11, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 5, 22, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 6, 33, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_VISITS_TYPE, null, 2, vangaItemId1))

        dao.updateCounters(emptyList(), listOf(TEST_KEY))

        val entity = get(TEST_KEY, 20, 5)!!
        assertThat(entity.key, equalTo(TEST_KEY))
        assertThat(entity.personalHourlyVisits, equalTo(44))
        assertThat(entity.personalDailyVisits, equalTo(22))
        assertThat(entity.personalCount, equalTo(100))
        assertThat(entity.totalDailyVisits, equalTo(0))
        assertThat(entity.totalHourlyVisits, equalTo(0))
        assertThat(entity.totalCount, equalTo(0))
    }

    @Test
    fun `update visits info, remove all visits for vanga item, no visits in table for that item`() {
        Assume.assumeThat(dao.getAllVisitsAmount(), equalTo(0))

        val vangaItemId1 = dao.insertVangaItem(VangaEntity(TEST_KEY))

        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 1, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 3, 1, vangaItemId1))

        dao.insertVisitsInfo(VisitsEntity(TOTAL_HOURLY_VISITS_TYPE, 1, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_HOURLY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_DAILY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(TOTAL_DAILY_VISITS_TYPE, 3, 1, vangaItemId1))

        Assume.assumeThat(dao.getAllVisitsAmount(), equalTo(8))

        dao.removeVisitsForVangaItem(vangaItemId1)

        assertThat(dao.getAllVisitsAmount(), equalTo(0))
    }

    @Test
    fun `update visits info for 2 apps, remove visits for first app, visits for second apps are in db`() {
        Assume.assumeThat(dao.getAllVisitsAmount(), equalTo(0))

        val vangaItemId1 = dao.insertVangaItem(VangaEntity(TEST_KEY))
        val vangaItemId2 = dao.insertVangaItem(VangaEntity(TEST_KEY_2))

        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 1, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 2, 1, vangaItemId1))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 3, 1, vangaItemId1))

        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 1, 1, vangaItemId2))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, 2, 1, vangaItemId2))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 2, 1, vangaItemId2))
        dao.insertVisitsInfo(VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, 3, 1, vangaItemId2))

        Assume.assumeThat(dao.getAllVisitsAmount(), equalTo(8))

        dao.removeVisitsForVangaItem(vangaItemId1)

        assertThat(dao.getAllVisitsAmount(), equalTo(4))
    }

    @Test
    fun `insert vanga item, remove this item, no items in DB`() {
        Assume.assumeThat(dao.getEntryCount(), equalTo(0))

        val vangaItemId1 = dao.insertVangaItem(VangaEntity(TEST_KEY))

        Assume.assumeThat(dao.getEntryCount(), equalTo(1))

        dao.removeVangaItemById(vangaItemId1)

        assertThat(dao.getEntryCount(), equalTo(0))
    }

    @Test
    fun `insert 2 items, remove second item, first item in DB`() {
        Assume.assumeThat(dao.getEntryCount(), equalTo(0))

        dao.insertVangaItem(VangaEntity(TEST_KEY))
        val vangaItemId2 = dao.insertVangaItem(VangaEntity(TEST_KEY_2))

        Assume.assumeThat(dao.getEntryCount(), equalTo(2))

        dao.removeVangaItemById(vangaItemId2)

        assertThat(dao.getEntryCount(), equalTo(1))
        assertThat(dao.getVangaItem(TEST_KEY), notNullValue())
        assertThat(dao.getVangaItem(TEST_KEY_2), nullValue())
    }

    @Test
    fun `increment all starts, value is incremented`() {
        val before = dao.getAllStartsCount().visitsValue

        dao.incrementAllStartsCount()

        assertThat(dao.getAllStartsCount().visitsValue, equalTo(before + 1))
    }

    @Test
    fun `use internal apis, increment all starts, value is incremented`() {
        dao.createAllStartsItem()

        val before = dao.getAllStartsCountInternal()!!.visitsValue

        dao.incrementAllStartCountInternal()

        assertThat(dao.getAllStartsCountInternal()!!.visitsValue, equalTo(before + 1))
    }

    @Test
    fun `increment all starts use internal api, value is not incremented`() {
        val before = dao.getAllStartsCount().visitsValue

        dao.incrementAllStartCountInternal()

        assertThat(dao.getAllStartsCount().visitsValue, equalTo(before))
    }

    @Test
    fun `create all starts count explicitly, has value 1`() {
        dao.createAllStartsItem()

        assertThat(dao.getAllStartsCount().visitsValue, equalTo(1))
    }

    @Test
    fun `create all starts count implicitly, has value 1`() {
        assertThat(dao.getAllStartsCount().visitsValue, equalTo(1))
    }

    @Test
    fun `explicit create all starts, increment all starts, value is incremented`() {
        dao.createAllStartsItemIfNeeded()
        val before = dao.getAllStartsCount().visitsValue

        dao.incrementAllStartsCount()

        assertThat(dao.getAllStartsCount().visitsValue, equalTo(before + 1))
    }

    @Test
    fun `clear db from visits values, two vanga items, add 1024 visits and 1 recently for each, first cleared, second retained`() {
        fun getNonRecentlyVisitsRowsCount(vangaItemId: Long): Long =
                dao.db.compileStatement("SELECT COUNT(*)" +
                        " FROM $VISITS_TABLE_NAME" +
                        " WHERE $FIELD_VANGA_ITEM_ID == $vangaItemId" +
                        " AND $FIELD_VISIT_TYPE <> $RECENT_VISITS_TYPE").simpleQueryForLong()

        val visitsCount = 1024

        dao.db.runInTransactionInlined {
            val vangaItemId1 = dao.insertVangaItem(VangaEntity("{test/test1}"))
            val vangaItemId2 = dao.insertVangaItem(VangaEntity("{test/test2}"))

            listOf(vangaItemId1, vangaItemId2).forEach { vangaItemId ->
                dao.createVisitsBatchInsert(visitsCount).apply {
                    add(VisitsEntity(RECENT_VISITS_TYPE, null, 1, vangaItemId))
                    repeat(visitsCount) {
                        add(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, it, it, vangaItemId))
                    }

                    executeAndClose()
                }
            }

            dao.clearDbFromOldValues(listOf(vangaItemId1))

            assertThat(getNonRecentlyVisitsRowsCount(vangaItemId1), equalTo(0L))
            assertThat(getNonRecentlyVisitsRowsCount(vangaItemId2), equalTo(visitsCount.toLong()))
        }
    }

    @Test
    fun `update counters for missing keys, handles 1024 items batch`() {

        fun getUnknownTotalVisitCount() =
                dao.db.compileStatement("SELECT COUNT(*)" +
                        " FROM $VISITS_TABLE_NAME" +
                        " WHERE $FIELD_VISIT_VALUE == $TOTAL_VISITS_UNKNOWN_VALUE" +
                        " AND $FIELD_VISIT_TYPE == $TOTAL_VISITS_TYPE").simpleQueryForLong()

        val keysCount = 1024L

        dao.db.runInTransactionInlined {

            val keys = (0 until keysCount).map { "package$it/class$it" }

            //add vanga items
            keys.forEach { dao.updateVisitsInfo(it, 5, 5, 1, visitsBatchInsert, true) }


            val unknownTotalVisitCountBefore = getUnknownTotalVisitCount()

            dao.updateCountersForMissingKeys(keys)

            val unknownTotalVisitCountAfter = getUnknownTotalVisitCount()

            assertThat(unknownTotalVisitCountBefore, equalTo(keysCount))
            assertThat(unknownTotalVisitCountAfter, equalTo(0L))
        }
    }

    @Test
    fun `retained 1024 and added 1024 vanga items`() {
        dao.db.runInTransactionInlined {
            val range = 0 until 1024

            val insertedItems = range.map { VangaEntity("{inserted/test$it}") }
            val nonInsertedItems = range.map { VangaEntity("{nonInserted/test$it}") }

            insertedItems.forEach { dao.insertVangaItem(it) }

            val clientVangaItems = (insertedItems + nonInsertedItems)
                    .map { ClientVangaItem(it.key) }
                    .shuffled()

            val keysToVangaItemId = dao.getKeysToVangaItemId(clientVangaItems)

            assertThat(insertedItems.all { keysToVangaItemId[it.key] != null }, equalTo(true))
            assertThat(nonInsertedItems.all { keysToVangaItemId[it.key] == null }, equalTo(true))
        }
    }
}