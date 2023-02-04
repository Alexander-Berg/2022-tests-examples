package ru.yandex.auto.vin.decoder.model.offer

import com.google.protobuf.util.Timestamps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import ru.auto.api.CommonModel
import ru.yandex.auto.vin.decoder.model.offer.OfferChanges.{History, OfferChangesOps}
import ru.yandex.auto.vin.decoder.proto.VinHistory._

import scala.concurrent.duration.DurationLong
import scala.jdk.CollectionConverters._

class OfferChangesTest extends AnyWordSpecLike with MockitoSugar with Matchers {

  def buildOffer(
      kmAgeHistory: Seq[KmageInfo] = Nil,
      priceHistory: Seq[PriceInfo] = Nil,
      photos: Seq[CommonModel.Photo] = Nil,
      created: Long = 0): VinInfo = {
    VinInfo
      .newBuilder()
      .setUid("user:123")
      .addAllKmageHistory(kmAgeHistory.asJava)
      .addAllPriceHistory(priceHistory.asJava)
      .addAllImages(photos.asJava)
      .setDateOfPlacement(created)
      .build()
  }

  "groupedRecords" must {
    "group and filter same prices" in {

      val now = System.currentTimeMillis()
      val prev = now - 100000000

      val prices = List(
        PriceInfo
          .newBuilder()
          .setPrice(500000f)
          .setCreateTimestamp(now)
          .build(),
        PriceInfo
          .newBuilder()
          .setPrice(500000f)
          .setCreateTimestamp(now - 100)
          .build(),
        PriceInfo
          .newBuilder()
          .setPrice(500001f)
          .setCreateTimestamp(prev)
          .build(),
        PriceInfo
          .newBuilder()
          .setPrice(600000f)
          .setCreateTimestamp(prev + 1000)
          .build()
      )

      val offer = buildOffer(priceHistory = prices, created = prev)

      val grouped = offer.groupedHistoryChanges(None)

      grouped.size shouldBe 2
      val keyNow = now.millis.toDays.days.toMillis
      val keyPrev = prev.millis.toDays.days.toMillis
      grouped.find(_.date == keyNow).head.history shouldBe OfferChanges.History.WithPriceOpt(
        Some(buildPriceRecord(priceInfo = prices.head, diff = -100000))
      )
      grouped.find(_.date == keyPrev).head.history.price shouldBe Some(buildPriceRecord(prices(3)))
      grouped.find(_.date == keyPrev).head.history.kmAge shouldBe Some(
        buildKmAgeRecord(KmageInfo.newBuilder().setUpdateTimestamp(Timestamps.fromMillis(prev)).build())
      )
    }

    "group and filter changed kmAge" in {

      val now = System.currentTimeMillis()
      val prev = now - 100000000

      val kmAges = List(
        KmageInfo
          .newBuilder()
          .setKmage(10000)
          .setUpdateTimestamp(Timestamps.fromMillis(now))
          .build(),
        KmageInfo
          .newBuilder()
          .setKmage(20000)
          .setUpdateTimestamp(Timestamps.fromMillis(now - 1000))
          .build(),
        KmageInfo
          .newBuilder()
          .setKmage(500000)
          .setUpdateTimestamp(Timestamps.fromMillis(prev))
          .build(),
        KmageInfo
          .newBuilder()
          .setKmage(500000)
          .setUpdateTimestamp(Timestamps.fromMillis(prev - 100))
          .build()
      )

      val offer = buildOffer(kmAgeHistory = kmAges, created = prev)

      val grouped =
        offer.groupedHistoryChanges(Some(KmAgeConfirmed.newBuilder().setIsHidden(true).setValue(500000).build()))

      grouped.size shouldBe 2
      val keyNow = now.millis.toDays.days.toMillis
      val keyPrev = prev.millis.toDays.days.toMillis
      grouped.find(_.date == keyNow).head.history shouldBe OfferChanges.History.WithKmAgeOpt(
        Some(buildKmAgeRecord(kmAges.head))
      )

      grouped.find(_.date == keyPrev).head.history.price shouldBe Some(
        buildPriceRecord(PriceInfo.newBuilder().setCreateTimestamp(prev).build())
      )

    }

    "group many records by day" in {
      val now = System.currentTimeMillis()
      val prev = now - 100000000
      val photos = List(
        CommonModel.Photo
          .newBuilder()
          .setCreateDate(now - 100000)
          .setDeleteDate(now)
          .setIsDeleted(true)
          .build(),
        CommonModel.Photo
          .newBuilder()
          .setCreateDate(now)
          .build(),
        CommonModel.Photo
          .newBuilder()
          .setCreateDate(prev)
          .build(),
        CommonModel.Photo
          .newBuilder()
          .setCreateDate(now)
          .build(),
        CommonModel.Photo
          .newBuilder()
          .setCreateDate(now)
          .build(),
        CommonModel.Photo
          .newBuilder()
          .setCreateDate(prev)
          .build()
      )

      val kmAges = List(
        KmageInfo
          .newBuilder()
          .setKmage(50000)
          .setUpdateTimestamp(Timestamps.fromMillis(now))
          .build()
      )

      val prices = List(
        PriceInfo
          .newBuilder()
          .setPrice(500000f)
          .setCreateTimestamp(now)
          .build(),
        PriceInfo
          .newBuilder()
          .setPrice(600000f)
          .setCreateTimestamp(prev)
          .build()
      )

      val offer = buildOffer(kmAges, prices, photos)

      val grouped = offer.groupedHistoryChanges(None)
      grouped.size shouldBe 2
      val keyNow = now.millis.toDays.days.toMillis
      val keyPrev = prev.millis.toDays.days.toMillis
      grouped.find(_.date == keyNow).head.history shouldBe History(
        List(photos(1)),
        List(photos.head),
        kmAges.headOption.map(buildKmAgeRecord),
        prices.headOption.map(x => buildPriceRecord(x, -100000))
      )
      grouped.find(_.date == keyPrev).head.history shouldBe History(
        photosAdded = List(photos(2)),
        photosDeleted = Nil,
        kmAge = None,
        price = Some(buildPriceRecord(prices(1)))
      )
    }
  }

  private def buildPriceRecord(priceInfo: PriceInfo, diff: Int = 0) =
    PriceHistoryRecord.newBuilder().setPriceDiff(diff).setPriceInfo(priceInfo).build()
  private def buildKmAgeRecord(kmAge: KmageInfo) = KmageHistoryRecord.newBuilder().setKmageInfo(kmAge).build()

}
