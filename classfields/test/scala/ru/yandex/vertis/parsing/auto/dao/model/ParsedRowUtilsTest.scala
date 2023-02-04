package ru.yandex.vertis.parsing.auto.dao.model

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRowUtils.RichParsedOfferOrBuilder
import ru.yandex.vertis.parsing.auto.diffs.OfferFields

/**
  * Created by andrey on 1/9/18.
  */
@RunWith(classOf[JUnitRunner])
class ParsedRowUtilsTest extends FunSuite {
  test("getPhones") {
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone("phone0")
    (1 to 10).sliding(2).foreach {
      case Seq(i1, i2) =>
        offer
          .addStatusHistoryBuilder()
          .addDiffBuilder()
          .setName(OfferFields.Phones)
          .setOldValue("phone1, phone2")
          .setNewValue("phone2, phone3")
    }
    val phones1: Seq[String] = offer.build().getCurrentPhones
    assert(phones1.length == 1)
    assert(phones1.head == "phone0")
    offer.getOfferBuilder.getSellerBuilder.clearPhones()
    val phones2: Seq[String] = offer.build().getCurrentPhones
    assert(phones2.isEmpty)
  }
}
