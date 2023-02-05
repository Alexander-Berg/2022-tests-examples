package com.yandex.vanga.db

import androidx.room.Room
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.yandex.vanga.BaseRobolectricTest
import com.yandex.vanga.Logger
import com.yandex.vanga.entity.PERSONAL_DAILY_VISITS_TYPE
import com.yandex.vanga.entity.PERSONAL_HOURLY_VISITS_TYPE
import com.yandex.vanga.entity.PERSONAL_VISITS_TYPE
import com.yandex.vanga.entity.VangaEntity
import com.yandex.vanga.entity.VisitsEntity
import com.yandex.vanga.equalTo
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNull.nullValue
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

private const val TEST_KEY_1 = "test key 1"

private const val TEST_HUR_OF_DAY_1 = 1
private const val TEST_HUR_OF_DAY_2 = 2

private const val TEST_DAY_OF_WEEK = 1
private const val TEST_DAY_OF_WEEK_2 = 2

private const val TEST_VISITS = 10

class VangaDaoTest: BaseRobolectricTest() {

    private lateinit var db: VangaStorage
    private lateinit var dao: VangaDao
    private lateinit var visitsBatchInsert: VisitsBatchInsert

    val logger = object : Logger {
        override fun d(message: String) {
            println("d $message")
        }

        override fun logException(message: String, t: Throwable) {
            println("logException $message $t")
        }

        override fun e(message: String) {
            println("e $message")
        }
    }

    @Before
    fun setUp() {

        db = Room.databaseBuilder(RuntimeEnvironment.application, VangaStorage::class.java, "vanga.db")
            .allowMainThreadQueries()
            .build()
        dao = db.vangaDao
        dao.db = db.openHelper.writableDatabase

        dao.installTimeHelper =
                mock { on { getFirstInstallTime(any(), any()) } doReturn 0L }

        Assume.assumeThat(dao.getVangaItemCount(), `is`(0))
        Assume.assumeThat(dao.getVisitsItemCount(), `is`(0))

        visitsBatchInsert = spy(dao.createVisitsBatchInsert(10))
        doAnswer { invocationOnMock ->
            val result = invocationOnMock.callRealMethod()
            visitsBatchInsert.execute()
            result
        }.`when`(visitsBatchInsert).add(any())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `on querying existing vanga item, it returned`() {
        val origin = VangaEntity(TEST_KEY_1)
        dao.insertVangaItem(origin)

        val item = dao.getVangaItem(TEST_KEY_1)
        Assert.assertThat(item, `is`(origin))
    }

    @Test
    fun `on querying non existing vanga item, null is returned`() {
        val item = dao.getVangaItem(TEST_KEY_1)

        Assert.assertThat(item, nullValue())
    }

    @Test
    fun `on insert vanga item, item in db matches origin`() {
        val origin = VangaEntity(TEST_KEY_1)
        dao.insertVangaItem(origin)

        val item = dao.getVangaItem(TEST_KEY_1)

        Assert.assertThat(item, `is`(origin))
    }

    @Test
    fun `on insert visits item, item in db matches origin`() {
        val origin = insertTestVisitsInfo()

        val item = dao.getVisitsInfo(origin.vangaItemId, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK)

        Assert.assertThat(item, `is`(origin))
    }

    @Test
    fun `on update visits info, vanga item's first install time is set`() {
        val installTime = 100L

        dao.installTimeHelper = mock { on { getFirstInstallTime(any(), any()) } doReturn installTime }

        dao.updateVisitsInfo(TEST_KEY_1, 1, 2, 1, visitsBatchInsert, false)

        val item = dao.getVangaItem(TEST_KEY_1)!!

        Assert.assertThat(item.firstInstallTime, equalTo(installTime))
    }

    @Test
    fun `on querying existing visits info, it returned`() {
        val origin = insertTestVisitsInfo()

        val item = dao.getVisitsInfo(origin.vangaItemId, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK)

        Assert.assertThat(item, `is`(origin))
    }

    @Test
    fun `on querying non existing visits info, null returned`() {
        val item = dao.getVisitsInfo(1, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK)

        Assert.assertThat(item, nullValue())
    }

    @Test
    fun `on querying existing personal info, it returned`() {
        val origin = insertTestPersonalVisitsInfo()

        val item = dao.getPersonalVisitsInfo(origin.vangaItemId)

        Assert.assertThat(item, `is`(origin))
    }

    @Test
    fun `on querying non existing personal info, null returned`() {
        val item = dao.getPersonalVisitsInfo(1)

        Assert.assertThat(item, nullValue())
    }

    @Test
    fun `on increment existing personal info, it incremented`() {
        val origin = insertTestPersonalVisitsInfo()

        dao.incrementPersonal(origin.vangaItemId)
        val item = dao.getPersonalVisitsInfo(origin.vangaItemId)

        Assert.assertThat(item!!.visitsValue, `is`(origin.visitsValue + 1))
    }

    @Test
    fun `on increment visits info, it incremented`() {
        val origin = insertTestVisitsInfo()

        dao.incrementVisitsInfo(origin.vangaItemId, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK)
        val item = dao.getVisitsInfo(origin.vangaItemId, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK)

        Assert.assertThat(item!!.visitsValue, `is`(origin.visitsValue + 1))
    }

    @Test
    fun `on update visits for non existing item, vanga item added`() {
        val time = 100500L
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)

        val item = dao.getVangaItem(TEST_KEY_1)

        Assert.assertThat(VangaEntity(TEST_KEY_1), `is`(item))
    }

