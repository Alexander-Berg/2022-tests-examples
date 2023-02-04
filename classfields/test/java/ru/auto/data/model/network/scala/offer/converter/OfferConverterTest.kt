package ru.auto.data.model.network.scala.offer.converter

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.network.scala.draft.NWDraft
import ru.auto.testextension.FileTestUtils

/**
 * @author dumchev on 17.07.17.
 */
@RunWith(AllureRunner::class) class OfferConverterTest {


    @Test
    fun `convert offer from draft response`() {
        val draft: NWDraft = FileTestUtils.readJsonAsset("/assets/draft", NWDraft::class.java)

        val nwOffer = draft.offer

        val offer = OfferConverter().fromNetwork(nwOffer!!)
        println(offer)
    }
}
