package ru.yandex.realty.rent.service

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.indexer.events.IndexerEvents
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.proto.unified.offer.offertype.RentOffer
import ru.yandex.realty.proto.unified.offer.partner.{PartnerInfo, XmlInfo}
import ru.yandex.realty.proto.unified.offer.rent.YandexRentInfo
import ru.yandex.realty.proto.unified.offer.state.{OfferState, VosState}
import ru.yandex.realty.proto.unified.vos.offer.Publishing.ShowStatus
import ru.yandex.realty.rent.dao.{CleanSchemaBeforeEach, RentSpecBase}
import ru.yandex.realty.rent.service.impl.RealtyUnifiedOffersProcessorImpl
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.Mappings._

@RunWith(classOf[JUnitRunner])
class RealtyUnifiedOffersProcessorSpec extends AsyncSpecBase with RentSpecBase with CleanSchemaBeforeEach {

  implicit val trace = Traced.empty

  "RealtyUnifiedOffersProcessor.process" should {
    "update flat if corresponding unified offer with owner request id is processed" in new Wiring {
      val initialFlat = flatGen().next
      val ownerRequest = ownerRequestGen.next.copy(flatId = initialFlat.id)
      val offerId = readableString.next

      ownerRequestDao.create(Seq(ownerRequest)).futureValue
      flatDao.create(initialFlat).futureValue

      val publishedUnifiedOffer =
        createUnifiedOfferEvent(
          offerId,
          hasYandexRentInfo = true,
          Some(ownerRequest.ownerRequestId),
          ShowStatus.PUBLISHED
        )
      processor.process(publishedUnifiedOffer).futureValue

      val publishedFlat = flatDao.findById(initialFlat.id).futureValue
      publishedFlat.data.getRealtyInfo.getOfferId shouldBe offerId
      publishedFlat.data.getRealtyInfo.getIsPublished shouldBe true

      val unpublishedUnifiedOffer =
        createUnifiedOfferEvent(
          offerId,
          hasYandexRentInfo = true,
          Some(ownerRequest.ownerRequestId),
          ShowStatus.UNPUBLISHED
        )
      processor.process(unpublishedUnifiedOffer).futureValue

      val unpublishedFlat = flatDao.findById(initialFlat.id).futureValue
      unpublishedFlat.data.getRealtyInfo.getOfferId shouldBe offerId
      unpublishedFlat.data.getRealtyInfo.getIsPublished shouldBe false
    }

    "update flat if corresponding unified offer with flat id is processed" in new Wiring {
      val initialFlat = flatGen().next
      val offerId = readableString.next

      flatDao.create(initialFlat).futureValue

      val publishedUnifiedOffer =
        createUnifiedOfferEvent(
          offerId,
          hasYandexRentInfo = true,
          Some(initialFlat.flatId),
          ShowStatus.PUBLISHED
        )
      processor.process(publishedUnifiedOffer).futureValue

      val publishedFlat = flatDao.findById(initialFlat.id).futureValue
      publishedFlat.data.getRealtyInfo.getOfferId shouldBe offerId
      publishedFlat.data.getRealtyInfo.getIsPublished shouldBe true

      val unpublishedUnifiedOffer =
        createUnifiedOfferEvent(
          offerId,
          hasYandexRentInfo = true,
          Some(initialFlat.id),
          ShowStatus.UNPUBLISHED
        )
      processor.process(unpublishedUnifiedOffer).futureValue

      val unpublishedFlat = flatDao.findById(initialFlat.id).futureValue
      unpublishedFlat.data.getRealtyInfo.getOfferId shouldBe offerId
      unpublishedFlat.data.getRealtyInfo.getIsPublished shouldBe false
    }

    "ignore non-rent offers" in new Wiring {
      val offerId = readableString.next

      val update =
        createUnifiedOfferEvent(offerId, hasYandexRentInfo = false, Some("abacaba"), ShowStatus.PUBLISHED)
      processor.process(update).futureValue // should not throw
    }
  }

  private def createUnifiedOfferEvent(
    offerId: String,
    hasYandexRentInfo: Boolean,
    partnerInternalId: Option[String],
    vosShowStatus: ShowStatus
  ): IndexerEvents.UnifiedOfferEvent =
    IndexerEvents.UnifiedOfferEvent
      .newBuilder()
      .setUpsert(
        IndexerEvents.UnifiedOfferEvent.Upsert
          .newBuilder()
          .setOfferV2(
            UnifiedOffer
              .newBuilder()
              .setOfferId(offerId)
              .setRent(
                RentOffer
                  .newBuilder()
                  .applyTransformIf(hasYandexRentInfo, _.setYandexRentInfo(YandexRentInfo.newBuilder()))
              )
              .setPartner(
                PartnerInfo
                  .newBuilder()
                  .applyTransformIf(
                    partnerInternalId.nonEmpty,
                    _.setXmlInfo(XmlInfo.newBuilder().setPartnerInternalId(partnerInternalId.get))
                  )
              )
              .setOfferState(
                OfferState.newBuilder().setVosState(VosState.newBuilder().setShowStatus(vosShowStatus))
              )
          )
      )
      .build()

  private trait Wiring {
    val processor = new RealtyUnifiedOffersProcessorImpl(flatDao, ownerRequestDao)
  }
}
