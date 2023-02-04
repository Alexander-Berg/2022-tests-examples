package ru.auto.api.services.statistics

import java.time.{LocalDate, OffsetDateTime, ZoneOffset}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers._
import ru.auto.api.StatisticsModel.{ProductActivationsDailyStat, ProductsActivationsDailyStat}
import ru.auto.api.managers.wallet.WalletListingParams
import ru.auto.api.model.AutoruDealer
import ru.auto.api.services.salesman.SalesmanClient.{OfferId, OffersListing}
import ru.auto.dealer_stats.proto.Rpc.{DealerProductActivationsDailyStat, DealerProductActivationsStat, DealerProductsActivationsDailyStat, GetOfferProductActivationsDailyStatsResponse, GetProductActivationsDailyStatsResponse, GetProductActivationsStatsResponse, ProductsActivationsTotalStats, TotalStat}
import ru.yandex.vertis.paging.Paging

import scala.jdk.CollectionConverters._

object MetricsTestData {

  val dealer = AutoruDealer(1872)
  val testDealer = AutoruDealer(20101)
  val stoDealer = AutoruDealer(26898)
  val from = LocalDate.of(2018, 6, 3)
  val to = LocalDate.of(2018, 6, 18)
  val pageNum = 1
  val halfPageSize = 5
  val pageSize = 10
  val params = WalletListingParams(from, Some(to), Some(pageNum + 1), Some(halfPageSize))
  val altParams = WalletListingParams(from, None, None, None)
  val page1Params = WalletListingParams(from, Some(to), pageNum = Some(1), pageSize = Some(10))
  val pageCount = 4
  val zoneOffset = ZoneOffset.of("+03:00")
  val odt1 = OffsetDateTime.of(2018, 6, 14, 23, 59, 59, 999000000, zoneOffset)
  val odt2 = OffsetDateTime.of(2018, 6, 13, 23, 59, 59, 999000000, zoneOffset)
  val odt3 = OffsetDateTime.of(2018, 6, 12, 23, 59, 59, 999000000, zoneOffset)
  val odt4 = OffsetDateTime.of(2018, 6, 7, 23, 59, 59, 999000000, zoneOffset)
  val odt5 = OffsetDateTime.of(2018, 6, 17, 23, 59, 59, 999000000, zoneOffset)

  val detailedActivationsDate = LocalDate.of(2018, 6, 17)

  val offersListing = OffersListing(List(OfferId(1073188176), OfferId(1071117871)), 2)

  val metric1 =
    DealerProductActivationsDailyStat
      .newBuilder()
      .setProduct("placement")
      .setDate(odt1.toLocalDate.toString)
      .setSpentKopecks(5791000)
      .setCount(553)
      .build()

  val metric2 = DealerProductActivationsDailyStat
    .newBuilder()
    .setProduct("quota:placement:moto")
    .setDate(odt1.toLocalDate.toString)
    .setSpentKopecks(6500)
    .setCount(1)
    .build()

  val metric3 = DealerProductActivationsDailyStat
    .newBuilder()
    .setProduct("placement")
    .setDate(odt3.toLocalDate.toString)
    .setSpentKopecks(5472000)
    .setCount(300)
    .build()

  val metric4 = DealerProductActivationsDailyStat
    .newBuilder()
    .setProduct("placement")
    .setDate(odt4.toLocalDate.toString)
    .setSpentKopecks(0)
    .setCount(365)
    .build()

  val metric5 = DealerProductActivationsDailyStat
    .newBuilder()
    .setProduct("special-offer")
    .setDate(odt1.toLocalDate.toString)
    .setSpentKopecks(15000)
    .setCount(1)
    .build()

  val metric6 = DealerProductActivationsDailyStat
    .newBuilder()
    .setProduct("boost")
    .setDate(odt2.toLocalDate.toString)
    .setSpentKopecks(5430000)
    .setCount(181)
    .build()

  val metric7 = DealerProductActivationsDailyStat
    .newBuilder()
    .setProduct("placement")
    .setDate(odt2.toLocalDate.toString)
    .setSpentKopecks(5489000)
    .setCount(548)
    .build()

  val metric8 = DealerProductActivationsDailyStat
    .newBuilder()
    .setProduct("quota:placement:moto")
    .setDate(odt2.toLocalDate.toString)
    .setSpentKopecks(6500)
    .setCount(1)
    .build()

