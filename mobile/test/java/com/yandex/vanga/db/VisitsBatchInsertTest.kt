package com.yandex.vanga.db

import android.os.Build
import androidx.room.Room
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.yandex.vanga.FIELD_VANGA_ITEM_ID
import com.yandex.vanga.FIELD_VISIT_KEY
import com.yandex.vanga.FIELD_VISIT_TYPE
import com.yandex.vanga.FIELD_VISIT_VALUE
import com.yandex.vanga.VISITS_TABLE_NAME
import com.yandex.vanga.entity.PERSONAL_HOURLY_VISITS_TYPE
import com.yandex.vanga.entity.VangaEntity
import com.yandex.vanga.entity.VisitsEntity
import com.yandex.vanga.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * NB: we only can test fallback insert method due sqlite in robolectris is quite old:
 * https://github.com/robolectric/robolectric/issues/4209
 * Newer batch insert approach can be manually tested via Demo/Actions
 * in VangaApp: [com.yandex.vanga.RatingManagerInternal.testVisitBatchInsert]
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class VisitsBatchInsertTest(val testItemsCount: Int) {

    private lateinit var dao: VangaDao
    private lateinit var storage: VangaStorage

    companion object {

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun data(): Collection<Array<Any>> {
            @Suppress("UNCHECKED_CAST")
            return listOf(anyArray(1), anyArray(10), anyArray(100), anyArray(1000))
        }

        private fun anyArray(v: Int) = arrayOf<Any>(v)
    }

    @Before
    fun setUp() {

        storage = Room.databaseBuilder(RuntimeEnvironment.application!!, VangaStorage::class.java, "vanga.storage")
                .allowMainThreadQueries()
                .build()
        dao = storage.vangaDao
        dao.db = storage.openHelper.writableDatabase

        dao.installTimeHelper =
                mock { on { getFirstInstallTime(any(), any()) } doReturn 0L }
    }

    @Test
    fun `can insert N items`() {
        dao.db.runInTransactionInlined {

            val vangaItemId = dao.insertVangaItem(VangaEntity("{test/test}"))

            val batch = VisitsBatchInsert(dao.db, 10, dao.sqliteVersion)
            repeat(testItemsCount) {
                batch.add(VisitsEntity(PERSONAL_HOURLY_VISITS_TYPE, it + 1, it, vangaItemId))
            }

            batch.executeAndClose()

            dao.db.query("SELECT $FIELD_VISIT_TYPE, $FIELD_VISIT_KEY, $FIELD_VISIT_VALUE " +
                    "FROM $VISITS_TABLE_NAME " +
                    "WHERE $FIELD_VANGA_ITEM_ID == $vangaItemId " +
                    "AND $FIELD_VISIT_TYPE == $PERSONAL_HOURLY_VISITS_TYPE " +
                    "ORDER BY $FIELD_VISIT_VALUE")
                    .use { cursor ->

                        assertThat(cursor.count, equalTo(testItemsCount))

                        repeat(testItemsCount) {
                            cursor.moveToNext()
                            assertThat(cursor.getInt(0), equalTo(PERSONAL_HOURLY_VISITS_TYPE))
                            assertThat(cursor.getInt(1), equalTo(it + 1))
                            assertThat(cursor.getInt(2), equalTo(it))
                        }
                    }
        }
    }
}
