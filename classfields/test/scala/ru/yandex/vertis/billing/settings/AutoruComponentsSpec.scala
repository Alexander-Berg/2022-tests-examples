package ru.yandex.vertis.billing.settings

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.model_core.gens.{CampaignSnapshotGen, Producer}
import ru.yandex.vertis.billing.model_core.{CampaignSnapshot, CostPerCall, DynamicPrice, FixPrice, Placement, Product}

/**
  * @author ruslansd
  */
class AutoruComponentsSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  private def callWithFixPriceSnapshotGen(time: DateTime): Gen[CampaignSnapshot] = {
    val callProduct = Product(Placement(CostPerCall(FixPrice(Gen.posNum[Int].next))))
    CampaignSnapshotGen.map { snapshot =>
      snapshot.copy(time = time, product = callProduct)
    }
  }

  private def callWithDynamicPriceSnapshotGen(time: DateTime): Gen[CampaignSnapshot] = {
    val callProduct = Product(Placement(CostPerCall(DynamicPrice())))
    CampaignSnapshotGen.map { snapshot =>
      snapshot.copy(time = time, product = callProduct)
    }
  }

  "AutoruComponents" should {
    val oldCallTime = AutoRuComponents.StartDateTimeVSBILLING3582.minusHours(2)
    val newCallTime = AutoRuComponents.StartDateTimeVSBILLING3582.plusHours(2)

    "make callPayloadCorrection for old calls" in {
      forAll(callWithFixPriceSnapshotGen(oldCallTime), Gen.posNum[Long]) { (snapshot, payload) =>
        AutoRuComponents.callPayloadCorrection(snapshot, payload) shouldBe snapshot.product.totalCost
      }
    }

    "make callPayloadCorrection for old calls (DynamicPrice)" in {
      forAll(callWithDynamicPriceSnapshotGen(oldCallTime), Gen.posNum[Long]) { (snapshot, payload) =>
        AutoRuComponents.callPayloadCorrection(snapshot, payload) shouldBe 0
      }
    }
    "make callPayloadCorrection for new calls" in {
      forAll(callWithFixPriceSnapshotGen(newCallTime), Gen.posNum[Long]) { (snapshot, payload) =>
        AutoRuComponents.callPayloadCorrection(snapshot, payload) shouldBe payload
      }
    }

    "make callPayloadCorrection for new calls (DynamicPrice)" in {
      forAll(callWithDynamicPriceSnapshotGen(newCallTime), Gen.posNum[Long]) { (snapshot, payload) =>
        AutoRuComponents.callPayloadCorrection(snapshot, payload) shouldBe payload
      }
    }
  }

}