  val metric9 = DealerProductActivationsDailyStat
    .newBuilder()
    .setProduct("placement")
    .setDate(odt3.toLocalDate.toString)
    .setSpentKopecks(5472000)
    .setCount(510)
    .build()

  val metric10 = DealerProductActivationsDailyStat
    .newBuilder()
    .setProduct("boost")
    .setDate(odt4.toLocalDate.toString)
    .setSpentKopecks(2250000)
    .setCount(75)
    .build()

  val metric11 = DealerProductActivationsDailyStat
    .newBuilder()
    .setProduct("placement")
    .setDate(odt4.toLocalDate.toString)
    .setSpentKopecks(4308500)
    .setCount(365)
    .build()

  val metric12 = DealerProductActivationsDailyStat
    .newBuilder()
    .setProduct("premium")
    .setDate(odt4.toLocalDate.toString)
    .setSpentKopecks(150000)
    .setCount(3)
    .build()

  val allMetrics =
    Seq(metric1, metric2, metric3, metric4, metric5, metric6, metric7, metric8, metric9, metric10, metric11, metric12)

  val productActivationsDailyStats = GetProductActivationsDailyStatsResponse
    .newBuilder()
    .addAllActivationStats(
      allMetrics.asJava
    )
    .setTotal(
      TotalStat
        .newBuilder()
        .setCount(allMetrics.map(_.getCount).sum)
        .setSpentKopecks(allMetrics.map(_.getSpentKopecks).sum)
    )
    .build()

  // scalastyle:off method.length
  def checkStats(stats: Vector[ProductActivationsDailyStat]): Assertion = {
    val stat0 = stats(0)
    stat0.getProduct shouldBe "placement"
    stat0.getDate shouldBe "2018-06-13"
    stat0.getCount shouldBe 548
    stat0.getSum shouldBe 54890
    val stat1 = stats(1)
    stat1.getProduct shouldBe "quota:placement:moto"
    stat1.getDate shouldBe "2018-06-13"
    stat1.getCount shouldBe 1
    stat1.getSum shouldBe 65
    val stat2 = stats(2)
    stat2.getProduct shouldBe "placement"
    stat2.getDate shouldBe "2018-06-14"
    stat2.getCount shouldBe 553
    stat2.getSum shouldBe 57910
    val stat3 = stats(3)
    stat3.getProduct shouldBe "quota:placement:moto"
    stat3.getDate shouldBe "2018-06-14"
    stat3.getCount shouldBe 1
    stat3.getSum shouldBe 65
    val stat4 = stats(4)
    stat4.getProduct shouldBe "special-offer"
    stat4.getDate shouldBe "2018-06-14"
    stat4.getCount shouldBe 1
    stat4.getSum shouldBe 150
  }
  // scalastyle:on method.length

  def checkPaging(paging: Paging): Assertion = {
    paging.getPageCount shouldBe 3
    val page = paging.getPage
    page.getNum shouldBe 1
    page.getSize shouldBe halfPageSize
  }

  val detailedMetric1 =
    GetOfferProductActivationsDailyStatsResponse.OfferProductActivationsDailyStats
      .newBuilder()
      .setOfferId("15281737")
      .addAllActivationStats(
        Seq(
          DealerProductActivationsDailyStat
            .newBuilder()
            .setProduct("placement")
            .setDate(odt5.toLocalDate.toString)
            .setSpentKopecks(1000)
            .setCount(1)
            .build(),
          DealerProductActivationsDailyStat
            .newBuilder()
            .setProduct("premium")
            .setDate(odt5.toLocalDate.toString)
            .setSpentKopecks(50000)
            .setCount(1)
            .build(),
          DealerProductActivationsDailyStat
            .newBuilder()
            .setProduct("special-offer")
            .setDate(odt5.toLocalDate.toString)
            .setSpentKopecks(7500)
            .setCount(1)
            .build()
        ).asJava
      )
      .build()

  val detailedMetric2 =
    GetOfferProductActivationsDailyStatsResponse.OfferProductActivationsDailyStats
      .newBuilder()
      .setOfferId("15281739")
      .addAllActivationStats(
        Seq(
          DealerProductActivationsDailyStat
            .newBuilder()
            .setProduct("placement")
            .setDate(odt5.toLocalDate.toString)
            .setSpentKopecks(4000)
            .setCount(1)
            .build()
        ).asJava
      )
      .build()

