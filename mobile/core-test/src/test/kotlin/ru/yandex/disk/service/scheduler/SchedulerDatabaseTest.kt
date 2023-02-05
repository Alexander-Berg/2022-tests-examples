package ru.yandex.disk.service.scheduler

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.service.work.WorkDatabaseSchemeCreator
import ru.yandex.disk.storage.MockSharedPreferences
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.upload.QueueAutouploadsCommandRequest

private const val COMMAND_TAG_1 = "test1"
private const val COMMAND_TAG_2 = "test2"

@Config(manifest = Config.NONE)
class SchedulerDatabaseTest : AndroidTestCase2() {

    private lateinit var schedulerDatabase: SchedulerDatabase

    override fun setUp() {
        super.setUp()
        schedulerDatabase = SchedulerDatabase(SchedulerDH(mockContext, WorkDatabaseSchemeCreator(MockSharedPreferences())))
    }

    @Test
    fun `should remove task by tag`() {
        schedulerDatabase.add(buildScheduleInfo(COMMAND_TAG_1, 1))
        schedulerDatabase.add(buildScheduleInfo(COMMAND_TAG_2, 2))

        schedulerDatabase.remove(COMMAND_TAG_1)

        assertSingleTask(2L, COMMAND_TAG_2)
    }

    @Test
    fun `should not crash on missing task remove`() {
        schedulerDatabase.add(buildScheduleInfo(COMMAND_TAG_2, 2))

        schedulerDatabase.remove(COMMAND_TAG_1)

        assertSingleTask(2L, COMMAND_TAG_2)
    }

    @Test
    fun `should update existing entry`() {
        schedulerDatabase.add(buildScheduleInfo(COMMAND_TAG_1, 2L))
        val updatedDate = 5L
        schedulerDatabase.add(buildScheduleInfo(COMMAND_TAG_1, updatedDate))

        assertSingleTask(updatedDate, COMMAND_TAG_1)
    }

    private fun assertSingleTask(updatedDate: Long, tag: String) {
        val allTasks = schedulerDatabase.queryAll()
        assertThat(allTasks.count, equalTo(1))

        val task = allTasks.singleAndCopy()!!
        assertThat(task.tag, equalTo(tag))
        assertThat(task.date, equalTo(updatedDate))
    }

    @Test
    fun `should check if command already scheduled`() {
        val tag = QueueAutouploadsCommandRequest().javaClass.name
        assertThat(schedulerDatabase.isScheduled(tag), equalTo(false))

        schedulerDatabase.add(buildScheduleInfo(tag, 100))
        assertThat(schedulerDatabase.isScheduled(tag), equalTo(true))
    }

    private fun buildScheduleInfo(tag: String, date: Long) = ScheduledRequestInfo(tag, date, 0, "")
}