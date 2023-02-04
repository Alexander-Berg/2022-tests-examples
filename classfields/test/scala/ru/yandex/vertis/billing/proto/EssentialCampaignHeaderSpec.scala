package ru.yandex.vertis.billing.proto

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.microcore_model.{Dsl, Fingerprints}
import ru.yandex.vertis.billing.model_core.gens.{CampaignHeaderGen, Producer}
import ru.yandex.vertis.billing.model_core.proto.Conversions

import scala.util.Success

/**
  * Spec for essential campaign header conversion and fingerprint.
  *
  * @see [[Dsl.essentialHeader()]]
  * @see [[ru.yandex.vertis.billing.model.proto.Conversions.toEssentialMessage()]]
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
class EssentialCampaignHeaderSpec extends AnyWordSpec with Matchers {

  "Essential campaign header" should {
    "convert to protobuf and back" in {
      val regular = CampaignHeaderGen.next(1).head
      val regularMessage = Conversions.toMessage(regular)
      val essentialMessage = Conversions.toEssentialMessage(regular)

      essentialMessage.getId should be(regularMessage.getId)
      essentialMessage.getOrder.getId should be(regularMessage.getOrder.getId)
      essentialMessage.getProduct should be(regularMessage.getProduct)
      essentialMessage.getSettings should be(regularMessage.getSettings)

      val tryEssential = Conversions.campaignHeaderFromMessage(essentialMessage)
      tryEssential match {
        case Success(essential) =>
          essential.id should be(regular.id)
          essential.order.id should be(regular.order.id)
          essential.product should be(regular.product)
          essential.settings should be(regular.settings)
        case other => fail(s"Unexpected $other")
      }
    }

    "has same Fingerprint as regular" in {
      val sources = CampaignHeaderGen.next(100)
      val regularMessages = sources.map(Conversions.toMessage(_))
      val essentialMessages = sources.map(Conversions.toEssentialMessage)

      regularMessages.zip(essentialMessages).foreach { case (regular, essential) =>
        Fingerprints.ofCampaignHeader(essential) should be(Fingerprints.ofCampaignHeader(regular))
      }
    }

  }

}
