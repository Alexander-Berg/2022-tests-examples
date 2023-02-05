package ru.yandex.disk.sql

import android.content.ContentValues
import org.junit.Ignore
import org.junit.Test
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.TestObjectsFactory
import java.util.*
import kotlin.system.measureTimeMillis

class TableTruncaterTest: AndroidTestCase2() {
    private lateinit var dbOpenHelper: SQLiteOpenHelper2

    override fun setUp() {
        super.setUp()
        dbOpenHelper = TestObjectsFactory.createSqlite(mockContext)
        dbOpenHelper.addDatabaseOpenListener(TestSchemaCreator())
    }

    @Test
    @Ignore
    fun `delete from large table MUST work fast`() {
        val recordCount = 1_000_000
        insertAndExecWithMeasure("delete", recordCount) { db: SQLiteDatabase2 ->
            db.query("DELETE FROM $testTableName", null)
        }
        insertAndExecWithMeasure("truncate", recordCount) {
            TableTruncater({ dbOpenHelper.writableDatabase }).truncate(testTableName)
        }
    }

    private fun insertAndExecWithMeasure(operation: String, recordCount: Int, deleteF: (SQLiteDatabase2) -> Unit) {
        insertRecords(recordCount, anotherTestTableName)

        val insertTime = measureTimeMillis {
            insertRecords(recordCount, testTableName)
        }

        val db = dbOpenHelper.writableDatabase
        val operationTime = measureTimeMillis {
            db.beginTransaction()
            try {
                deleteF(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        println("Times: insert = $insertTime ms, $operation = $operationTime ms")
    }

    private fun insertRecords(recordCount: Int, tableName: String) {
        val db = dbOpenHelper.writableDatabase
        val random = Random()
        db.beginTransaction()
        try {
            for (i in 1..recordCount) {
                val cv = ContentValues()
                cv.put("key", i)
                cv.put("value", random.nextInt(200))
                db.insert(tableName, null, cv)

                if (i % 10000 == 0) {
                    println("Inserted record count: $i")
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    class TestSchemaCreator : SQLiteOpenHelper2.DatabaseOpenListener {
        override fun onCreate(db: SQLiteDatabase2) {
            db.execSQL(SQLVocabulary.CREATE_TABLE + testTableName + " ("
                    + "key" + SQLVocabulary.TEXT_
                    + "value" + SQLVocabulary.TEXT + ")"
            )
            db.execSQL(SQLVocabulary.CREATE_TABLE + anotherTestTableName + " ("
                    + "key" + SQLVocabulary.TEXT_
                    + "value" + SQLVocabulary.TEXT + ")"
            )
            db.execSQL("CREATE TRIGGER truncate_test_noop AFTER DELETE ON $testTableName\n" +
                    "BEGIN\n" +
                    "UPDATE $anotherTestTableName SET value = old.value + '1' WHERE value = old.value;\n" +
                    "END;")
        }

        override fun onUpgrade(db: SQLiteDatabase2, oldVersion: Int, newVersion: Int) {
            drop(db)
            onCreate(db)
        }

        private fun drop(db: SQLiteDatabase2) {
            db.execSQL(SQLVocabulary.DROP_TABLE_IF_EXISTS + testTableName)
        }

        override fun onOpen(db: SQLiteDatabase2) = Unit
    }

    companion object {
        private const val testTableName = "truncate_test"
        private const val anotherTestTableName = "another_table"
    }
}