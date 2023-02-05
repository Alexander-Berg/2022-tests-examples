package ru.yandex.disk.service.work

import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.monitoring.PeriodicJobCommandRequest
import ru.yandex.disk.service.BundleAdapter
import ru.yandex.disk.service.JobId
import ru.yandex.disk.service.scheduler.CommandRequestBundler
import ru.yandex.disk.service.scheduler.SchedulerDH
import ru.yandex.disk.sql.SQLiteOpenHelper2
import ru.yandex.disk.storage.MockSharedPreferences
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.upload.QueueAutouploadsCommandRequest

@Config(manifest = Config.NONE)
class WorkDatabaseTest : AndroidTestCase2() {

    private lateinit var workDatabase: WorkDatabase
    private lateinit var sqLiteOpenHelper2: SchedulerDH

    override fun setUp() {
        super.setUp()
        sqLiteOpenHelper2 = SchedulerDH(mockContext, WorkDatabaseSchemeCreator(MockSharedPreferences()))
        workDatabase = WorkDatabase(sqLiteOpenHelper2, CommandRequestBundler(),
                BundleAdapter())
    }

    @Test
    fun `should drop all works by class`() {
        workDatabase.enqueueRequest(JobId.SCHEDULING, QueueAutouploadsCommandRequest())
        workDatabase.enqueueRequest(JobId.SCHEDULING, PeriodicJobCommandRequest())
        workDatabase.enqueueRequest(JobId.SCHEDULING, PeriodicJobCommandRequest())

        workDatabase.dropWork(PeriodicJobCommandRequest())

        val works = workDatabase.queryWork(JobId.SCHEDULING).apply { moveToFirst() }
        assertThat(works.count, equalTo(1))
        val queuedRequestClass = works.createFieldAccessor().requestClassName
        assertThat(queuedRequestClass, equalTo(QueueAutouploadsCommandRequest::class.java.canonicalName))
    }
}
