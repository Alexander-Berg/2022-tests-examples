package ru.yandex.disk.test

import ru.yandex.disk.sql.SQLiteDatabase2
import ru.yandex.disk.sql.SQLiteOpenHelper2

class MediaItemsTableCreator : SQLiteOpenHelper2.DatabaseOpenListener {

    override fun onCreate(db: SQLiteDatabase2) {
        db.execSQL("CREATE TABLE MediaItems (path TEXT);")
    }

    override fun onOpen(db: SQLiteDatabase2) = Unit

    override fun onUpgrade(db: SQLiteDatabase2, oldVersion: Int, newVersion: Int) = Unit
}
