package ru.yandex.realty.managers.feed

import akka.http.scaladsl.model.StatusCodes
import org.joda.time.format.DateTimeFormat
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.statistics.api.Response
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.call.CallSearchRequest
import ru.yandex.realty.clients.capa.{CapaClient, CapaClientException, Partner, Status, StatusInfo}
import ru.yandex.realty.clients.statistics._
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.errors.CommonError
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.managers.lk.stats.CallStatisticsAdapter
import ru.yandex.realty.model.duration.TimeRange
import ru.yandex.realty.model.user.UserRefGenerators
import ru.yandex.realty.stat.AggregationStatLevel
import ru.yandex.realty.util.protobuf.TimeProtoFormats.{DateTimeFormat => ProtoDateTimeFormat, TimeRangeProtoFormat}
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class DefaultPartnerFeedManagerSpec extends AsyncSpecBase with RequestAware with UserRefGenerators {

  private val capaClient = mock[CapaClient]
  private val vosClientNG = mock[VosClientNG]
  private val rawStatisticsClient = mock[RawStatisticsClient]
  private val features = new SimpleFeatures
  private val callStatisticsAdapter = new CallStatisticsAdapter(rawStatisticsClient)

  private val partnerManager =
    new DefaultPartnerFeedManager(capaClient, vosClientNG, callStatisticsAdapter, rawStatisticsClient, features)

  private val сapaFailure = new CapaClientException("NOT_FOUND", "Some was not found", StatusCodes.NotFound)
  private val сommonFailure = CommonError("ERROR_CODE", "Error message")

  private def mockGetPartners = toMockFunction2(capaClient.getPartners(_: String)(_: Traced))

  private def mockAggregatedCalls = toMockFunction4(
    rawStatisticsClient.aggregatedFlatCalls(_: CallSearchRequest, _: AggregationStatLevel, _: Seq[AggregationCallType])(
      _: Traced
    )
  )

  "DefaultPartnerFeedManager " should {
    "getCommonCallStats returns statistics by calls" in {
      val uid = "123456"
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val baseDate = formatter.parseDateTime("2018-01-10")
      val timeRange = TimeRange(Some(baseDate.minusDays(2)), Some(baseDate))
      val partnerFromCapa = Partner(123, Map.empty, StatusInfo(Status(1, "1"), None, 123, uid.toLong), None, 123456)
      import AggregationCallType._
      val aggEntry1 = Response.AggregationEntry
        .newBuilder()
        .setTimestamp(ProtoDateTimeFormat.write(baseDate.minusDays(2)))
        .addValues(Response.AggregatedValue.newBuilder().setName(Total.name).setValue(1L))
        .addValues(Response.AggregatedValue.newBuilder().setName(Missed.name).setValue(1L))
      val aggEntry2 = Response.AggregationEntry
        .newBuilder()
        .setTimestamp(ProtoDateTimeFormat.write(baseDate.minusDays(1)))
        .addValues(Response.AggregatedValue.newBuilder().setName(Total.name).setValue(3L))
        .addValues(Response.AggregatedValue.newBuilder().setName(Success.name).setValue(1L))
        .addValues(Response.AggregatedValue.newBuilder().setName(Blocked.name).setValue(1L))
        .addValues(Response.AggregatedValue.newBuilder().setName(Target.name).setValue(1L))

      mockGetPartners
        .expects(uid, *)
        .returningF(List(partnerFromCapa))

      mockAggregatedCalls
        .expects(
          *,
          AggregationStatLevel.Day,
          AggregationCallType.allTypes(),
          *
        )
        .returningF(
          Response.AggregationResult
            .newBuilder()
            .addEntries(aggEntry1)
            .addEntries(aggEntry2)
            .build()
        )

      val user = passportUserGen.next

      withRequestContext(user) { implicit r =>
        val result = partnerManager.getCommonCallStats(uid, AggregationStatLevel.Day, timeRange).futureValue

        result.getStats.getTotal.getTotal should be(4)
        result.getStats.getTotal.getBlocked should be(1)
        result.getStats.getTotal.getMissed should be(1)
        result.getStats.getTotal.getSuccess should be(1)
        result.getStats.getTotal.getTarget should be(1)
        result.getStats.getTotal.getNonTarget should be(0)
        result.getStats.getEntriesCount should be(2)
      }
    }

    "getCommonCallStats returns statistics by calls if raw stat return empty list" in {
      val uid = "123456"
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val baseDate = formatter.parseDateTime("2018-01-10")
      val timeRange = TimeRange(Some(baseDate.minusDays(2)), Some(baseDate))
      val partnerFromCapa = Partner(123, Map.empty, StatusInfo(Status(1, "1"), None, 123, uid.toLong), None, 123456)

      mockGetPartners
        .expects(uid, *)
        .returningF(List(partnerFromCapa))

      mockAggregatedCalls
        .expects(
          *,
          AggregationStatLevel.Day,
          AggregationCallType.allTypes(),
          *
        )
        .returningF(Response.AggregationResult.getDefaultInstance)

      val user = passportUserGen.next

      withRequestContext(user) { implicit r =>
        val result = partnerManager.getCommonCallStats(uid, AggregationStatLevel.Day, timeRange).futureValue

        result.getStats.getTotal.getTotal should be(0)
        result.getStats.getTotal.getBlocked should be(0)
        result.getStats.getTotal.getMissed should be(0)
        result.getStats.getTotal.getSuccess should be(0)
        result.getStats.getTotal.getTarget should be(0)
        result.getStats.getTotal.getNonTarget should be(0)
        result.getStats.getEntriesCount should be(0)
      }
    }

    "getCommonCallStats returns statistics by calls for level All and date to is None" in {
      val uid = "123456"
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val baseDate = formatter.parseDateTime("2018-01-10")
      val timeRange = TimeRange(Some(baseDate.minusDays(2)), None)
      val partnerFromCapa = Partner(123, Map.empty, StatusInfo(Status(1, "1"), None, 123, uid.toLong), None, 123456)
      import AggregationCallType._
      val aggEntry1 = Response.AggregationEntry
        .newBuilder()
        .setTimestamp(ProtoDateTimeFormat.write(baseDate.minusDays(2)))
        .addValues(Response.AggregatedValue.newBuilder().setName(Total.name).setValue(1L))
        .addValues(Response.AggregatedValue.newBuilder().setName(Missed.name).setValue(1L))

      mockGetPartners
        .expects(uid, *)
        .returningF(List(partnerFromCapa))

      mockAggregatedCalls
        .expects(
          *,
          AggregationStatLevel.All,
          AggregationCallType.allTypes(),
          *
        )
        .returningF(
          Response.AggregationResult
            .newBuilder()
            .addEntries(aggEntry1)
            .build()
        )

      val user = passportUserGen.next

      withRequestContext(user) { implicit r =>
        val result = partnerManager.getCommonCallStats(uid, AggregationStatLevel.All, timeRange).futureValue

        result.getStats.getTotal.getTotal should be(1)
        result.getStats.getTotal.getBlocked should be(0)
        result.getStats.getTotal.getMissed should be(1)
        result.getStats.getTotal.getSuccess should be(0)
        result.getStats.getTotal.getTarget should be(0)
        result.getStats.getTotal.getNonTarget should be(0)
        result.getStats.getEntriesCount should be(1)

        result.getStats.getEntries(0).getInterval should be
        TimeRangeProtoFormat.write(timeRange)
      }
    }

    "getCommonCallStats return error if rawStatistics return error" in {
      val uid = "123456"

      mockGetPartners
        .expects(uid, *)
        .returningF(List.empty)

      mockAggregatedCalls
        .expects(
          *,
          AggregationStatLevel.Day,
          AggregationCallType.allTypes(),
          *
        )
        .throwingF(сommonFailure)

      interceptCause[CommonError] {
        val user = passportUserGen.next

        withRequestContext(user) { implicit r =>
          partnerManager.getCommonCallStats(uid, AggregationStatLevel.Day, TimeRange.Empty).futureValue
        }
      }
    }

    "getCommonCallStats return error if capa return error for partner Id search" in {
      val uid = "123456"
      mockGetPartners
        .expects(uid, *)
        .throwingF(сapaFailure)

      interceptCause[CapaClientException] {
        val user = passportUserGen.next

        withRequestContext(user) { implicit r =>
          partnerManager.getCommonCallStats(uid, AggregationStatLevel.Day, TimeRange.Empty).futureValue
        }
      }
    }
  }

}