  val placementActivations = GetOfferProductActivationsDailyStatsResponse
    .newBuilder()
    .addAllOfferProductActivationsStats(
      Seq(detailedMetric1, detailedMetric2).asJava
    )
    .build()

  val dailyStats = Seq(
    DealerProductsActivationsDailyStat
      .newBuilder()
      .setDate("2018-06-14")
      .addAllProductStats(
        Seq(
          DealerProductActivationsStat
            .newBuilder()
            .setProduct("placement")
            .setCount(553)
            .setSpentKopecks(5791000)
            .build(),
          DealerProductActivationsStat
            .newBuilder()
            .setProduct("quota:placement:moto")
            .setCount(1)
            .setSpentKopecks(6500)
            .build(),
          DealerProductActivationsStat
            .newBuilder()
            .setProduct("special-offer")
            .setCount(1)
            .setSpentKopecks(15000)
            .build()
        ).asJava
      )
      .setTotal(TotalStat.newBuilder().setCount(553 + 1 + 1).setSpentKopecks(15000 + 6500 + 5791000).build())
      .build(),
    DealerProductsActivationsDailyStat
      .newBuilder()
      .setDate("2018-06-13")
      .addAllProductStats(
        Seq(
          DealerProductActivationsStat
            .newBuilder()
            .setProduct("boost")
            .setCount(181)
            .setSpentKopecks(5430000)
            .build(),
          DealerProductActivationsStat
            .newBuilder()
            .setProduct("placement")
            .setCount(548)
            .setSpentKopecks(5489000)
            .build(),
          DealerProductActivationsStat
            .newBuilder()
            .setProduct("quota:placement:moto")
            .setCount(1)
            .setSpentKopecks(6500)
            .build()
        ).asJava
      )
      .setTotal(TotalStat.newBuilder().setCount(181 + 548 + 1).setSpentKopecks(5430000 + 5489000 + 6500).build())
      .build(),
    DealerProductsActivationsDailyStat
      .newBuilder()
      .setDate("2018-06-12")
      .addAllProductStats(
        Seq(
          DealerProductActivationsStat
            .newBuilder()
            .setProduct("placement")
            .setSpentKopecks(5472000)
            .setCount(510)
            .build()
        ).asJava
      )
      .setTotal(TotalStat.newBuilder().setCount(300).setSpentKopecks(5472000).build())
      .build(),
    DealerProductsActivationsDailyStat
      .newBuilder()
      .setDate("2018-06-07")
      .addAllProductStats(
        Seq(
          DealerProductActivationsStat
            .newBuilder()
            .setProduct("boost")
            .setSpentKopecks(2250000)
            .setCount(75)
            .build(),
          DealerProductActivationsStat
            .newBuilder()
            .setProduct("placement")
            .setSpentKopecks(4308500)
            .setCount(365)
            .build(),
          DealerProductActivationsStat
            .newBuilder()
            .setProduct("premium")
            .setSpentKopecks(150000)
            .setCount(3)
            .build()
        ).asJava
      )
      .setTotal(TotalStat.newBuilder().setCount(75 + 365 + 3).setSpentKopecks(150000 + 4308500 + 2250000).build())
      .build()
  )

  val dailyClientProductActivations = GetProductActivationsStatsResponse
    .newBuilder()
    .addAllDailyStats(dailyStats.asJava)
    .setTotal(
      ProductsActivationsTotalStats
        .newBuilder()
        .setTotal(
          TotalStat
            .newBuilder()
            .setCount(553 + 1 + 1 + 365 + 548 + 1 + 510 + 75 + 181 + 3)
            .setSpentKopecks(150000 + 4308500 + 2250000 + 5472000 + 15000 + 6500 + 5791000 + 5430000 + 5489000 + 6500)
            .build()
        )
        .addAllProductStats(
          Seq(
            DealerProductActivationsStat
              .newBuilder()
              .setProduct("placement")
              .setCount(553 + 548 + 510 + 365)
              .setSpentKopecks(5791000 + 5489000 + 5472000 + 4308500)
              .build(),
            DealerProductActivationsStat
              .newBuilder()
              .setProduct("quota:placement:moto")
              .setCount(1 + 1)
              .setSpentKopecks(6500 + 6500)
              .build(),
            DealerProductActivationsStat
              .newBuilder()
              .setProduct("special-offer")
              .setCount(1)
              .setSpentKopecks(15000)
              .build(),
            DealerProductActivationsStat
              .newBuilder()
              .setProduct("boost")
              .setSpentKopecks(2250000 + 5430000)
              .setCount(75 + 181)
              .build(),
            DealerProductActivationsStat
              .newBuilder()
              .setProduct("premium")
              .setSpentKopecks(150000)
              .setCount(3)
              .build()
          ).asJava
        )
        .build()
    )
    .build()

