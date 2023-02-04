package ru.yandex.realty.unifier

import java.util
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.realty.util.process.{AbstractProcessor, Processor}

import scala.concurrent.{ExecutionContext, Future}

class SkipPremoderationAndRevokeAndWithouPhotosErrorTest extends AsyncSpecBase with Matchers with MockFactory {

  implicit val trace: Traced = Traced.empty

  "SkipPremoderationAndRevokeErrorTest" should {

    val processor: Processor[OfferWrapper] = new DumbProcessor with SkipPremoderationAndRevokeError
    val processorWithPhotoFilter: Processor[OfferWrapper] = new DumbProcessor with SkipWithouPhotosError
    with SkipPremoderationAndRevokeError
    val offerWrapper = mock[OfferWrapper]
    val offer = mock[Offer]

    "process" in {
      (offerWrapper.getOffer _).expects().returns(offer).repeat(5)

      (offer.isPremoderation _).expects().returns(false)
      (offer.isRevoked _).expects().returns(false)
      (offer.hasShowableImages _).expects().returns(true)

      (offer.isOk _).expects().returns(true)
      (offer.setActive _).expects(*).once()

      processorWithPhotoFilter.process(offerWrapper).futureValue
    }

    "doesn't process when offer is revoked" in {
      (offerWrapper.getOffer _).expects().returns(offer).twice()

      (offer.isPremoderation _).expects().returns(false)
      (offer.isRevoked _).expects().returns(true)

      processor.process(util.Arrays.asList[OfferWrapper](offerWrapper)).futureValue
      (offer.setActive _).expects(*).never()
    }

    "doesn't process when offer is premoderation" in {
      (offerWrapper.getOffer _).expects().returns(offer).once()

      (offer.isPremoderation _).expects().returns(true)

      processor.process(offerWrapper).futureValue
      (offer.setActive _).expects(*).never()
    }

    "doesn't process when offer without photos" in {
      (offerWrapper.getOffer _).expects().returns(offer).repeat(3)

      (offer.isPremoderation _).expects().returns(false)
      (offer.isRevoked _).expects().returns(false)
      (offer.hasShowableImages _).expects().returns(false)

      processorWithPhotoFilter.process(offerWrapper).futureValue
      (offer.setActive _).expects(*).never()
    }

    "process list" in {
      (offerWrapper.getOffer _).expects().returns(offer).repeat(9)

      (offer.isPremoderation _).expects().returns(false).twice()
      (offer.isRevoked _).expects().returns(false).twice()
      (offer.hasShowableImages _).expects().returns(true).twice()
      (offer.isOk _).expects().returns(true).twice()

      (offer.setActive _).expects(*).once()

      processorWithPhotoFilter.process(util.Arrays.asList(offerWrapper)).futureValue
    }

    "doesn't process list when offer is revoked" in {
      (offerWrapper.getOffer _).expects().returns(offer).twice()

      (offer.isPremoderation _).expects().returns(false)
      (offer.isRevoked _).expects().returns(true)

      processor.process(util.Arrays.asList(offerWrapper)).futureValue
      (offer.setActive _).expects(*).never()
    }

    "doesn't process list when offer is premoderation" in {
      (offerWrapper.getOffer _).expects().returns(offer).once()

      (offer.isPremoderation _).expects().returns(true)

      processor.process(util.Arrays.asList(offerWrapper)).futureValue
      (offer.setActive _).expects(*).never()
    }

    "doesn't process list when offer without photos" in {
      (offerWrapper.getOffer _).expects().returns(offer).repeat(3)

      (offer.isPremoderation _).expects().returns(false)
      (offer.isRevoked _).expects().returns(false)
      (offer.hasShowableImages _).expects().returns(false)

      processorWithPhotoFilter.process(util.Arrays.asList(offerWrapper)).futureValue
      (offer.setActive _).expects(*).never()
    }
  }
}

class DumbProcessor(implicit override val ec: ExecutionContext) extends AbstractProcessor[OfferWrapper] {

  override def process(t: OfferWrapper)(implicit trace: Traced): Future[Unit] = Future {
    t.getOffer.setActive(true)
  }
}
