package com.edadeal.android.ui.main

import com.edadeal.android.Res
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class AppSharerShareOfferTest(
    private val expectedUrl: String,
    private val host: String,
    private val locationSlug: String,
    private val offerId: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<String>> = listOf(
            arrayOf(
                "https://edadeal.ru/moskva/offers/e29c01de-4b18-4d1d-bda1-7734b8b76442?utm_source=share&utm_content=android&utm_medium=cards",
                Res.SHARE_URL, "moskva", "e29c01de-4b18-4d1d-bda1-7734b8b76442"
            ),
            arrayOf(
                "https://edadeal.ru/kazan/offers/248b3f6a-d9e9-47dc-8091-2f586e6ce95f?utm_source=share&utm_content=android&utm_medium=cards",
                Res.SHARE_URL, "kazan", "248b3f6a-d9e9-47dc-8091-2f586e6ce95f"
            )
        )
    }

    @Test
    fun `shareOffer should return correct Url built with provided params`() {
        assertEquals(expectedUrl, AppSharer.shareOffer(host, locationSlug, offerId))
    }
}
