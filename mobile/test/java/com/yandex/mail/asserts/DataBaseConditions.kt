package com.yandex.mail.asserts

import android.database.sqlite.SQLiteDatabase
import org.assertj.core.api.Condition

object DataBaseConditions {

    /**
     * https://stackoverflow.com/a/1604121
     */
    @JvmStatic
    fun table(tableName: String): Condition<SQLiteDatabase> {
        return object : Condition<SQLiteDatabase>() {
            override fun matches(db: SQLiteDatabase): Boolean {
                db.rawQuery(String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'"), null)
                    .use { return it.moveToFirst() }
            }

            override fun toString(): String {
                return "table with name: '$tableName'"
            }
        }
    }

    @JvmStatic
    fun index(indexName: String): Condition<SQLiteDatabase> {
        return object : Condition<SQLiteDatabase>() {
            override fun matches(db: SQLiteDatabase): Boolean {
                db.rawQuery(String.format("SELECT name FROM sqlite_master WHERE type='index' AND name='$indexName'"), null)
                    .use { return it.moveToFirst() }
            }

            override fun toString(): String {
                return "index named: '$indexName'"
            }
        }
    }
}
