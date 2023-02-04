package ru.yandex.auto.vin.decoder.model.offer

import com.google.protobuf.util.Timestamps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import ru.auto.api.CommonModel.Photo
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.offer.VinOffersManager
import ru.yandex.auto.vin.decoder.proto.VinHistory.{KmageInfo, VinInfo}

import scala.jdk.CollectionConverters._

class OfferGroupTest extends AnyWordSpecLike with MockitoSugar with Matchers {

  def buildOffer(
      uid: String = "",
      kmAge: Int = 0,
      beaten: Boolean = false,
      kmAgeHistory: Seq[Int] = Nil): VinInfo = {
    val history = kmAgeHistory.zipWithIndex.map { case (kmAge, ind) =>
      KmageInfo.newBuilder
        .setKmage(kmAge)
        .setUpdateTimestamp(Timestamps.fromMillis(ind + 1L))
        .build
    }
    VinInfo
      .newBuilder()
      .setUid(uid)
      .setKmage(kmAge)
      .addAllKmageHistory(history.asJava)
      .setBeaten(beaten)
      .build()
  }

  "glueOffers" must {
    "return no groups for empty list of offers" in {
      VinOffersManager.glueOffers(List.empty, EventType.AUTORU_OFFER).size shouldBe 0
    }

    "return single group for single offer" in {
      val o = buildOffer()
      val groups = VinOffersManager.glueOffers(List(o), EventType.AUTORU_OFFER)
      groups.size shouldBe 1
      groups.head.composite shouldBe o
    }

    "put offers in different groups if beaten state changes" in {
      val notBeaten = buildOffer(uid = "1", kmAge = 2)
      val beaten = buildOffer(uid = "1", kmAge = 2, beaten = true)
      val groups = VinOffersManager.glueOffers(List(notBeaten, beaten), EventType.AUTORU_OFFER)
      groups.size shouldBe 2
      groups.head.composite shouldBe notBeaten
      groups(1).composite shouldBe beaten
    }

    "put offers in different groups if kmage changes" in {
      val o1 = buildOffer("1", 2)
      val o2 = buildOffer("1", 3)
      val groups = VinOffersManager.glueOffers(List(o1, o2), EventType.AUTORU_OFFER)
      groups.size shouldBe 2
      groups.head.composite shouldBe o1
      groups(1).composite shouldBe o2
    }

    "put offers in different groups if uid changes" in {
      val o1 = buildOffer("1", 2)
      val o2 = buildOffer("2", 2)
      val groups = VinOffersManager.glueOffers(List(o1, o2), EventType.AUTORU_OFFER)
      groups.size shouldBe 2
      groups.head.composite shouldBe o1
      groups(1).composite shouldBe o2
    }

    "combine offers in single group if they matches" in {
      val o1 = buildOffer("12", 22, true)
      val o2 = buildOffer("12", 22, true)
      val groups = VinOffersManager.glueOffers(List(o1, o2), EventType.AUTORU_OFFER)
      groups.size shouldBe 1
      groups.head.offers.head shouldBe o1
      groups.head.offers(1) shouldBe o2
    }
  }

  "listKmAgeHistoryInfo" should {
    "filter out changes less than 500 km, except last one" in {
      val o1 = buildOffer("12", 122, false, Seq(1, 1, 2, 503, 10000, 12, 22, 122))
      val groups = VinOffersManager.glueOffers(List(o1), EventType.AUTORU_OFFER)
      val history = groups.head.kmAgeHistory
      val changeHistory = groups.head.kmAgeChangeHistory

      history.map(_.getKmage) shouldBe List(1, 503, 10000, 12, 122)
      changeHistory.size shouldBe 4
    }

    "filter out all <= 0 kmages except first one. It can be equal to 0" in {
      val o1 = buildOffer("12", 10, false, Seq(0, 0, 1, -1000, 2000, 0, 10))
      val groups = VinOffersManager.glueOffers(List(o1), EventType.AUTORU_OFFER)
      val history = groups.head.kmAgeHistory
      val changeHistory = groups.head.kmAgeChangeHistory

      history.map(_.getKmage) shouldBe List(0, 2000, 10)
      changeHistory.size shouldBe 2
    }

    "put offer with more images as first in a group" in {
      val o1 = buildOffer("1").toBuilder
        .addImages(Photo.newBuilder().build())
        .build()
      val o2 = buildOffer("1").toBuilder
        .addImages(Photo.newBuilder().build())
        .addImages(Photo.newBuilder().build())
        .addImages(Photo.newBuilder().build())
        .build()
      val groups = VinOffersManager.glueOffers(List(o1, o2), EventType.AUTORU_OFFER)
      groups.size shouldBe 1
      groups.head.offers.size shouldBe 2
      groups.head.composite shouldBe o2
    }
  }
}
