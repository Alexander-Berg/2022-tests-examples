package com.edadeal.android.ui.receiptinput

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ReceiptToQrStringTest(private val receipt: Receipt, private val qrString: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                Receipt(2018, 7, 27, 13, 51, "473.10", "9288000100086466", "2512", "403920071"),
                "t=20180727T1351&s=473.10&fn=9288000100086466&i=2512&fp=403920071&n=1"
            ),
            arrayOf(
                Receipt(2018, 3, 3, 16, 45, "5254.33", "8710000100545944", "98504", "3953104112"),
                "t=20180303T1645&s=5254.33&fn=8710000100545944&i=98504&fp=3953104112&n=1"
            ),
            arrayOf(
                Receipt(2018, 7, 15, 14, 1, "195.00", "8712000101128832", "27091", "987338047"),
                "t=20180715T1401&s=195.00&fn=8712000101128832&i=27091&fp=987338047&n=1"
            ),
            arrayOf(
                Receipt(2017, 12, 20, 8, 37, "189.00", "8710000100650632", "85237", "4120781343"),
                "t=20171220T0837&s=189.00&fn=8710000100650632&i=85237&fp=4120781343&n=1"
            )
        )
    }

    @Test
    fun `toQrString should correctly compose Receipt entity fields to qr code string`() {
        assertEquals(qrString, receipt.toQrString())
    }
}
