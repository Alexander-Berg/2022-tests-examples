package com.yandex.vanga.db

import com.yandex.vanga.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.lang.Exception

class SQLiteVersionTest {

    @Test
    fun `correct version, able to parse version`() {
        val version = SQLiteVersion("3.7.11")

        assertThat(version.major, equalTo(3))
        assertThat(version.minor, equalTo(7))
        assertThat(version.patch, equalTo(11))
    }

    @Test(expected = Exception::class)
    fun `bad version, failed to parse version`() {
        SQLiteVersion("3.7.11-rc")
    }

    @Test
    fun `other version's major is below`() {
        assertThat(SQLiteVersion("3.7.11").isGreaterOrEquals(2, 11, 12), equalTo(true))
    }

    @Test
    fun `other version's minor is below`() {
        assertThat(SQLiteVersion("3.7.11").isGreaterOrEquals(3, 4, 11), equalTo(true))
    }

    @Test
    fun `other version's patch is below`() {
        assertThat(SQLiteVersion("3.7.11").isGreaterOrEquals(3, 7, 10), equalTo(true))
    }

    @Test
    fun `other version's major is above`() {
        assertThat(SQLiteVersion("3.7.11").isGreaterOrEquals(4, 1, 1), equalTo(false))
    }

    @Test
    fun `other version's minor is above`() {
        assertThat(SQLiteVersion("3.7.11").isGreaterOrEquals(3, 7, 12), equalTo(false))
    }

    @Test
    fun `other version's patch is above`() {
        assertThat(SQLiteVersion("3.7.11").isGreaterOrEquals(3, 7, 155), equalTo(false))
    }

    @Test
    fun `other version is equals`() {
        assertThat(SQLiteVersion("3.71.11").isGreaterOrEquals(3, 71, 11), equalTo(true))
    }

}