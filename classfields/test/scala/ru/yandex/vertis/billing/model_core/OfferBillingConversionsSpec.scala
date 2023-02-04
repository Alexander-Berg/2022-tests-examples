package ru.yandex.vertis.billing.model_core

import java.io.ByteArrayOutputStream
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.OfferBillingConversionsSpec._
import ru.yandex.vertis.billing.model_core.bytes.Conversions
import ru.yandex.vertis.billing.model_core.gens.{
  OfferBillingGen,
  OfferBillingKnownCampaignGen,
  OfferBillingUnknownCampaignGen,
  Producer
}
import ru.yandex.vertis.billing.model_core.proto.{Conversions => Proto}

/**
  * Specs on offer billing conversions
  *
  * @author alesavin
  */
class OfferBillingConversionsSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  "OfferBilling" should {

    "write and read one offer billing known campaign" in {
      forAll(OfferBillingKnownCampaignGen) { ob =>
        val output = serialize(ob)
        val ob2 = Conversions.offerBillingFromBytes(output).get
        assert(withCampaingStatus(ob) === ob2)
      }
    }
    "write and read one offer billing" in {
      forAll(OfferBillingUnknownCampaignGen) { ob =>
        val ob = OfferBillingUnknownCampaignGen.next(1).head
        val output = serialize(ob)
        val ob2 = Conversions.offerBillingFromBytes(output).get
        assert(withCampaingStatus(ob) === ob2)
      }
    }

    "write delimited and read one offer billing with error" in {
      val ob = OfferBillingGen.next(1).head
      val pob = Proto.toMessage(ob)

      val output = new ByteArrayOutputStream()
      pob.writeDelimitedTo(output)
      output.close()

      intercept[Exception] {
        Conversions.offerBillingFromBytes(output.toByteArray).get
        fail("expected deserialization failure")
      }
    }
  }

  def serialize(ob: OfferBilling): Array[Byte] = {
    val data = Conversions.toByteArray(ob)

    val output = new ByteArrayOutputStream()
    output.write(data)
    output.close()
    output.toByteArray
  }
}

object OfferBillingConversionsSpec {

  def withCampaingStatus(ob: OfferBilling, st: Option[CampaignStatus] = None) = ob match {
    case kc: OfferBilling.KnownCampaign =>
      kc.copy(campaign = kc.campaign.copy(status = st))
    case _ => ob
  }

}
