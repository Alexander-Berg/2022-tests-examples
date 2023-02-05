package ru.yandex.disk.util

import android.content.ContentResolver
import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle

class MapCursorWrapper(private val items: Array<Map<String, Any?>>) : Cursor {

    private var fields = createFieldsArray()
    private var currentPosition = -1
    private var closed = false

    private fun createFieldsArray() : Array<String> {
        val result = mutableListOf<String>()
        if (items.isNotEmpty()) {
            items[0].keys.forEach {
                result.add(it)
            }
        }
        return result.toTypedArray()
    }

    override fun moveToPosition(position: Int): Boolean {
        currentPosition = position
        return currentPosition in 0 until items.size
    }

    private fun getObject(columnIndex: Int): Any? {
        if (closed) {
            throw IllegalStateException("Attempt to read from closed cursor")
        }
        val key = fields[columnIndex]
        return items[currentPosition][key]
    }

    override fun getColumnIndex(columnName: String?) = fields.indexOf(columnName)

    override fun getColumnIndexOrThrow(columnName: String?): Int {
        val index = getColumnIndex(columnName)
        if (index == -1) {
            throw IllegalArgumentException("Column doesn't exist")
        }
        return index
    }

    override fun getColumnName(columnIndex: Int) = fields[columnIndex]

    override fun getColumnNames(): Array<String> = fields

    override fun getColumnCount() = fields.size

    private inline fun <reified T> get(columnIndex: Int, defaultValue: T) =
            getObject(columnIndex) as? T ?: defaultValue

    override fun getDouble(columnIndex: Int): Double = get(columnIndex, 0.0)

    override fun getFloat(columnIndex: Int): Float = get(columnIndex, 0.0f)

    override fun getLong(columnIndex: Int): Long = get(columnIndex, 0)

    override fun getShort(columnIndex: Int): Short = get(columnIndex, 0)

    override fun getInt(columnIndex: Int) = get(columnIndex, 0)

    override fun getBlob(columnIndex: Int): ByteArray = get(columnIndex, ByteArray(0))

    override fun getString(columnIndex: Int) = get(columnIndex, "")

    override fun moveToFirst(): Boolean = moveToPosition(0)

    override fun moveToNext() = moveToPosition(currentPosition + 1)

    override fun moveToPrevious(): Boolean = moveToPosition(currentPosition - 1)

    override fun close() {
        closed = true
    }

    override fun getPosition() = currentPosition

    override fun getCount() = items.size

    override fun isClosed() = closed

    override fun isBeforeFirst() = currentPosition < 0

    override fun isFirst() = currentPosition == 0

    override fun isLast() = currentPosition == items.lastIndex

    override fun isAfterLast() = currentPosition > items.lastIndex

    override fun move(offset: Int) = moveToPosition(currentPosition + offset)

    override fun isNull(columnIndex: Int) = getObject(columnIndex) == null

    override fun getType(columnIndex: Int): Int {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun unregisterContentObserver(observer: ContentObserver?) {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun requery(): Boolean {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun getWantsAllOnMoveCalls(): Boolean {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun registerDataSetObserver(observer: DataSetObserver?) {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun registerContentObserver(observer: ContentObserver?) {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun moveToLast() = moveToPosition(items.lastIndex)

    override fun deactivate() {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun getNotificationUri(): Uri {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun respond(extras: Bundle?): Bundle {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver?) {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun setNotificationUri(cr: ContentResolver?, uri: Uri?) {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun copyStringToBuffer(columnIndex: Int, buffer: CharArrayBuffer?) {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun getExtras(): Bundle {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun setExtras(extras: Bundle?) {
        throw UnsupportedOperationException("Not implemented")
    }
}