package ru.yandex.realty.cadastr.dao

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.cadastr.gen.CadastrModelsGen
import ru.yandex.realty.cadastr.model.{Excerpt, Report, Request}
import ru.yandex.realty.cadastr.model.enums.ReportType

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ReportDaoSpec extends WordSpecLike with CadastrSpecBase with CadastrModelsGen with CleanSchemaBeforeEach {

  "ReportDao" should {
    "delete obsolete reports" in {
      val paidReport = reportGen(reportId = Some("1")).next
      val report = reportGen(reportId = Some("2"), isPaid = false).next
      val reportWithoutData =
        reportGen(reportId = Some("3"), cadastrNumber = Some("number1"), isPaid = false, hasData = false).next
      val reportWithData = reportGen(reportId = Some("4"), cadastrNumber = Some("number2"), isPaid = false).next
      val report3 = reportGen(
        reportId = Some("5"),
        cadastrNumber = Some("number3"),
        isPaid = false,
        reportType = Some(ReportType.RentFlat)
      ).next
      val paidReport3 =
        reportGen(
          reportId = Some("6"),
          cadastrNumber = Some("number3"),
          reportType = Some(ReportType.RentFlat)
        ).next
      val obsoleteReport3 =
        reportGen(
          reportId = Some("7"),
          cadastrNumber = Some("number3"),
          isPaid = false,
          reportType = Some(ReportType.RentFlat),
          created = DateTime.now().minusDays(1)
        ).next
      val report4 = reportGen(
        reportId = Some("8"),
        cadastrNumber = Some("number4"),
        isPaid = false,
        reportType = Some(ReportType.RentFlat)
      ).next
      val secondObsoleteReport4 =
        reportGen(
          reportId = Some("9"),
          cadastrNumber = Some("number4"),
          isPaid = false,
          reportType = Some(ReportType.RentFlat),
          created = DateTime.now().minusDays(1)
        ).next
      val firstObsoleteReport4 =
        reportGen(
          reportId = Some("10"),
          cadastrNumber = Some("number4"),
          isPaid = false,
          reportType = Some(ReportType.RentFlat),
          created = DateTime.now().minusDays(2)
        ).next
      val paidReport4 = reportGen(reportId = Some("11"), cadastrNumber = Some("number4")).next
      val flatReport =
        reportGen(
          reportId = Some("12"),
          cadastrNumber = Some("number5"),
          isPaid = false,
          reportType = Some(ReportType.Flat)
        ).next
      val paidFlatReport =
        reportGen(
          reportId = Some("13"),
          cadastrNumber = Some("number5"),
          reportType = Some(ReportType.Flat)
        ).next
      val siteReport =
        reportGen(
          reportId = Some("14"),
          cadastrNumber = Some("number5"),
          isPaid = false,
          reportType = Some(ReportType.Site)
        ).next
      val paidSiteReport =
        reportGen(
          reportId = Some("15"),
          cadastrNumber = Some("number5"),
          reportType = Some(ReportType.Flat)
        ).next
      val flatReport6 =
        reportGen(
          reportId = Some("16"),
          cadastrNumber = Some("number6"),
          isPaid = false,
          reportType = Some(ReportType.Flat)
        ).next
      val obsoleteFlatReport6 = reportGen(
        reportId = Some("17"),
        cadastrNumber = Some("number6"),
        isPaid = false,
        reportType = Some(ReportType.Flat),
        created = DateTime.now().minusDays(1)
      ).next
      val siteReport6 =
        reportGen(
          reportId = Some("18"),
          cadastrNumber = Some("number6"),
          isPaid = false,
          reportType = Some(ReportType.Site)
        ).next
      val obsoleteSiteReport6 = reportGen(
        reportId = Some("19"),
        cadastrNumber = Some("number6"),
        isPaid = false,
        reportType = Some(ReportType.Site),
        created = DateTime.now().minusDays(1)
      ).next

      val reports = Seq(
        paidReport,
        report,
        reportWithoutData,
        reportWithData,
        report3,
        paidReport3,
        obsoleteReport3,
        report4,
        firstObsoleteReport4,
        secondObsoleteReport4,
        paidReport4,
        flatReport,
        paidFlatReport,
        siteReport,
        paidSiteReport,
        flatReport6,
        obsoleteFlatReport6,
        siteReport6,
        obsoleteSiteReport6
      )

      val report2requestsSeq = for {
        report <- reports
        requests = genReportRequests(report)
      } yield report -> requests
      val report2requests = report2requestsSeq.toMap

      val report2excerptsSeq = for {
        report <- reports
        excerpts = genReportExcerpts(report)
      } yield report -> excerpts
      val report2excerpts = report2excerptsSeq.toMap

      val insertsF = for {
        report <- reports
        excerpts = report2excerpts(report)
        requests = report2requests(report)
      } yield reportDao.create(report, requests, excerpts)

      val deletedReports = for {
        _ <- Future.sequence(insertsF)
        deletedReports <- reportDao.deleteObsoleteReports(100)
      } yield deletedReports

      val expected =
        Set(obsoleteReport3, firstObsoleteReport4, secondObsoleteReport4, obsoleteFlatReport6, obsoleteSiteReport6).map(
          _.reportId
        )
      val notExpected = reports.filter(report => !expected.contains(report.reportId)).toSet

      deletedReports.futureValue.toSet shouldBe expected
      Future.sequence(expected.map(reportDao.getByIdOpt)).futureValue shouldBe Set(None)
      Future.sequence(notExpected.map(_.reportId).map(reportDao.getById)).futureValue.size shouldBe notExpected.size
      Future.sequence(expected.map(excerptDao.get)).futureValue.flatten.size shouldBe 0
      Future.sequence(notExpected.map(_.reportId).map(excerptDao.get)).futureValue.flatten shouldBe notExpected
        .flatMap(report2excerpts)
      Future.sequence(expected.map(requestDao.getByReportId)).futureValue.flatten.size shouldBe 0
      Future
        .sequence(notExpected.map(_.reportId).map(requestDao.getByReportId))
        .futureValue
        .flatten shouldBe notExpected
        .flatMap(report2requests)
    }
  }

  private def genReportExcerpts(report: Report): Seq[Excerpt] = {
    val excerptsCount = Gen.choose(0, 3).next
    Range.inclusive(1, excerptsCount).flatMap(excerptGen(report.id).next)
  }

  private def genReportRequests(report: Report): Seq[Request] = {
    val excerptsCount = Gen.choose(0, 3).next
    Range.inclusive(1, excerptsCount).flatMap(requestGen(reportId = Some(report.id)).next)
  }
}
