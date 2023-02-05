package ru.yandex.disk.sql

import android.content.ContentValues
import android.preference.PreferenceManager
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Test
import ru.yandex.disk.sql.SQLVocabulary.*
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.TestObjectsFactory
import java.util.*

class TableSyncHelperTest : AndroidTestCase2() {

    private lateinit var dbOpenHelper: SQLiteOpenHelper2
    private lateinit var sut: TableSyncHelper
    private val readyTable1 = TableSuffix.SECOND.getTableName(TABLE1)
    private val readyTable2 = TableSuffix.SECOND.getTableName(TABLE2)
    private val syncingTable1 = TableSuffix.FIRST.getTableName(TABLE1)
    private val syncingTable2 = TableSuffix.FIRST.getTableName(TABLE2)

    private val copyBatchSize = 5

    override fun setUp() {
        super.setUp()
        dbOpenHelper = TestObjectsFactory.createSqlite(mockContext)
        dbOpenHelper.addDatabaseOpenListener(TestSchemaCreator())
        val keyValueStore = PreferenceManager.getDefaultSharedPreferences(mockContext)
        sut = TableSyncHelper(dbOpenHelper, copyBatchSize, TableSyncSuffixes(keyValueStore, "test") { true })
        sut.addTable(TABLE1)
        sut.addTable(TABLE2)
    }

    @Test
    fun `begin should copy ready tables data to syncing tables`() {
        val random = Random()
        for(i in 1..12) {
            val cv = ContentValues()
            cv.put("key", i)
            cv.put("value1", random.nextInt(10))
            cv.put("value2", random.nextInt(200))
            dbOpenHelper.writableDatabase.insert(readyTable1, null, cv)
        }

        for(i in 1..14) {
            val cv = ContentValues()
            cv.put("key1", if (i == copyBatchSize + 1) copyBatchSize else i)
            cv.put("key2", i)
            cv.put("value1", random.nextInt(10))
            cv.put("value2", random.nextInt(200))
            dbOpenHelper.writableDatabase.insert(readyTable2, null, cv)
        }

        sut.beginSync()

        dbOpenHelper.writableDatabase.delete(readyTable1, "key LIKE 'rand%'", null)
        dbOpenHelper.writableDatabase.delete(syncingTable1, "key LIKE 'rand%'", null)

        assertReadySizeNotChangedAndContentEqualToSyncing(readyTable1, syncingTable1, 12,
                listOf("key"))

        assertReadySizeNotChangedAndContentEqualToSyncing(readyTable2, syncingTable2, 14,
                listOf("key1", "key2"))
    }

    private fun assertReadySizeNotChangedAndContentEqualToSyncing(
            readyTable: String, syncingTable: String, tableSize: Long, idCols: List<String>) {
        assertThatTableSizeEqualTo(readyTable, tableSize)
        assertTableHasIdenticalData(readyTable, syncingTable, idCols)
    }

    private fun assertThatTableSizeEqualTo(table:String, expectedSize: Long) {
        assertThat("table size NOT equal to expected",
                DbUtils.queryNumEntries(dbOpenHelper.writableDatabase, table, null, null),
                equalTo(expectedSize)
        )
    }

    private fun assertTableHasIdenticalData(table1:String, table2:String, idCols:List<String>) {
        fun query(table:String) = dbOpenHelper.readableDatabase.query(
                SELECT_ALL_FROM + table + ORDER_BY + idCols.joinToString()
        )
        query(table1).use { c1 ->
            query(table2).use { c2 ->
                assertThat(c2.columnNames, equalTo(c1.columnNames))

                while(c1.moveToNext() and c2.moveToNext()) {
                    c1.columnNames.forEach {
                        assertThat(
                                c2.getString(c2.getColumnIndex(it)),
                                equalTo(c1.getString(c1.getColumnIndex(it)))
                        )
                    }
                }

                assertThat(c1.isAfterLast, equalTo(true))
                assertThat(c2.isAfterLast, equalTo(true))
            }
        }
    }

    class TestSchemaCreator : SQLiteOpenHelper2.DatabaseOpenListener {
        override fun onCreate(db: SQLiteDatabase2) {
            TableSuffix.values().forEach { create(db, it) }
        }

        private fun create(db: SQLiteDatabase2, suffix: TableSuffix) {
            val table1 = suffix.getTableName(TABLE1)
            db.execSQL(CREATE_TABLE + table1 + " ("
                    + "key" + TEXT_
                    + "value1" + TEXT_
                    + "value2" + INTEGER_
                    + PRIMARY_KEY + "(key)"
                    + ")"
            )
            db.execSQL(CREATE_TABLE + suffix.getTableName(TABLE2) + " ("
                    + "key1" + TEXT_
                    + "key2" + TEXT_
                    + "value1" + TEXT_
                    + "value2" + INTEGER_
                    + PRIMARY_KEY + "(key1, key2)"
                    + ")"
            )
            db.execSQL(CREATE_TRIGGER + suffix.getTableName("concurrent_insert") + AFTER + " INSERT ON " + table1
                    + BEGIN + INSERT_INTO + table1
                    + "(key, value1, value2) VALUES('rand' || random(), new.value1, new.value2)" + _END_)
        }

        override fun onUpgrade(db: SQLiteDatabase2, oldVersion: Int, newVersion: Int) {
            TableSuffix.values().forEach { drop(db, it) }
            onCreate(db)
        }

        private fun drop(db: SQLiteDatabase2, suffix: TableSuffix) {
            db.execSQL(DROP_TABLE_IF_EXISTS + suffix.getTableName(TABLE1))
            db.execSQL(DROP_TABLE_IF_EXISTS + suffix.getTableName(TABLE2))
        }

        override fun onOpen(db: SQLiteDatabase2) = Unit
    }

    companion object {
        const val TABLE1 = "test1"
        const val TABLE2 = "test2"
    }
}
