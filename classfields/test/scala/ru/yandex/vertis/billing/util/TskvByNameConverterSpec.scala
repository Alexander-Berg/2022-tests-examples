package ru.yandex.vertis.billing.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.gens.{
  teleponyCallSettingsGen,
  OfferBillingKnownCampaignGen,
  OfferBillingUnknownCampaignGen,
  Producer
}
import ru.yandex.vertis.billing.util.TskvByNameConverterSpec._
import ru.yandex.vertis.billing.util.tskv.TskvByNameConverter

/**
  * Specs on [[TskvByNameConverter]]
  *
  * @author alesavin
  */
class TskvByNameConverterSpec extends AnyWordSpec with Matchers {

  import TskvByNameConverter._

  "TskvByNameConverter" should {

    "convert OfferBilling to kv" in {
      val ob = OfferBillingKnownCampaignGen.next
      val kv = toKv("ob")(ob).toList
      kv.foreach(i => info(i))
      kv.filter(_.startsWith("ob.isKnown=")).map(getValue) should
        be(List("true"))
      kv.filter(_.startsWith("ob.campaign.order.balance2.totalSpent=")).map(getValue) should
        be(List(ob.campaign.order.balance2.totalSpent.toString))
    }

    "convert two implementations to the diff log view" in {
      // val kv1 = toKv("ob")(A("1")).mkString("\t")
      // val kv2 = toKv("ob")(B("1")).mkString("\t")
      // kv1 should not be kv2
    }

    "convert OfferBilling UnknownCampaign" in {
      val ob = OfferBillingUnknownCampaignGen.next
      val kv = toKv("ob")(ob).toList
      kv.foreach(i => info(i))
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

    "convert call settings" in {
      val settings = teleponyCallSettingsGen().next

      // TODO(darl) not used
      // implicit val tskvFormat = "realty-front-log"

      val lines = toKv("settings")(settings).toList
      lines.foreach(i => info(i))

      val keys = lines.map(_.split("=").head)
      require(keys.toSet.size == keys.size, s"Non-uniq keys detected")
    }
  }
}

object TskvByNameConverterSpec {

  def getValue(s: String) = s.split("=").last

  sealed trait TestTrait {

    def field: String
  }
  case class A(field: String) extends TestTrait
  case class B(field: String) extends TestTrait
}
