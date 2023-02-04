package ru.yandex.auto.subscriptions

import org.scalatest.FunSuite
import ru.auto.api.ApiOfferModel.Offer
import ru.yandex.auto.clone.unifier.modifier.SearchTag
import ru.yandex.auto.subscriptions.OfferUtils._

import scala.collection.JavaConversions._

class OfferUtilsTest extends FunSuite {
  test("hasAnyTag") {
    val offer = Offer
      .newBuilder()
      .addAllTags(Seq(SearchTag.HISTORY_DISCOUNT.getValue, SearchTag.HISTORY_INCREASE.getValue))
      .build()

    assert(offer.hasAnyTag(SearchTag.HISTORY_DISCOUNT, SearchTag.HISTORY_INCREASE))
  }
}
