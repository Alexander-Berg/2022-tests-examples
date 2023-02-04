package ru.yandex.vos2.watching.stages.products

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.offer.CampaignType._
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.watching.ProcessingState

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class PaidProlongStageSpec extends WordSpec with Matchers {

  "PaidProlongStage" should {

    "prolong offer if it has active products after end of show" in {
      val product1 = createProduct(CAMPAIGN_TYPE_PREMIUM, duration = 1.day.toMillis)
      val product2 = createProduct(CAMPAIGN_TYPE_RAISING, duration = 2.days.toMillis)
      val product3 = createProduct(CAMPAIGN_TYPE_PROMOTION, duration = 3.days.toMillis)
      val ttl = 10.days.toHours.toInt
      val builder = newOfferWithProducts(Seq(product1, product2, product3))
        .setOfferTTLHours(ttl)
        .setTimestampTtlStart(System.currentTimeMillis() - DAYS.toMillis(8))
      val result = PaidProlongStage.process(ProcessingState(builder.build(), builder.build()))
      val expected = System.currentTimeMillis() + HOURS.toMillis(ttl)
      val actual = result.offer.getTimestampExpire
      actual shouldBe expected +- SECONDS.toMillis(10)
    }

    "do not touch offer expiring after the latest bought product's end" in {
      val product1 = createProduct(CAMPAIGN_TYPE_PREMIUM, duration = 1.day.toMillis)
      val product2 = createProduct(CAMPAIGN_TYPE_RAISING, duration = 2.days.toMillis)
      val product3 = createProduct(CAMPAIGN_TYPE_PROMOTION, duration = 3.days.toMillis)
      val ttl = 10.days.toHours.toInt
      val original = newOfferWithProducts(Seq(product1, product2, product3))
        .setOfferTTLHours(ttl)
        .setTimestampTtlStart(System.currentTimeMillis() - HOURS.toMillis(ttl) + DAYS.toMillis(3) + 10)
        .build()
      val result = PaidProlongStage.process(ProcessingState(original, original))
      result.offer shouldBe original
    }

    "not prolong offer if its half life has not passed yet" in {
      val product1 = createProduct(CAMPAIGN_TYPE_PREMIUM, duration = 1.day.toMillis)
      val product2 = createProduct(CAMPAIGN_TYPE_RAISING, duration = 2.days.toMillis)
      val product3 = createProduct(CAMPAIGN_TYPE_PROMOTION, duration = 3.days.toMillis)
      val ttl = 10.days.toHours.toInt
      val original = newOfferWithProducts(Seq(product1, product2, product3))
        .setOfferTTLHours(ttl)
        .setTimestampTtlStart(System.currentTimeMillis() - DAYS.toMillis(1))
        .build()
      val result = PaidProlongStage.process(ProcessingState(original, original))
      result.offer shouldBe original
    }
  }
}
