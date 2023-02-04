package ru.yandex.vertis.statistics.api.managers

import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.statistics.api.Request.AggregateBy.Granularity
import realty.statistics.api.Request.{AggregateBy, AggregatedStatisticsRequest, StatisticComponent}
import realty.statistics.api.Response.AggregatedStatisticsGranularEntry
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.statist.StatistClient
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.statist.model.api.ApiModel.{
  FieldFilters,
  MultipleDailyValues,
  ObjectDailyValues,
  ObjectDayValues
}
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class DefaultShowsManagerSpec extends AsyncSpecBase {

  private val statistClient = mock[StatistClient]
  private val showsManager = new DefaultShowsManager(statistClient)
  private val partnerIds = List(
    "1069314898",
    "1069314904",
    "1069315072"
  ).asJava

  "DefaulShowsManager" should {
    "correctly transform partner show statistics by day" in {
      val request = AggregatedStatisticsRequest
        .newBuilder()
        .addAllPartnerIds(partnerIds)
        .setFrom("2021-03-25")
        .setTo("2021-03-31")
        .addComponents(StatisticComponent.OFFER_SHOW)
        .addComponents(StatisticComponent.CARD_SHOW)
        .addComponents(StatisticComponent.PHONE_SHOW)
        .setAggregateBy(
          AggregateBy
            .newBuilder()
            .setGranularity(Granularity.DAY)
            .setPeriod(1)
        )
        .build()

      val expectedResponse = MultipleDailyValues
        .newBuilder()
        .putObjects(
          "1069314898",
          ObjectDailyValues
            .newBuilder()
            .addDays(buildDayCounters("2021-03-25", 25, 5, 1))
            .addDays(buildDayCounters("2021-03-26", 26, 6, 2))
            .addDays(buildDayCounters("2021-03-27", 27, 7, 3))
            .addDays(buildDayCounters("2021-03-28", 28, 8, 4))
            .addDays(buildDayCounters("2021-03-29", 29, 9, 5))
            .addDays(buildDayCounters("2021-03-30", 30, 0, 0))
            .addDays(buildDayCounters("2021-03-31", 31, 1, 0))
            .build()
        )
        .putObjects(
          "1069314904",
          ObjectDailyValues
            .newBuilder()
            .addDays(buildDayCounters("2021-03-25", 35, 15, 3))
            .addDays(buildDayCounters("2021-03-26", 36, 16, 4))
            .addDays(buildDayCounters("2021-03-27", 37, 17, 5))
            .addDays(buildDayCounters("2021-03-28", 38, 18, 6))
            .addDays(buildDayCounters("2021-03-29", 39, 19, 7))
            .addDays(buildDayCounters("2021-03-30", 40, 10, 1))
            .addDays(buildDayCounters("2021-03-31", 41, 11, 2))
            .build()
        )
        .putObjects(
          "1069315072",
          ObjectDailyValues
            .newBuilder()
            .addDays(buildDayCounters("2021-03-25", 45, 25, 13))
            .addDays(buildDayCounters("2021-03-26", 46, 26, 14))
            .addDays(buildDayCounters("2021-03-27", 47, 27, 15))
            .addDays(buildDayCounters("2021-03-28", 48, 28, 16))
            .addDays(buildDayCounters("2021-03-29", 49, 29, 17))
            .addDays(buildDayCounters("2021-03-30", 50, 20, 11))
            .addDays(buildDayCounters("2021-03-31", 51, 21, 12))
            .build()
        )
        .build()

      val trace = Traced.empty
      (statistClient
        .getMultipleByDay(
          _: String,
          _: Seq[String],
          _: Seq[String],
          _: LocalDate,
          _: LocalDate,
          _: FieldFilters
        )(_: Traced))
        .expects(
          "realty_partner_type_category",
          ArrayBuffer("offer_show", "card_show", "phone_show"),
          partnerIds.asScala,
          new LocalDate("2021-03-25"),
          new LocalDate("2021-04-01"),
          FieldFilters.getDefaultInstance,
          trace
        )
        .returning(Future.successful(expectedResponse))

      val response = showsManager.getAggregatedStatistic(request)(trace).futureValue
      response.getDetailsCount shouldBe (7)
      val details = response.getDetailsList.asScala
      getCounterByDate(details, "2021-03-25", StatisticComponent.OFFER_SHOW) shouldBe (105)
      getCounterByDate(details, "2021-03-25", StatisticComponent.CARD_SHOW) shouldBe (45)
      getCounterByDate(details, "2021-03-25", StatisticComponent.PHONE_SHOW) shouldBe (17)
    }
  }

  private def getCounterByDate(
    details: Seq[AggregatedStatisticsGranularEntry],
    date: String,
    component: StatisticComponent
  ): Long = {
    val ts = DateTimeFormat.write(ISODateTimeFormat.dateTimeParser().parseDateTime(date))
    details
      .find(_.getTimestamp == ts)
      .get
      .getEntriesList
      .asScala
      .find(_.getComponent == component)
      .get
      .getValue
  }

  private def buildDayCounters(date: String, offerShow: Int, cardShow: Int, phoneShow: Int): ObjectDayValues =
    ObjectDayValues
      .newBuilder()
      .setDay(date)
      .putComponents("offer_show", offerShow)
      .putComponents("card_show", cardShow)
      .putComponents("phone_show", phoneShow)
      .build()
}
