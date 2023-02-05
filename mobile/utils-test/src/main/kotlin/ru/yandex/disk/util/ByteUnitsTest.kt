package ru.yandex.disk.util

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import ru.yandex.disk.spaceutils.ByteUnit
import ru.yandex.disk.test.Assert2

class ByteUnitsTest : Assert2() {

    @Test
    fun `should convert from bytes`() {
        assertThat(ByteUnit.BYTES.toBytes(1), equalTo(1L))
        assertThat(ByteUnit.BYTES.toKB(1024), equalTo(1L))
        assertThat(ByteUnit.BYTES.toMB(1_048_576), equalTo(1L))
        assertThat(ByteUnit.BYTES.toGB(1_073_741_824), equalTo(1L))
        assertThat(ByteUnit.BYTES.toTB(1_099_511_627_776), equalTo(1L))
    }

    @Test
    fun `should convert from kilobytes`() {
        assertThat(ByteUnit.KB.toBytes(1), equalTo(1024L))
        assertThat(ByteUnit.KB.toKB(1L), equalTo(1L))
        assertThat(ByteUnit.KB.toMB(1024), equalTo(1L))
        assertThat(ByteUnit.KB.toGB(1_048_576), equalTo(1L))
        assertThat(ByteUnit.KB.toTB(1_073_741_824), equalTo(1L))
    }

    @Test
    fun `should convert from megabytes`() {
        assertThat(ByteUnit.MB.toBytes(1), equalTo(1_048_576L))
        assertThat(ByteUnit.MB.toKB(1), equalTo(1024L))
        assertThat(ByteUnit.MB.toMB(1), equalTo(1L))
        assertThat(ByteUnit.MB.toGB(1024), equalTo(1L))
        assertThat(ByteUnit.MB.toTB(1_048_576), equalTo(1L))
    }

    @Test
    fun `should convert from gigabytes`() {
        assertThat(ByteUnit.GB.toBytes(1), equalTo(1_073_741_824L))
        assertThat(ByteUnit.GB.toKB(1), equalTo(1_048_576L))
        assertThat(ByteUnit.GB.toMB(1), equalTo(1024L))
        assertThat(ByteUnit.GB.toGB(1), equalTo(1L))
        assertThat(ByteUnit.GB.toTB(1024), equalTo(1L))
    }

    @Test
    fun `should convert from terabytes`() {
        assertThat(ByteUnit.TB.toBytes(1), equalTo(1_099_511_627_776L))
        assertThat(ByteUnit.TB.toKB(1), equalTo(1_073_741_824L))
        assertThat(ByteUnit.TB.toMB(1), equalTo(1_048_576L))
        assertThat(ByteUnit.TB.toGB(1), equalTo(1024L))
        assertThat(ByteUnit.TB.toTB(1), equalTo(1L))
    }

    @Test
    fun `should prevent overflow`() {
        assertThat(ByteUnit.TB.toBytes(9999999), equalTo(Long.MAX_VALUE))
        assertThat(ByteUnit.GB.toBytes(9999999999), equalTo(Long.MAX_VALUE))
        assertThat(ByteUnit.MB.toBytes(9999999999999), equalTo(Long.MAX_VALUE))
        assertThat(ByteUnit.KB.toBytes(9999999999999999), equalTo(Long.MAX_VALUE))
    }
}