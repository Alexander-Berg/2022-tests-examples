package com.yandex.vanga

import com.yandex.vanga.entity.PERSONAL_DAILY_VISITS_TYPE
import com.yandex.vanga.entity.PERSONAL_HOURLY_VISITS_TYPE
import com.yandex.vanga.entity.PERSONAL_VISITS_TYPE
import com.yandex.vanga.entity.RECENT_VISITS_TYPE
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class VangaDaoNormalizationTest : BaseVangaRobolectricTest() {

    private val firstHourOfDay = 1
    private val firstDayOfWeek = 2

    private val secondHourOfDay = 0
    private val secondDayOfWeek = firstDayOfWeek

    private val thirdHourOfDay = 23
    private val thirdDayOfWeek = 1

    @Before
    override fun setUp() {
        super.setUp()

        calendar[Calendar.HOUR_OF_DAY] = firstHourOfDay
        calendar[Calendar.DAY_OF_WEEK] = firstDayOfWeek
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar[Calendar.HOUR_OF_DAY] = secondHourOfDay
        calendar[Calendar.DAY_OF_WEEK] = secondDayOfWeek
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
        calendar[Calendar.HOUR_OF_DAY] = thirdHourOfDay
        calendar[Calendar.DAY_OF_WEEK] = thirdDayOfWeek
        dao.updateVisitsInfo(TEST_KEY, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK], 1, visitsBatchInsert)
    }

    @Test
    fun `max personal count is 15, normalized personal count is correct`() {
        val maxPersonalCount = 15
        val expectedNormalizedPersonal = 4 * 1.0 / maxPersonalCount

        val firstValue = dao.getNormalizedCountList(maxPersonalCount, firstHourOfDay, firstDayOfWeek).firstOrNull { e -> e.visitType == PERSONAL_VISITS_TYPE }

        assertThat(firstValue!!.normalizedVisitValue, equalTo(expectedNormalizedPersonal))
    }

    @Test
    fun `max personal count is 7, normalized hourly visits is correct I`() {
        val maxPersonalCount = 7
        val expectedNormalizedHourlyVisits = 2 * 1.0 / maxPersonalCount

        val firstValue = dao.getNormalizedCountList(maxPersonalCount, firstHourOfDay, firstDayOfWeek).firstOrNull { e -> e.visitType == PERSONAL_HOURLY_VISITS_TYPE }

        assertThat(firstValue!!.normalizedVisitValue, equalTo(expectedNormalizedHourlyVisits))
    }

    @Test
    fun `max personal count is 6, normalized hourly visits is correct II`() {
        val maxPersonalCount = 6
        val expectedNormalizedHourlyVisits = 1 * 1.0 / maxPersonalCount

        val firstValue = dao.getNormalizedCountList(maxPersonalCount, secondHourOfDay, secondDayOfWeek).firstOrNull { e -> e.visitType == PERSONAL_HOURLY_VISITS_TYPE }

        assertThat(firstValue!!.normalizedVisitValue, equalTo(expectedNormalizedHourlyVisits))
    }

    @Test
    fun `max personal count is 12, normalized daily visits is correct I`() {
        val maxPersonalCount = 13
        val expectedNormalizedDailyVisits = 3 * 1.0 / maxPersonalCount

        val firstValue = dao.getNormalizedCountList(maxPersonalCount, secondHourOfDay, secondDayOfWeek).firstOrNull { e -> e.visitType == PERSONAL_DAILY_VISITS_TYPE }

        assertThat(firstValue!!.normalizedVisitValue, equalTo(expectedNormalizedDailyVisits))
    }

    @Test
    fun `max personal count is 4, normalized daily visits is correct II`() {
        val maxPersonalCount = 4
        val expectedNormalizedDailyVisits = 1 * 1.0 / maxPersonalCount

        val firstValue = dao.getNormalizedCountList(maxPersonalCount, thirdHourOfDay, thirdDayOfWeek).firstOrNull { e -> e.visitType == PERSONAL_DAILY_VISITS_TYPE }

        assertThat(firstValue!!.normalizedVisitValue, equalTo(expectedNormalizedDailyVisits))
    }

    @Test
    fun `insert just installed item after 4 visits, has recent 0`() {
        dao.updateVisitsInfo(TEST_KEY_2, firstHourOfDay, firstDayOfWeek, 1, visitsBatchInsert, true)

        val vangaItemId = dao.getVangaItem(TEST_KEY_2)!!.rowId!!

        val values = dao.getNormalizedCountList(100, firstHourOfDay, firstDayOfWeek)
        val recentVisitEntity = values.firstOrNull { e -> e.vangaItemId == vangaItemId && e.visitType == RECENT_VISITS_TYPE }!!

        assertThat(recentVisitEntity.visitsValue, equalTo(0))
    }
}