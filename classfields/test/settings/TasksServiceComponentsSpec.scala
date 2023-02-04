package ru.yandex.vertis.billing.settings

import org.scalatest.Ignore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.event.Generator._
import ru.yandex.vertis.billing.event.{EventsProviders, Extractor}
import ru.yandex.vertis.billing.model_core.ServiceObject.Kinds
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.model_core.{PartnerOfferId, ServiceObject}

import scala.util.{Failure, Success}

/**
  * Spec on [[TasksServiceComponents]]
  *
  * @author ruslansd
  */
@Ignore //todo(darl) тест не запускался под мавеном (и падал при запуске)
class TasksServiceComponentsSpec extends AnyWordSpec with Matchers with EventsProviders {

  "RealtyTasksServiceComponents" should {

    "get event with valid offer id" in {
      val siteId = "20504"
      val partner = "24900"
      val valid = EventRecordGen.next
        .withValue(Extractor.SiteIdCellName, siteId)
        .withValue(Extractor.PhoneOwnerCellName, s"ag:$partner")

      RealtyTasksServiceComponents.getOfferId(valid) match {
        case Success(offer: ServiceObject) =>
          offer.kind should be(Kinds.NewBuilding)
          offer.partnerId should be(partner)
          offer.id should be(siteId)
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "get event with valid offer id another format" in {
      val siteId = "20504"
      val partner = "24900"
      val valid = EventRecordGen.next
        .withValue(Extractor.SiteIdCellName, siteId)
        .withValue(Extractor.PhoneOwnerCellName, s"ag:$partner")

      RealtyTasksServiceComponents.getOfferId(valid) match {
        case Success(offer: ServiceObject) =>
          offer.kind should be(Kinds.NewBuilding)
          offer.partnerId should be(partner)
          offer.id should be(siteId)
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "offer_id if phone owner doesn't exist" in {
      val siteId = "20504"
      val offerId = "205043232"
      val valid = EventRecordGen.next
        .withValue(Extractor.SiteIdCellName, siteId)
        .withValue(Extractor.OfferIdCellName, offerId)

      RealtyTasksServiceComponents.getOfferId(valid) match {
        case Success(offer: ServiceObject) =>
          offer.kind should be(Kinds.NewBuilding)
          offer.partnerId should be(offerId)
          offer.id should be(siteId)
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "fail without card_type" in {
      val siteId = "20504"
      val offerId = "205043232"
      val valid = EventRecordGen.next
        .withValue(Extractor.SiteIdCellName, siteId)
        .withValue(Extractor.OfferIdCellName, offerId)

      RealtyTasksServiceComponents.getOfferId(valid) match {
        case Failure(_) =>
          info("Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }
  }

}
