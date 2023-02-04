package ru.yandex.realty.callcenter.processors

import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.callcenter.compute.ConciergeCsvExporter
import ru.yandex.realty.callcenter.dao.{ConciergeRequestDao, ConciergeRequestRecord}
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.http.HttpClientMock
import ru.yandex.realty.sender.{DefaultSenderClient, SenderClient}
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.realty.tracing.Traced

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CallAgreeProcessorSpec extends AsyncSpecBase with RegionGraphTestComponents with HttpClientMock {

  private val TemplateId = "consent_to_call"

  "CallAgreeProcessor" should {

    "process records" in new CallAgreeProcessorFixture {
      val records: Seq[ConciergeRequestRecord] = prepareRecords()
      val ids: Set[String] = records.map(_.id).toSet

      (dao
        .selectLastCallAgree(_: Instant)(_: Traced))
        .expects(*, *)
        .once()
        .returns(Future.successful(records))

      (dao
        .updateSentFlags(_: Set[String])(_: Traced))
        .expects(ids, *)
        .once()
        .returns(Future.successful(true))

      httpClient.expect(POST, s"/api/0/realty/transactional/$TemplateId/send")
      httpClient.respondWith(StatusCodes.OK, "{\"result\": {\"status\": \"ok\"}}")

      processor.process.futureValue
    }

    "process records if sender failed" in new CallAgreeProcessorFixture {
      val records: Seq[ConciergeRequestRecord] = prepareRecords()
      val ids: Set[String] = records.map(_.id).toSet

      (dao
        .selectLastCallAgree(_: Instant)(_: Traced))
        .expects(*, *)
        .once()
        .returns(Future.successful(records))

      (dao
        .updateSentFlags(_: Set[String])(_: Traced))
        .expects(ids, *)
        .never()
        .returns(Future.successful(true))

      httpClient.expect(POST, s"/api/0/realty/transactional/$TemplateId/send")
      httpClient.respondWith(StatusCodes.OK, "{\"result\": {\"status\": \"fail\", \"error\":\"could not send email\"}}")

      processor.process.futureValue
    }

    "process records if no new records found" in {
      val sender = mock[SenderClient]
      val dao: ConciergeRequestDao = mock[ConciergeRequestDao]
      val exporter: ConciergeCsvExporter = mock[ConciergeCsvExporter]
      val targets: Seq[String] = Seq("a@yandexmail.y", "b@yandexmail.y")

      val processor = new CallAgreeProcessor(sender, dao, exporter, targets)

      (dao
        .selectLastCallAgree(_: Instant)(_: Traced))
        .expects(*, *)
        .once()
        .returns(Future.successful(Seq.empty))

      processor.process.futureValue
    }
  }

  trait CallAgreeProcessorFixture {
    val sender = new DefaultSenderClient(httpService, "authUserName")
    val dao: ConciergeRequestDao = mock[ConciergeRequestDao]
    val sitesGroupingService: SitesGroupingService = mock[SitesGroupingService]
    val exporter: ConciergeCsvExporter = new ConciergeCsvExporter(regionGraphProvider, sitesGroupingService)
    val targets: Seq[String] = Seq("a@yandexmail.y", "b@yandexmail.y")

    val processor = new CallAgreeProcessor(sender, dao, exporter, targets)
  }

  private def prepareRecords(): Seq[ConciergeRequestRecord] = {
    val createDate: Instant = Instant
      .now()
      .atZone(ZoneId.of("Europe/Moscow"))
      .withYear(2022)
      .withMonth(7)
      .withDayOfMonth(20)
      .withHour(0)
      .withMinute(0)
      .withSecond(0)
      .withNano(0)
      .toInstant

    val createDate2: Instant = createDate
      .atZone(ZoneId.of("Europe/Moscow"))
      .withDayOfMonth(21)
      .toInstant

    Seq(
      ConciergeRequestRecord(
        id = "1",
        createDate = createDate,
        payloadType = "",
        uuid = Some("2"),
        userName = Some("Name"),
        refererUrl = Some("url"),
        rgid = 1L,
        phone = "+79999999999",
        comment = Some("comment"),
        status = "",
        callIntervalFrom = Some(createDate),
        callIntervalTo = Some(createDate.plus(12, ChronoUnit.HOURS))
      ),
      ConciergeRequestRecord(
        id = "2",
        createDate = createDate2,
        payloadType = "",
        uuid = Some("2"),
        userName = Some("Name2"),
        refererUrl = Some("url2"),
        rgid = 2,
        phone = "+79999999998",
        comment = Some("comment2"),
        status = "",
        callIntervalFrom = Some(createDate2),
        callIntervalTo = Some(createDate2.plus(13, ChronoUnit.HOURS))
      )
    )
  }
}
