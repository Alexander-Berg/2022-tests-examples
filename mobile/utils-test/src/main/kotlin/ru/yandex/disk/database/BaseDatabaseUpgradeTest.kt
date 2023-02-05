package ru.yandex.disk.database

import android.database.Cursor
import com.google.common.io.Files
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import ru.yandex.disk.sql.DbUtils
import ru.yandex.disk.sql.SQLVocabulary
import ru.yandex.disk.sql.SQLiteOpenHelper2
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.Assert2
import ru.yandex.disk.test.SeclusiveContext
import ru.yandex.disk.util.Arrays2.asStringArray
import ru.yandex.disk.utils.copyColumnToList
import java.io.FileOutputStream

abstract class BaseDatabaseUpgradeTest : AndroidTestCase2() {

    lateinit var context: SeclusiveContext

    override fun setUp() {
        super.setUp()
        context = SeclusiveContext(mockContext)
    }

    protected fun saveDbSnapshot(name: String, path: String) {
        val file = mockContext.getDatabasePath(name)
        Files.asByteSource(file).copyTo(FileOutputStream(path))
    }

    protected fun setupOldDatabase(filename: String, dbName: String) {
        recreateDbFromTestResources(context, dbName, "/olddatabases/" + filename)
    }

    protected fun getDbTriggersNames(sqlite: SQLiteOpenHelper2) = getDatabaseObjectsNames(sqlite, "trigger")

    protected fun getDbIndexesNames(sqlite: SQLiteOpenHelper2) = getDatabaseObjectsNames(sqlite, "index")

    private fun getDatabaseObjectsNames(sqlite: SQLiteOpenHelper2, type: String): List<String> {
        return sqlite.readableDatabase
                .query("SELECT name FROM sqlite_master WHERE type = ?", asStringArray(type))
                .copyColumnToList(0)
    }

    protected fun assertNoTable(sqlite: SQLiteOpenHelper2, tableName: String) {
        val tableSchemaSql = DbUtils.getTableSchemaSql(sqlite.getReadableDatabase(), tableName)
        Assert2.assertThat(tableSchemaSql, `is`<Any>(nullValue()))
    }

    protected fun getTableInfo(sqlite: SQLiteOpenHelper2, tableName: String): Cursor {
        return sqlite.writableDatabase
                .query(SQLVocabulary.SELECT_ALL_FROM + tableName, null)
    }
}