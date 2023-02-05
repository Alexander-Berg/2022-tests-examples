package ru.yandex.disk.upload

import android.content.ContentValues
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Ignore
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import ru.yandex.disk.provider.DiskContract
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.test.TestObjectsFactory

class UploadQueueTriggersTest : TestCase2() {

    private val sqlite = TestObjectsFactory.createSqlite(RuntimeEnvironment.application)
    private val diskDatabase = TestObjectsFactory.createDiskDatabase(sqlite)
    private val db = sqlite.writableDatabase

    @Test
    fun `should fill src parent column`() {
        val parent = "storage/emulated/0/DCIM"
        val srcName = "$parent/Waterfall.jpg"
        val cv = ContentValues(1)
        cv.put(DiskContract.Queue.SRC_NAME, srcName)

        val id = db.insert(DiskContract.Queue.TABLE, null, cv)

        val srcParent = getSrcParent(id)
        assertThat(srcParent, equalTo(parent))
    }

    @Test
    fun `should update src parent column`() {
        val oldParent = "storage/emulated/0/DCIM"
        val newParent = "storage/emulated/0/DCIM/Camera"
        val oldSrcName = "$oldParent/Forest.jpg"
        val cv = ContentValues(1)
        cv.put(DiskContract.Queue.SRC_NAME, oldSrcName)
        val id = db.insert(DiskContract.Queue.TABLE, null, cv)

        val newSrcName = "$newParent/Forest.jpg"
        val newCV = ContentValues(1)
        newCV.put(DiskContract.Queue.SRC_NAME, newSrcName)
        db.update(DiskContract.Queue.TABLE, newCV, "_id = ?", arrayOf(id.toString()))

        val srcParent = getSrcParent(id)
        assertThat(srcParent, equalTo(newParent))
    }

    private fun getSrcParent(id: Long): String {
        db.query("SELECT src_parent FROM ${DiskContract.Queue.TABLE} WHERE _id = $id").use {
            it.moveToFirst()
            return it.getString(0)
        }
    }
}