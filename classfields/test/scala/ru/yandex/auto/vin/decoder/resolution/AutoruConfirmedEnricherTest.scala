package ru.yandex.auto.vin.decoder.resolution

import com.google.protobuf.BoolValue
import com.google.protobuf.util.Timestamps
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.manager.offer.VinOffersManager
import ru.yandex.auto.vin.decoder.proto.VinHistory.{KmageInfo, PriceInfo, VinInfo, VinInfoHistory}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class AutoruConfirmedEnricherTest extends AnyFunSuite with MockitoSupport {

  test("Should filter edited kmAge") {
    val ts1 = Timestamps.fromMillis(System.currentTimeMillis() - 100000)
    val ts2 = Timestamps.fromMillis(System.currentTimeMillis() - 10000)
    val ts3 = Timestamps.fromMillis(System.currentTimeMillis() - 1000)

    val kmage1 = KmageInfo
      .newBuilder()
      .setKmage(100000)
      .setUpdateTimestamp(ts1)
      .build()

    val kmage2 = KmageInfo
      .newBuilder()
      .setKmage(5000)
      .setEditedByModerator(BoolValue.of(true))
      .setUpdateTimestamp(ts2)
      .build()

    val kmage3 = KmageInfo
      .newBuilder()
      .setKmage(1000)
      .setEditedByModerator(BoolValue.of(true))
      .setUpdateTimestamp(ts3)
      .build()

    val offers = List(
      VinInfo
        .newBuilder()
        .addAllKmageHistory(
          Seq(kmage3, kmage1, kmage2).asJava
        )
        .build()
    )

    val data = VinOffersManager.hidePricesAndMileages(offers)

    assert(data.size == 1)
    assert(data.head.getKmageHistoryCount == 1)
    assert(
      data.head.getKmageHistoryList.asScala.head == kmage3.toBuilder
        .setUpdateTimestamp(ts1)
        .build()
    )
  }

  test("Should filter edited price") {
    val ts1 = System.currentTimeMillis() - 100000
    val ts2 = System.currentTimeMillis() - 10000
    val ts3 = System.currentTimeMillis()

    val price1 = PriceInfo
      .newBuilder()
      .setPrice(10000000)
      .setCreateTimestamp(ts1)
      .build()

    val price2 = PriceInfo
      .newBuilder()
      .setPrice(100000)
      .setCreateTimestamp(ts2)
      .setEditedByModerator(BoolValue.of(true))
      .build()

    val price3 = PriceInfo
      .newBuilder()
      .setPrice(990000)
      .setCreateTimestamp(ts3)
      .build()

    val offers = List(
      VinInfo
        .newBuilder()
        .addAllPriceHistory(
          Seq(price2, price1, price3).asJava
        )
        .build()
    )
    val data = VinOffersManager.hidePricesAndMileages(offers)

    assert(data.size == 1)
    assert(data.head.getPriceHistoryCount == 2)
    assert(
      data.head.getPriceHistoryList.asScala.head == price2.toBuilder
        .setCreateTimestamp(ts1)
        .build
    )
    assert(data.head.getPriceHistoryList.asScala.last == price3)
  }

  test("Should hide edited kmage if it's not marked as fake") {
    val ts1 = Timestamps.fromMillis(100)
    val ts2 = Timestamps.fromMillis(200)
    val ts3 = Timestamps.fromMillis(300)

    val kmage1 = KmageInfo.newBuilder
      .setKmage(290000)
      .setUpdateTimestamp(ts1)
      .build

    val kmage2 = KmageInfo.newBuilder
      .setKmage(300000)
      .setUpdateTimestamp(ts2)
      .build

    val kmage3 = KmageInfo.newBuilder
      .setKmage(300001)
      .setUpdateTimestamp(ts3)
      .setEditedByModerator(BoolValue.of(true))
      .build

    val kmage4 = KmageInfo.newBuilder
      .setKmage(100000)
      .setUpdateTimestamp(ts3)
      .setEditedByModerator(BoolValue.of(true))
      .setFakeSuspicionByModerator(true)
      .build

    val offers = List(
      VinInfo.newBuilder
        .addAllKmageHistory(List(kmage1, kmage2, kmage3, kmage4).asJava)
        .build
    )
    val data = VinOffersManager.hidePricesAndMileages(offers)

    assert(data.size == 1)
    assert(data.head.getKmageHistoryCount == 3)
    val kmages = data.head.getKmageHistoryList.asScala
    assert(kmages(0) == kmage1)
    assert(kmages(1) == kmage3.toBuilder.setUpdateTimestamp(kmage2.getUpdateTimestamp).build)
    assert(kmages(2) == kmage4)
  }

}