  // scalastyle:off method.length
  def checkDailyStats(stats: Vector[ProductsActivationsDailyStat]): Assertion = {
    stats(0).getDate shouldBe "2018-06-14"
    stats(0).getTotal.getCount shouldBe 555
    stats(0).getTotal.getSum shouldBe 58125
    val List(stat0, stat1, stat2) = stats(0).getProductStatsList.asScala.toList: @unchecked
    stat0.getProduct shouldBe "placement"
    stat0.getCount shouldBe 553
    stat0.getSum shouldBe 57910
    stat1.getProduct shouldBe "quota:placement:moto"
    stat1.getCount shouldBe 1
    stat1.getSum shouldBe 65
    stat2.getProduct shouldBe "special-offer"
    stat2.getCount shouldBe 1
    stat2.getSum shouldBe 150
    stats(1).getDate shouldBe "2018-06-13"
    stats(1).getTotal.getCount shouldBe 730
    stats(1).getTotal.getSum shouldBe 109255
    val List(stat3, stat4, stat5) = stats(1).getProductStatsList.asScala.toList: @unchecked
    stat3.getProduct shouldBe "boost"
    stat3.getCount shouldBe 181
    stat3.getSum shouldBe 54300
    stat4.getProduct shouldBe "placement"
    stat4.getCount shouldBe 548
    stat4.getSum shouldBe 54890
    stat5.getProduct shouldBe "quota:placement:moto"
    stat5.getCount shouldBe 1
    stat5.getSum shouldBe 65
    stats(2).getDate shouldBe "2018-06-12"
    stats(2).getTotal.getCount shouldBe 300
    stats(2).getTotal.getSum shouldBe 54720
    val List(stat6) = stats(2).getProductStatsList.asScala.toList: @unchecked
    stat6.getProduct shouldBe "placement"
    stat6.getCount shouldBe 510
    stat6.getSum shouldBe 54720
    stats(3).getDate shouldBe "2018-06-07"
    stats(3).getTotal.getCount shouldBe 443
    stats(3).getTotal.getSum shouldBe 67085
    val List(stat7, stat8, stat9) = stats(3).getProductStatsList.asScala.toList: @unchecked
    stat7.getProduct shouldBe "boost"
    stat7.getCount shouldBe 75
    stat7.getSum shouldBe 22500
    stat8.getProduct shouldBe "placement"
    stat8.getCount shouldBe 365
    stat8.getSum shouldBe 43085
    stat9.getProduct shouldBe "premium"
    stat9.getCount shouldBe 3
    stat9.getSum shouldBe 1500
  }
  // scalastyle:on method.length

  val offerMetrics =
    GetOfferProductActivationsDailyStatsResponse
      .newBuilder()
      .addAllOfferProductActivationsStats(
        Seq(
          GetOfferProductActivationsDailyStatsResponse.OfferProductActivationsDailyStats
            .newBuilder()
            .setOfferId("1071117871-0a288")
            .addAllActivationStats(
              Seq(
                DealerProductActivationsDailyStat
                  .newBuilder()
                  .setProduct("boost")
                  .setDate("2018-06-07")
                  .setSpentKopecks(15000)
                  .setCount(1)
                  .build(),
                DealerProductActivationsDailyStat
                  .newBuilder()
                  .setProduct("placement")
                  .setDate("2018-06-07")
                  .setSpentKopecks(17000)
                  .setCount(2)
                  .build()
              ).asJava
            )
            .build(),
          GetOfferProductActivationsDailyStatsResponse.OfferProductActivationsDailyStats
            .newBuilder()
            .setOfferId("1073188176-2b601371")
            .addAllActivationStats(
              Seq(
                DealerProductActivationsDailyStat
                  .newBuilder()
                  .setProduct("placement")
                  .setDate("2018-06-07")
                  .setSpentKopecks(26000)
                  .setCount(2)
                  .build()
              ).asJava
            )
            .build()
        ).asJava
      )
      .build()

}
