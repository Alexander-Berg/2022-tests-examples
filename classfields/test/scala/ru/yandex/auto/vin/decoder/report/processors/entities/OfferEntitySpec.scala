package ru.yandex.auto.vin.decoder.report.processors.entities

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.offer.VinOffersManager
import ru.yandex.auto.vin.decoder.model.offer.OfferGroup
import ru.yandex.auto.vin.decoder.model.vin
import ru.yandex.auto.vin.decoder.model.vin.{MileageInfo, Offer}
import ru.yandex.auto.vin.decoder.proto.VinHistory.{KmageInfo, VinInfo}

import scala.jdk.CollectionConverters.IterableHasAsJava

class OfferEntitySpec extends AnyWordSpecLike with Matchers {

  private val vinCode = "JTMHT05J605098077"

  "OfferEntity" should {

    "take first mileage and date from offer history" in {
      val mileageHistory = List(
        MileageInfo(3000, 500, editedByModerator = false),
        MileageInfo(5000, 1000, editedByModerator = false),
        MileageInfo(8000, 2000, editedByModerator = false)
      )
      val vinInfo = protoOffer("1234", timestamp = 1000, dateOfplacement = 500, mileageHistory = mileageHistory)
      val current = Offer(vinInfo)
      val offerGroup = OfferGroup(List(vinInfo), EventType.AUTORU_OFFER)
      val offerEntity = OfferEntity(offerGroup, current, Map.empty, vinCode)

      offerEntity.getKmageOrig.get shouldBe 3000
      offerEntity.getDate shouldBe 500
    }

    "take mileage and date of placement from offer then mileage history is empty" in {
      val vinInfo = protoOffer("1234", timestamp = 1000, dateOfplacement = 500, mileage = 2000, mileageHistory = Nil)
      val current = Offer(vinInfo)
      val offerGroup = OfferGroup(List(vinInfo), EventType.AUTORU_OFFER)
      val offerEntity = OfferEntity(offerGroup, current, Map.empty, vinCode)

      offerEntity.getKmageOrig.get shouldBe 2000
      offerEntity.getDate shouldBe 500
    }
  }

  "work fine together with OfferKmAgeChangeEntity" in {
    val vinInfo = createVinInfo(kmAgeHistory = List(1, 1, 2, 503, 10000, 12, 22, 122))
    val offerGroup = OfferGroup(List(vinInfo), EventType.AUTORU_OFFER)
    val offer = Offer(vinInfo)
    val oe = OfferEntity(offerGroup, offer, Map.empty, vinCode)
    val changeEntities = offerGroup.kmAgeChangeHistory
      .map(OfferMileageChangeEntity(offerGroup, offer, Map.empty, _))
    val history = (oe +: changeEntities).sortBy(_.getDate)

    vinInfo.getKmage shouldBe 122
    oe.getKmage shouldBe Some(1)
    history.map(_.getKmage) shouldBe List(1 /*OfferEntity*/, 503, 10000, 12, 122).map(Some(_))
  }

  private def createVinInfo(kmAgeHistory: List[Int]): VinInfo = {
    val vih = VinInfo.newBuilder
    if (kmAgeHistory.isEmpty) {
      vih.build
    } else {
      val hist = kmAgeHistory.zipWithIndex
        .map { case (h, ind) =>
          KmageInfo.newBuilder
            .setKmage(h)
            .setUpdateTimestamp(Timestamps.fromMillis(ind + 1L))
            .build
        }
      vih
        .setOfferId("fake_offer")
        .setKmage(kmAgeHistory.last)
        .addAllKmageHistory(hist.asJava)
        .build
    }
  }

  def protoOffer(
      offerId: String,
      mark: String = "BMW",
      model: String = "5ER",
      timestamp: Long,
      dateOfplacement: Long,
      price: Int = 5000,
      mileage: Int = 5000,
      mileageHistory: List[vin.MileageInfo] = Nil,
      status: OfferStatus = OfferStatus.ACTIVE,
      ignore: Boolean = false,
      ignoreReason: String = VinOffersManager.IGNORE_BY_SUPPORT): VinInfo = {
    VinInfo
      .newBuilder()
      .setOfferId(offerId)
      .setMark(mark)
      .setModel(model)
      .setTimestamp(timestamp)
      .setDateOfPlacement(dateOfplacement)
      .setPrice(price)
      .setKmage(mileage)
      .addAllKmageHistory(mileageHistory.map { historyItem =>
        val seconds = historyItem.updateTimestamp / 1000
        val nanos = (historyItem.updateTimestamp % 1000) * 1000000
        KmageInfo
          .newBuilder()
          .setKmage(historyItem.mileage)
          .setUpdateTimestamp(Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos.toInt))
          .build()
      }.asJava)
      .setOfferStatus(status)
      .setIgnored(ignore)
      .setIgnoreReason(ignoreReason)
      .addEquipped("wheels")
      .build()
  }
}
