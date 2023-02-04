package ru.yandex.vos2.watching.stages.products

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.offer.CampaignType._
import ru.yandex.vos2.watching.ProcessingState

import scala.collection.JavaConverters._
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ProductsCleanupStageSpec extends WordSpec with Matchers {

  private val interval = 1.minute
  private val stage = new ProductsCleanupStage(interval)

  "ProductsCleanupStage" should {

    "remove expired products" in {
      val duration = 100
      val watermark = System.currentTimeMillis() - interval.toMillis - duration
      val received = watermark - 1
      val product1 = createProduct(CAMPAIGN_TYPE_PREMIUM, duration = duration, start = received, active = false)
      val product2 = createProduct(CAMPAIGN_TYPE_PLACEMENT, duration = duration - 10, start = received)
      val builder = newOfferWithProducts(Seq(product1, product2))
      val result = stage.process(ProcessingState(builder.build(), builder.build()))
      result.offer.getOfferRealty.getProductsList shouldBe empty
    }

    "keep not expired inactive products" in {
      val duration = 100
      val watermark = System.currentTimeMillis() - interval.toMillis - duration
      val received = watermark - 1
      val product1 = createProduct(CAMPAIGN_TYPE_PREMIUM, duration = duration, start = received, active = false)
      val product2 = createProduct(CAMPAIGN_TYPE_PLACEMENT, duration = duration * 2, start = received, active = false)
      val builder = newOfferWithProducts(Seq(product1, product2))
      val result = stage.process(ProcessingState(builder.build(), builder.build()))
      result.offer.getOfferRealty.getProductsList.asScala shouldBe Seq(product2)
    }
  }
}