    @Test
    fun `on update visits for non existing item, personal count is 1`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val personalVisitsInfo = dao.getPersonalVisitsInfo(vangaItemId!!)!!.visitsValue

        Assert.assertThat(personalVisitsInfo, `is`(1))
    }

    @Test
    fun `on update visits for non existing item, hourly count is 1`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_HOURLY_VISITS_TYPE, TEST_HUR_OF_DAY_1)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(1))
    }

    @Test
    fun `on update visits for non existing item, daily count is 1`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(1))
    }

    @Test
    fun `on update visits for existing item, personal count is 2`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val personalVisitsInfo = dao.getPersonalVisitsInfo(vangaItemId!!)!!.visitsValue

        Assert.assertThat(personalVisitsInfo, `is`(2))
    }

    @Test
    fun `on update visits for existing item, hourly count is 2`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_HOURLY_VISITS_TYPE, TEST_HUR_OF_DAY_1)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(2))
    }

    @Test
    fun `on update visits for existing item, daily count is 2`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(2))
    }

    @Test
    fun `one entry exist, update this entry with another hour, personal is 2`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_2, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val personalVisitsInfo = dao.getPersonalVisitsInfo(vangaItemId!!)!!.visitsValue

        Assert.assertThat(personalVisitsInfo, `is`(2))
    }

    @Test
    fun `one entry exist, update this entry with another hour and another day, personal is 2`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_2, TEST_DAY_OF_WEEK_2, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val personalVisitsInfo = dao.getPersonalVisitsInfo(vangaItemId!!)!!.visitsValue

        Assert.assertThat(personalVisitsInfo, `is`(2))
    }

    @Test
    fun `one entry exist, update this entry with another day, personal is 2`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK_2, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val personalVisitsInfo = dao.getPersonalVisitsInfo(vangaItemId!!)!!.visitsValue

        Assert.assertThat(personalVisitsInfo, `is`(2))
    }

    @Test
    fun `one entry exist, update this entry with another hour and same day, hourly count for first hour is 1`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_2, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_HOURLY_VISITS_TYPE, TEST_HUR_OF_DAY_1)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(1))
    }

    @Test
    fun `one entry exist, update this entry with another hour and same day, hourly count for second hour is 1`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_2, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_HOURLY_VISITS_TYPE, TEST_HUR_OF_DAY_2)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(1))
    }

    @Test
    fun `one entry exist, update this entry with another hour and same day, daily count is 2`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_2, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(2))
    }


    @Test
    fun `one entry exist, update this entry with another day and same hour, daily count for first hour is 1`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK_2, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(1))
    }

    @Test
    fun `one entry exist, update this entry with another day and same hour, daily count for second hour is 1`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK_2, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK_2)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(1))
    }

    @Test
    fun `one entry exist, update this entry with another day and same hour, hourly count is 2`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK_2, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_HOURLY_VISITS_TYPE, TEST_HUR_OF_DAY_1)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(2))
    }

    @Test
    fun `one entry exist, update this entry with another hour and another day, hourly for first hour is 1`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_2, TEST_DAY_OF_WEEK_2, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_HOURLY_VISITS_TYPE, TEST_HUR_OF_DAY_1)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(1))
    }

    @Test
    fun `one entry exist, update this entry with another hour and another day, hourly for second hour is 1`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_2, TEST_DAY_OF_WEEK_2, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_HOURLY_VISITS_TYPE, TEST_HUR_OF_DAY_2)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(1))
    }

    @Test
    fun `one entry exist, update this entry with another hour and another day, hourly for first day is 1`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_2, TEST_DAY_OF_WEEK_2, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(1))
    }

    @Test
    fun `one entry exist, update this entry with another hour and another day, hourly for second day is 1`() {
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_1, TEST_DAY_OF_WEEK, 1, visitsBatchInsert)
        dao.updateVisitsInfo(TEST_KEY_1, TEST_HUR_OF_DAY_2, TEST_DAY_OF_WEEK_2, 1, visitsBatchInsert)
        val vangaItemId = dao.getVangaItem(TEST_KEY_1)!!.rowId

        val hourlyVisitsInfo = dao.getVisitsInfo(vangaItemId!!, PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK_2)!!.visitsValue

        Assert.assertThat(hourlyVisitsInfo, `is`(1))
    }

    private fun insertTestVisitsInfo(): VisitsEntity {
        val vangaItemId = dao.insertVangaItem(VangaEntity(TEST_KEY_1))
        val origin = VisitsEntity(PERSONAL_DAILY_VISITS_TYPE, TEST_DAY_OF_WEEK, TEST_VISITS, vangaItemId)
        dao.insertVisitsInfo(origin)
        return origin
    }

    private fun insertTestPersonalVisitsInfo(): VisitsEntity {
        val vangaItemId = dao.insertVangaItem(VangaEntity(TEST_KEY_1))
        val origin = VisitsEntity(PERSONAL_VISITS_TYPE, null, TEST_VISITS, vangaItemId)
        dao.insertVisitsInfo(origin)
        return origin
    }
}
