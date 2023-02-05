package com.edadeal.android.data.check

import android.net.Uri
import com.edadeal.android.dto.Check
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PrefsCheckRepositoryImplementationTest {

    private val fieldSeparator = PrefsCheckRepository.FIELD_SEPARATOR
    private val qr = "t=20180715T1401&s=195.00&fn=8712000101128832&i=27091&fp=987338047&n=1"
    private val escapedQr = Uri.encode(qr)
    private val time = "1990-12-31T23:59:59Z"
    private val scannerStrategy = "${fieldSeparator}auto$fieldSeparator"

    private fun encode(check: Check): String = PrefsCheckRepository.encode(check)
    private fun decode(string: String): Check? = PrefsCheckRepository.decode(string)
    private fun encodeDecodeCheck(check: Check): Check? = encode(check).let(::decode)

    private fun assertEncodeDecodeIsCorrect(expected: Check) {
        val decoded = encodeDecodeCheck(expected)
        assertEquals(expected, decoded)
    }

    @Test
    fun `decoded check should be equal to check from which it was encoded (Case - all fields)`() {
        assertEncodeDecodeIsCorrect(Check(qr, time, Check.DEFAULT_INPUT_TYPE, scannerStrategy))
    }

    @Test
    fun `decoded check should be equal to check from which it was encoded (Case - without time)`() {
        assertEncodeDecodeIsCorrect(Check(qr, null, Check.DEFAULT_INPUT_TYPE, scannerStrategy))
    }

    @Test
    fun `decoded check should not be equal to check from which it was not encoded`() {
        val checkToEncodeDecode = Check("1", "a", Check.DEFAULT_INPUT_TYPE, scannerStrategy)
        val anotherCheck = Check("2", "b", Check.DEFAULT_INPUT_TYPE, scannerStrategy)
        assertNotEquals(anotherCheck, checkToEncodeDecode)
        val decoded = encodeDecodeCheck(checkToEncodeDecode)
        assertNotEquals(anotherCheck, decoded)
    }

    @Test
    fun `string, encoded from check without time should has qr value and default input type with separators`() {
        val excepted = escapedQr + fieldSeparator + fieldSeparator + Check.DEFAULT_INPUT_TYPE.toString() + fieldSeparator
        val check = Check(qr, null, Check.DEFAULT_INPUT_TYPE, null)
        val encoded = encode(check)
        assertEquals(excepted, encoded)
    }

    @Test
    fun `string with qr and separator should be decoded to check with same qr, null time and default input type`() {
        val expected = Check(qr, null, Check.DEFAULT_INPUT_TYPE, null)
        val encoded = "$escapedQr$fieldSeparator"
        val decoded = decode(encoded)
        assertEquals(expected, decoded)
    }

    @Test
    fun `string with qr, separator and time should be decoded to check with same qr, time and default input type`() {
        val expected = Check(qr, time, Check.DEFAULT_INPUT_TYPE, null)
        val encoded = "$escapedQr$fieldSeparator$time"
        val decoded = decode(encoded)
        assertEquals(expected, decoded)
    }
}
