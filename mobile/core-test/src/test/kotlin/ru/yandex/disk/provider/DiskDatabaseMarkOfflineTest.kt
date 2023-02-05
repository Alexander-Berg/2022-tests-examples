package ru.yandex.disk.provider

import org.hamcrest.Matchers.equalTo
import org.junit.Test
import ru.yandex.disk.FileItem
import ru.yandex.util.Path

class DiskDatabaseMarkOfflineTest : DiskDatabaseMethodTest() {

    override fun setUp() {
        super.setUp()

        FileTree.create().content(FileTree.directory("AD"), FileTree.directory("BD"),
                FileTree.file("AF"), FileTree.file("BF")).insertToDiskDatabase(diskDb)
    }

    @Test
    fun `should mark disk items with correct OfflineMark values`() {
        val pathAD = Path("/disk/AD")
        val pathAF = Path("/disk/AF")
        val pathBD = Path("/disk/BD")
        val pathBF = Path("/disk/BF")

        diskDb.patchOfflineMarkByPath(pathAD, true);
        diskDb.patchOfflineMarkByPath(pathAF, true);
        diskDb.patchOfflineMarkByPath(pathBD, false);
        diskDb.patchOfflineMarkByPath(pathBF, false);

        assertThat(diskDb.queryDirectory(pathAD)?.offline, equalTo(FileItem.OfflineMark.MARKED))
        assertThat(diskDb.queryFileItem(pathAF)?.offline, equalTo(FileItem.OfflineMark.MARKED))
        assertThat(diskDb.queryDirectory(pathBD)?.offline, equalTo(FileItem.OfflineMark.NOT_MARKED))
        assertThat(diskDb.queryFileItem(pathBF)?.offline, equalTo(FileItem.OfflineMark.NOT_MARKED))
    }

}
