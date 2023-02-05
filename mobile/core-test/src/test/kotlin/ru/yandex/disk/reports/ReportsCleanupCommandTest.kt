package ru.yandex.disk.reports

import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.Storage
import ru.yandex.disk.test.AndroidTestCase2

@Config(manifest = Config.NONE)
class ReportsCleanupCommandTest: AndroidTestCase2() {
    private val storage = mock<Storage>()
    private val command = ReportsCleanupCommand(storage)

    @Test
    fun `should run reports clean`() {
        command.execute(ReportsCleanupCommandRequest())
        verify(storage).cleanReports()
    }
}
