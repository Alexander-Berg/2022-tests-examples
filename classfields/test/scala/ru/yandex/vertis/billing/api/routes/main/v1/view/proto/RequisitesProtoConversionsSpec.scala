package ru.yandex.vertis.billing.api.routes.main.v1.view.proto

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.api.routes.main.v1.view.proto.RequisitesProtoConversions.{
  RequisitesConversion,
  RequisitesIdResponseConversion,
  RequisitesResponseConversion
}
import ru.yandex.vertis.billing.model_core.gens.{Producer, RequisitesGen}

class RequisitesProtoConversionsSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  "requisites proto conversions" should {

    "convert arbitrary requisites" in {
      forAll(RequisitesGen) { requisites =>
        val proto = RequisitesConversion.to(requisites)
        requisites shouldEqual RequisitesConversion.from(proto)
      }
    }

    "convert list of requisites" in {
      val records = RequisitesGen.next(5)
      val proto = RequisitesResponseConversion.to(records)
      records should contain theSameElementsAs RequisitesResponseConversion.from(proto)
    }

    "convert requisites id to proto" in {
      val id = Gen.posNum[Long].next
      val proto = RequisitesIdResponseConversion.to(id)
      id should be(RequisitesIdResponseConversion.from(proto))
    }
  }
}
