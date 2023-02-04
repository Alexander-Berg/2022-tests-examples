package ru.yandex.vertis.billing.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.gens.{OfferBillingKnownCampaignGen, Producer}
import ru.yandex.vertis.billing.util.TskvByTypeConverterSpec._
import ru.yandex.vertis.billing.util.tskv.TskvByTypeConverter

/**
  * Specs on [[TskvByTypeConverter]]
  *
  * @author alesavin
  */
class TskvByTypeConverterSpec extends AnyWordSpec with Matchers {

  import TskvByTypeConverter._

  "TskvByTypeConverter" should {

    "convert OfferBilling to kv" in {
      val ob = OfferBillingKnownCampaignGen.next
      val kv = toKv("ob")(ob).toList
      kv.foreach(i => info(i))
      kv.filter(_.startsWith("ob.KnownCampaign.isKnown=")).map(getValue) should
        be(List("true"))
      kv.filter(_.startsWith("ob.KnownCampaign.CampaignHeader.Order.OrderBalance2.totalSpent=")).map(getValue) should
        be(List(ob.campaign.order.balance2.totalSpent.toString))
    }

    "convert two implementations to the diff log view" in {
      val kv1 = toKv("ob")(A("1")).mkString("\t")
      val kv2 = toKv("ob")(B("1")).mkString("\t")
      kv1 should not be kv2
    }

    "convert multi arguments to one tskv line" in {
      val ob = OfferBillingKnownCampaignGen.next
      val product = ob.campaign.product
      val customer = ob.campaign.customer

      implicit val tskvFormat = "realty-front-log"

      val line = toTskv(
        Map(
          "product" -> product,
          "customer" -> customer
        )
      )
      info(line.get)
    }
  }
}

object TskvByTypeConverterSpec {

  def getValue(s: String) = s.split("=").last

  sealed trait TestTrait {

    def field: String
  }
  case class A(field: String) extends TestTrait
  case class B(field: String) extends TestTrait

}
