package ru.yandex.disk.utils

import android.database.Cursor
import org.mockito.kotlin.*
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.util.BetterCursorWrapper

private const val INT_FIELD = "INT_FIELD"
private const val LONG_FIELD = "LONG_FIELD"
private const val STRING_FIELD = "STRING_FIELD"
private const val BOOLEAN_FIELD = "BOOLEAN_FIELD"
private const val NULL_FIELD = "NULL_FIELD"

private class DelegatedCursorWrapper(cursor: Cursor) : BetterCursorWrapper<Any>(cursor) {
    val intField by cursorInt(INT_FIELD)
    val longField by cursorLong(LONG_FIELD)
    val stringField by cursorString(STRING_FIELD)
    val booleanField by cursorBoolean(BOOLEAN_FIELD)
    val nullField by cursorIsNull(NULL_FIELD)

    override fun makeItemForRow() = Unit
}

class CursorDelegatesTest : TestCase2() {

    private val innerCursor = mock<Cursor>()
    private val delegatedCursor = DelegatedCursorWrapper(innerCursor)

    @Test
    fun `should access int field`() {
        delegatedCursor.intField
        verify(innerCursor).getInt(any())
    }

    @Test
    fun `should access long field`() {
        delegatedCursor.longField
        verify(innerCursor).getLong(any())
    }

    @Test
    fun `should access string field`() {
        delegatedCursor.stringField
        verify(innerCursor).getString(any())
    }

    @Test
    fun `should access boolean field`() {
        delegatedCursor.booleanField
        verify(innerCursor).getInt(any())
    }

    @Test
    fun `should check null field`() {
        delegatedCursor.nullField
        verify(innerCursor).isNull(any())
    }

    @Test
    fun `should resolve column index`() {
        val index = 1
        whenever(innerCursor.getColumnIndex(INT_FIELD)) doReturn index

        delegatedCursor.intField

        verify(innerCursor).getInt(eq(index))
    }

    @Test
    fun `should resolve column index only once`() {
        whenever(innerCursor.getColumnIndex(INT_FIELD)) doReturn 1

        delegatedCursor.intField
        delegatedCursor.intField

        verify(innerCursor).getColumnIndex(INT_FIELD)
    }

    @Test
    fun `should return actual value`() {
        val first = 1
        val second = 2
        val index = 0
        whenever(innerCursor.getColumnIndex(INT_FIELD)) doReturn index
        whenever(innerCursor.getInt(eq(index))) doReturn first
        delegatedCursor.intField

        whenever(innerCursor.getInt(eq(index))) doReturn second

        assertThat(delegatedCursor.intField, equalTo(second))
    }

    @Test
    fun `should not check nullability if not found column`() {
        whenever(innerCursor.getColumnIndex(NULL_FIELD)) doReturn -1

        val isNull = delegatedCursor.nullField

        assertThat(isNull, equalTo(true))
        verify(innerCursor, never()).isNull(any())
    }
}
