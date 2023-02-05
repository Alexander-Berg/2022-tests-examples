package ru.yandex.disk.service.scheduler

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import ru.yandex.disk.database.BaseDatabaseUpgradeTest
import ru.yandex.disk.service.work.WorkContract
import ru.yandex.disk.service.work.WorkDatabaseSchemeCreator
import ru.yandex.disk.sql.DbUtils
import ru.yandex.disk.sql.SQLiteOpenHelper2
import ru.yandex.disk.storage.MockSharedPreferences

class SchedulerDHUpgradeTest : BaseDatabaseUpgradeTest() {

    private lateinit var sqLiteOpenHelper2: SQLiteOpenHelper2

    @Test
    fun `should create work table on upgrade`() {
        setupOldDatabase("scheduler_3.41_from_android_6.0", SchedulerDH.DB_NAME)

        sqLiteOpenHelper2 = SchedulerDH(context, WorkDatabaseSchemeCreator(MockSharedPreferences()))

        assertThat(DbUtils.hasTable(sqLiteOpenHelper2.writableDatabase, WorkContract.TABLE),
                equalTo(true))
    }
}