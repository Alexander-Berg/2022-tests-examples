package com.yandex.vanga

import android.os.Build
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.yandex.vanga.db.AutoClosableVangaStorage
import com.yandex.vanga.db.VangaDao
import com.yandex.vanga.db.VangaStorageProvider
import com.yandex.vanga.db.VisitsBatchInsert
import com.yandex.vanga.entity.PERSONAL_DAILY_VISITS_TYPE
import com.yandex.vanga.entity.PERSONAL_HOURLY_VISITS_TYPE
import com.yandex.vanga.entity.TOTAL_DAILY_VISITS_TYPE
import com.yandex.vanga.entity.TOTAL_HOURLY_VISITS_TYPE
import com.yandex.vanga.entity.VangaTestEntity
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Calendar

const val TEST_KEY = "some key"
const val TEST_KEY_2 = "some key 2"
const val TEST_HOUR_OF_DAY = 1
const val TEST_DAY_OF_WEEK = 2

const val storageName = "vanga.db"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
abstract class BaseVangaRobolectricTest {

    lateinit var db: AutoClosableVangaStorage
    lateinit var dao: VangaDao
    lateinit var calendar: Calendar
    lateinit var visitsBatchInsert: VisitsBatchInsert

    private val reporter = object : Logger {
        override fun d(message: String) {
            print("d $message")
        }

        override fun logException(message: String, t: Throwable) {
            println("logException $message $t")
        }

        override fun e(message: String) {
            println("e $message")
        }
    }

    @Before
    open fun setUp() {

        db = VangaStorageProvider(reporter).get(RuntimeEnvironment.application, storageName, true)
        dao = db.vangaDao
        dao.installTimeHelper = mock { on { getFirstInstallTime(any(), any()) } doReturn 0L }

        calendar = Calendar.getInstance()
        calendar[Calendar.MINUTE] = 0

        visitsBatchInsert = spy(dao.createVisitsBatchInsert(10))
        doAnswer { invocationOnMock ->
            val result = invocationOnMock.callRealMethod()
            visitsBatchInsert.executeAndClose()
            result
        }.`when`(visitsBatchInsert).add(any())
    }

    @After
    fun tearDown() {
        db.close()
    }

    fun get(key: String, hourOfDay: Int, dayOfWeek: Int): VangaTestEntity? {
        val result = VangaTestEntity()
        val vangaEntity = dao.getVangaEntities().firstOrNull { e -> e.key == key } ?: return null
        val vangaItemId = vangaEntity.rowId!!

        result.key = vangaEntity.key
        result.hourOfDay = hourOfDay
        result.dayOfWeek = dayOfWeek

        dao.getRecentVisitsInfo(vangaItemId)?.visitsValue?.let { result.recent = it }
        dao.getPersonalVisitsInfo(vangaItemId)?.visitsValue?.let { result.personalCount = it }
        dao.getVisitsInfo(vangaItemId, PERSONAL_HOURLY_VISITS_TYPE, hourOfDay)?.visitsValue?.let { result.personalHourlyVisits = it }
        dao.getVisitsInfo(vangaItemId, PERSONAL_DAILY_VISITS_TYPE, dayOfWeek)?.visitsValue?.let { result.personalDailyVisits = it }
        dao.getTotalVisitsInfo(vangaItemId)?.visitsValue?.let { result.totalCount = it }
        dao.getVisitsInfo(vangaItemId, TOTAL_HOURLY_VISITS_TYPE, hourOfDay)?.visitsValue?.let { result.totalHourlyVisits = it }
        dao.getVisitsInfo(vangaItemId, TOTAL_DAILY_VISITS_TYPE, dayOfWeek)?.visitsValue?.let { result.totalDailyVisits = it }

        return result
    }

    fun getAllEntries(): ArrayList<VangaTestEntity> {
        val result = ArrayList<VangaTestEntity>()
        dao.getVangaEntities().forEach {
            val item = get(it.key, calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.DAY_OF_WEEK])
            if (item != null) {
                result.add(item)
            }
        }

        return result
    }
}
