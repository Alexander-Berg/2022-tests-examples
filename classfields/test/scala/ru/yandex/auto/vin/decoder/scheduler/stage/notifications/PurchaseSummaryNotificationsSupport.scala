package ru.yandex.auto.vin.decoder.scheduler.stage.notifications

import com.google.protobuf.util.Timestamps
import ru.auto.api.ApiOfferModel
import ru.auto.api.vin.ResponseModel.RawEssentialsReportResponse
import ru.auto.api.vin.VinReportModel.PtsBlock.{IntItem, StringItem}
import ru.auto.api.vin.VinReportModel.{AutoruOffersBlock, OfferRecord, PtsBlock, RawVinEssentialsReport}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.PurchaseSummaryNotificationState.{Purchase, PurchaseSummaryInfo}
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, WatchingStateUpdate}

import java.time.Instant
import scala.concurrent.duration._
import scala.language.implicitConversions

trait PurchaseSummaryNotificationsSupport {

  protected val offerId = "1095852360-ae3f6b0b"
  protected val userRef = "user:123"
  protected val anotherUserRef = "user:5678"
  protected val clientId = 1256L
  protected val vin = VinCode("Z94CB41AAER219163")
  protected val mark = "BMW"
  protected val model = "X5"
  protected val year = 2016

  protected def buildPurchaseSummaryNotification(
      scheduledFor: Instant,
      purchases: List[Purchase.Builder] = List.empty): PurchaseSummaryInfo.Builder = {
    val info = PurchaseSummaryInfo
      .newBuilder()
      .setScheduledForTimestamp(Timestamps.fromMillis(scheduledFor.toEpochMilli))
      .setCreateTimestamp(System.currentTimeMillis())
      .setOfferId(offerId)
    purchases.foreach(info.addPurchases)
    info
  }

  protected def buildPurchase(timestamp: Instant): Purchase.Builder = {
    Purchase
      .newBuilder()
      .setClientId(clientId)
      .setUserId(anotherUserRef)
      .setTimestamp(timestamp.toEpochMilli)
  }

  protected def buildEssentialsReport: RawEssentialsReportResponse = {
    val pts =
      PtsBlock
        .newBuilder()
        .setMark(StringItem.newBuilder().setValueText(mark))
        .setModel(StringItem.newBuilder().setValueText(model))
        .setYear(IntItem.newBuilder().setValue(2016))

    val offers = AutoruOffersBlock
      .newBuilder()
      .addOffers(
        OfferRecord
          .newBuilder()
          .setOfferId(offerId)
      )

    val report =
      RawVinEssentialsReport
        .newBuilder()
        .setAutoruOffers(offers)
        .setPtsInfo(pts)

    RawEssentialsReportResponse.newBuilder().setReport(report).build()
  }

  protected def buildVosOffer: ApiOfferModel.Offer.Builder = {
    val offer = ApiOfferModel.Offer
      .newBuilder()
      .setUserRef(userRef)
      .setId(offerId)
      .setStatus(ApiOfferModel.OfferStatus.ACTIVE)

    offer.getDocumentsBuilder.setYear(year)
    offer.getCarInfoBuilder.getMarkInfoBuilder.setName(mark)
    offer.getCarInfoBuilder.getModelInfoBuilder.setName(model)
    offer
  }

  implicit protected def createProcessingState(compoundState: CompoundState.Builder): ProcessingState[CompoundState] = {
    ProcessingState(createWatchingState(compoundState))
  }

  implicit protected def createWatchingState(
      compoundState: CompoundState.Builder): WatchingStateUpdate[CompoundState] = {
    WatchingStateUpdate(compoundState.build(), DefaultDelay(48.hours))
  }
}
