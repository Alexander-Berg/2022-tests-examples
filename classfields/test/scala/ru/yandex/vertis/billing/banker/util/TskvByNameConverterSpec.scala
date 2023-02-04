package ru.yandex.vertis.billing.banker.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.banker.model.gens.{AccountTransactionGen, Producer}
import ru.yandex.vertis.billing.banker.util.TskvByNameConverterSpec.{getValue, A, B}

/**
  * Specs on [[TskvByNameConverter]]
  */
class TskvByNameConverterSpec extends AnyWordSpec with Matchers {

  import TskvByNameConverter._

  "TskvByNameConverter" should {

    "convert OfferBilling to kv" in {
      val at = AccountTransactionGen.next
      val kv = toKv("at")(at).toList
      kv.foreach(i => info(i))
      kv.filter(_.startsWith("at.id.type=")).map(getValue) should
        be(List(at.id.`type`.toString))
      kv.filter(_.startsWith("at.income=")).map(getValue) should
        be(List(at.income.toString))
    }

    "convert OfferBilling UnknownCampaign" in {
      val ob = AccountTransactionGen.next
      val kv = toKv("ob")(ob).toList
      kv.foreach(i => info(i))
    }

    "convert multi arguments to one tskv line" in {
      val at = AccountTransactionGen.next
      val account = at.account
      val payload = at.payload

      implicit val tskvFormat = "realty-front-log"

      val line = toTskv(
        Map(
          "account" -> account,
          "payload" -> payload
        )
      )
      info(line.get)
    }

    "convert call settings" in {
      val settings = AccountTransactionGen.next

      val lines = toKv("settings")(settings).toList
      lines.foreach(i => info(i))

      val keys = lines.map(_.split("=").head)
      require(keys.toSet.size == keys.size, s"Non-uniq keys detected")
    }
  }
}

object TskvByNameConverterSpec {

  def getValue(s: String): String = s.split("=").last

  sealed trait TestTrait {

    def field: String
  }
  case class A(field: String) extends TestTrait
  case class B(field: String) extends TestTrait
}
