package ru.yandex.realty.callcenter.compute

import com.google.protobuf.timestamp.Timestamp
import org.apache.commons.codec.binary.Base64
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.palma.concierge_request.{ConciergeRequest, ConciergeSiteRequest}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.callcenter.dao.ConciergeRequestRecord
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.core.{Name, Node}
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.sites.SitesGroupingService

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}

@RunWith(classOf[JUnitRunner])
class ConciergeCsvExporterSpec extends AsyncSpecBase {
  val regionGraph: RegionGraph = mock[RegionGraph]
  val regionGraphProvider: Provider[RegionGraph] = () => regionGraph
  val sitesGroupingService: SitesGroupingService = mock[SitesGroupingService]

  trait CsvExporterFixture {
    val csvExporter = new ConciergeCsvExporter(regionGraphProvider, sitesGroupingService)
  }

  "ConciergeCsvExporter" should {
    "check csv one common request" in new CsvExporterFixture {
      val rgid_1 = 1
      val name: Name = new Name
      name.setDisplay("address")
      val node: Node = new Node
      node.setName(name)

      (regionGraph
        .getNodeById(_: java.lang.Long))
        .expects(Long.box(rgid_1))
        .returning(node)

      val requests: Seq[ConciergeRequest] =
        Seq(
          ConciergeRequest(
            id = "1",
            createDate = Option(Timestamp(1, 2)),
            uuid = "2",
            url = "url",
            rgid = rgid_1,
            phone = "+79999999999",
            comment = "common",
            sent = realty.palma.concierge_request.SentStatus.YES,
            userName = "Name",
            callIntervalFrom = 1,
            callIntervalTo = 10000
          )
        )
      Base64
        .decodeBase64(csvExporter.prepareCsv(requests))
        .map(_.toChar)
        .mkString
        .shouldBe(
          "\"id\";\"time\";\"region\";\"url\";\"comment\";\"phone\";\"userName\";\"callInterval\"\n" +
            "\"1\";\"1970-01-01 03:00:01 MSK\";\"address\";\"url\";\"common\";\"+79999999999\";\"Name\";\"1970-01-01 03:00:01 MSK - 1970-01-01 05:46:40 MSK\"\n"
        )
    }

    "check csv many common requests" in new CsvExporterFixture {
      val rgid_1 = 1
      val rgid_2 = 2

      val name_1: Name = new Name
      name_1.setDisplay("address")
      val node_1: Node = new Node
      node_1.setName(name_1)

      val name_2: Name = new Name
      name_2.setDisplay("address2")
      val node_2: Node = new Node
      node_2.setName(name_2)

      (regionGraph
        .getNodeById(_: java.lang.Long))
        .expects(Long.box(rgid_1))
        .returning(node_1)

      (regionGraph
        .getNodeById(_: java.lang.Long))
        .expects(Long.box(rgid_2))
        .returning(node_2)

      val requests: Seq[ConciergeRequest] =
        Seq(
          ConciergeRequest(
            id = "1",
            createDate = Option(Timestamp(1, 2)),
            uuid = "2",
            url = "url",
            rgid = rgid_1,
            phone = "+79999999999",
            comment = "common",
            sent = realty.palma.concierge_request.SentStatus.YES,
            userName = "Name",
            callIntervalFrom = 1,
            callIntervalTo = 10000
          ),
          ConciergeRequest(
            id = "1",
            createDate = Option(Timestamp(1, 2)),
            uuid = "2",
            url = "url",
            rgid = rgid_2,
            phone = "+79999999995",
            comment = "common2",
            sent = realty.palma.concierge_request.SentStatus.YES,
            userName = "Name2",
            callIntervalFrom = 1,
            callIntervalTo = 10000
          )
        )
      Base64
        .decodeBase64(csvExporter.prepareCsv(requests))
        .map(_.toChar)
        .mkString
        .shouldBe(
          "\"id\";\"time\";\"region\";\"url\";\"comment\";\"phone\";\"userName\";\"callInterval\"\n" +
            "\"1\";\"1970-01-01 03:00:01 MSK\";\"address\";\"url\";\"common\";\"+79999999999\";\"Name\";\"1970-01-01 03:00:01 MSK - 1970-01-01 05:46:40 MSK\"\n" +
            "\"1\";\"1970-01-01 03:00:01 MSK\";\"address2\";\"url\";\"common2\";\"+79999999995\";\"Name2\";\"1970-01-01 03:00:01 MSK - 1970-01-01 05:46:40 MSK\"\n"
        )
    }

    "check csv one site request" in new CsvExporterFixture {
      val location_1: Location = new Location
      location_1.setCombinedAddress("address")
      val siteId_1: Long = 1
      val site_1: Site = new Site(siteId_1)
      site_1.setName("site")
      site_1.setLocation(location_1)

      (sitesGroupingService
        .getSiteById(_: Long))
        .expects(siteId_1)
        .returning(site_1)
        .twice()

      val requests: Seq[ConciergeSiteRequest] =
        Seq(
          ConciergeSiteRequest(
            id = "1",
            createDate = Option(Timestamp(1, 2)),
            url = "url",
            phone = "+79999999999",
            siteId = siteId_1.toString,
            sent = realty.palma.concierge_request.SentStatus.YES,
            userName = "Name",
            callIntervalFrom = 1,
            callIntervalTo = 10000
          )
        )
      Base64
        .decodeBase64(csvExporter.prepareSiteCsv(requests))
        .map(_.toChar)
        .mkString
        .shouldBe(
          "\"id\";\"time\";\"url\";\"userName\";\"phone\";\"siteName\";\"siteAddress\";\"callInterval\"\n" +
            "\"1\";\"1970-01-01 03:00:01 MSK\";\"url\";\"Name\";\"+79999999999\";\"site\";\"address\";\"1970-01-01 03:00:01 MSK - 1970-01-01 05:46:40 MSK\"\n"
        )
    }

    "check csv many sites requests" in new CsvExporterFixture {
      val location_1: Location = new Location
      location_1.setCombinedAddress("address")
      val siteId_1: Long = 1
      val site_1: Site = new Site(siteId_1)
      site_1.setName("site")
      site_1.setLocation(location_1)

      val location_2: Location = new Location
      location_2.setCombinedAddress("address2")
      val siteId_2: Long = 2
      val site_2: Site = new Site(siteId_2)
      site_2.setName("site2")
      site_2.setLocation(location_2)

      (sitesGroupingService
        .getSiteById(_: Long))
        .expects(siteId_1)
        .returning(site_1)
        .twice()

      (sitesGroupingService
        .getSiteById(_: Long))
        .expects(siteId_2)
        .returning(site_2)
        .twice()

      val requests: Seq[ConciergeSiteRequest] =
        Seq(
          ConciergeSiteRequest(
            id = "1",
            createDate = Option(Timestamp(1, 2)),
            url = "url",
            phone = "+79999999999",
            siteId = siteId_1.toString,
            sent = realty.palma.concierge_request.SentStatus.YES,
            userName = "Name",
            callIntervalFrom = 1,
            callIntervalTo = 10000
          ),
          ConciergeSiteRequest(
            id = "2",
            createDate = Option(Timestamp(1, 2)),
            url = "url",
            phone = "+79999999999",
            siteId = siteId_2.toString,
            sent = realty.palma.concierge_request.SentStatus.YES,
            userName = "Name2",
            callIntervalFrom = 1,
            callIntervalTo = 10000
          )
        )
      Base64
        .decodeBase64(csvExporter.prepareSiteCsv(requests))
        .map(_.toChar)
        .mkString
        .shouldBe(
          "\"id\";\"time\";\"url\";\"userName\";\"phone\";\"siteName\";\"siteAddress\";\"callInterval\"\n" +
            "\"1\";\"1970-01-01 03:00:01 MSK\";\"url\";\"Name\";\"+79999999999\";\"site\";\"address\";\"1970-01-01 03:00:01 MSK - 1970-01-01 05:46:40 MSK\"\n" +
            "\"2\";\"1970-01-01 03:00:01 MSK\";\"url\";\"Name2\";\"+79999999999\";\"site2\";\"address2\";\"1970-01-01 03:00:01 MSK - 1970-01-01 05:46:40 MSK\"\n"
        )
    }

    "check csv one call argee record" in new CsvExporterFixture {
      val rgid_1 = 1
      val name: Name = new Name
      name.setDisplay("address")
      val node: Node = new Node
      node.setName(name)

      (regionGraph
        .getNodeById(_: java.lang.Long))
        .expects(Long.box(rgid_1))
        .returning(node)

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
        .toInstant();

      val records: Seq[ConciergeRequestRecord] =
        Seq(
          ConciergeRequestRecord(
            id = "1",
            createDate = createDate,
            payloadType = "",
            uuid = Some("2"),
            userName = Some("Name"),
            refererUrl = Some("url"),
            rgid = rgid_1,
            phone = "+79999999999",
            comment = Some("comment"),
            status = "",
            callIntervalFrom = Some(createDate),
            callIntervalTo = Some(createDate.plus(12, ChronoUnit.HOURS))
          )
        )
      Base64
        .decodeBase64(csvExporter.prepareCallAgreeCsv(records))
        .map(_.toChar)
        .mkString
        .shouldBe(
          "\"id\";\"time\";\"region\";\"url\";\"userName\";\"phone\";\"comment\";\"callInterval\"\n" +
            "\"1\";\"2022-07-20 00:00:00 MSK\";\"address\";\"url\";\"Name\";\"+79999999999\";\"comment\";\"2022-07-20 00:00:00 MSK - 2022-07-20 12:00:00 MSK\"\n"
        )
    }

    "check csv many call argee records" in new CsvExporterFixture {
      val rgid_1 = 1
      val name: Name = new Name
      name.setDisplay("address")
      val node: Node = new Node
      node.setName(name)

      (regionGraph
        .getNodeById(_: java.lang.Long))
        .expects(Long.box(rgid_1))
        .returning(node)

      (regionGraph
        .getNodeById(_: java.lang.Long))
        .expects(Long.box(2L))
        .returning(null)

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

      val records: Seq[ConciergeRequestRecord] =
        Seq(
          ConciergeRequestRecord(
            id = "1",
            createDate = createDate,
            payloadType = "",
            uuid = Some("2"),
            userName = Some("Name"),
            refererUrl = Some("url"),
            rgid = rgid_1,
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
      Base64
        .decodeBase64(csvExporter.prepareCallAgreeCsv(records))
        .map(_.toChar)
        .mkString
        .shouldBe(
          "\"id\";\"time\";\"region\";\"url\";\"userName\";\"phone\";\"comment\";\"callInterval\"\n" +
            "\"1\";\"2022-07-20 00:00:00 MSK\";\"address\";\"url\";\"Name\";\"+79999999999\";\"comment\";\"2022-07-20 00:00:00 MSK - 2022-07-20 12:00:00 MSK\"\n" +
            "\"2\";\"2022-07-21 00:00:00 MSK\";\"unknown\";\"url2\";\"Name2\";\"+79999999998\";\"comment2\";\"2022-07-21 00:00:00 MSK - 2022-07-21 13:00:00 MSK\"\n"
        )
    }
  }
}
