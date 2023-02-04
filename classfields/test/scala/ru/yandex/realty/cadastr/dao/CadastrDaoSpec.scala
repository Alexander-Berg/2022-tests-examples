package ru.yandex.realty.cadastr.dao

import com.google.protobuf.Timestamp
import org.joda.time.DateTime
import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.realty.cadastr.gen.CadastrModelsGen
import ru.yandex.realty.cadastr.model.enums.{CadastralNumberStatus, PaidReportPaymentStatus, RequestStatus}
import ru.yandex.realty.cadastr.model.update.ReportUpdate
import ru.yandex.realty.cadastr.model.{Offer, PaidReport, Report}
import ru.yandex.realty.cadastr.proto.model.paidreport.{PaidReport => ProtoPaidReport}
import ru.yandex.realty.cadastr.proto.model.report.FlatExcerptReport.CostInfo
import ru.yandex.realty.cadastr.proto.model.report.{ExcerptReport, FlatExcerptReport}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.util.{Random, Try}

// Commented out to make builds faster, cause this spec takes too long time.
// Also sometimes it fails with "timeout occurred waiting for a future to complete", for example:
// https://t.vertis.yandex-team.ru/buildConfiguration/VerticalsBackend_Realty_CiBuild/800065
// It was decided to uncomment this spec only for manual testing of new DAO methods.
//@RunWith(classOf[JUnitRunner])
class CadastrDaoSpec extends CadastrDaoBase with CadastrModelsGen {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(20, Millis))

  before {
    createTables()
  }

  after {
    dropTables()
  }

  "Cadastr DAOs" should {
    "insert, select, update offers" in {
      val offers = offerGen.next(3).toList
      offerDao.create(offers).futureValue
      offers.foreach { offer =>
        val o = offerDao.get(offer.offerId).futureValue
        assert(o.offerId == offer.offerId)
      }

      val updatedOffers = offers.map { offer =>
        offer.withUpdatedSearch {
          _.copy(cadastralNumberStatus = CadastralNumberStatus.Exists, cadastralNumber = Some("123"))
        }
      }

      updatedOffers.foreach { updatedOffer =>
        offerDao.update(updatedOffer.offerId)(_ => updatedOffer).futureValue
      }

      offers.foreach { offer =>
        val o = offerDao.get(offer.offerId).futureValue
        assert(o.cadastralNumberSearch.cadastralNumberStatus == CadastralNumberStatus.Exists)
        assert(o.cadastralNumberSearch.cadastralNumber.get == "123")
      }
    }

    "insert, select, update requests" in {
      val requests = requestGen().next(3).toList

      requestDao.create(requests).futureValue
      requests.foreach { request =>
        val r = requestDao.get(request.requestId).futureValue
        assert(r.requestId == request.requestId)
      }

      val inProgress = requestDao.getAllByStatus(RequestStatus.InProgress).futureValue
      assert(inProgress.length == requests.count(_.status == RequestStatus.InProgress))
    }

    "insert, select, update address info" in {
      val addressInfos = addressInfoGen.next(3).toList

      // create new item
      addressInfoDao.create(addressInfos).futureValue
      addressInfos.foreach { addressInfo =>
        // check created item exists
        val ai = addressInfoDao.getById(addressInfo.addressInfoId).futureValue
        assert(addressInfo.addressInfoId === ai.addressInfoId)

        val storedAi = addressInfoDao.getById(ai.addressInfoId).futureValue
        assert(storedAi == addressInfo)
      }
    }

    "insert, select, update paid report without address info" in {
      val paidReports = paidReportGen().next(3).toList

      // create new item
      paidReportDao.create(paidReports).futureValue
      paidReports.foreach { paidReport =>
        // check created item exists
        val pr = paidReportDao.getById(paidReport.paidReportId).futureValue
        assert(paidReport.paidReportId == pr.paidReportId)

        // update existing item
        val newLastMessage = Some(Random.nextString(5))
        val newPaymentStatus = PaidReportPaymentStatus.Paid

        paidReportDao
          .update(paidReport.paidReportId) { oldPaidReport =>
            oldPaidReport.copy(
              paymentStatus = newPaymentStatus,
              lastMessage = newLastMessage,
              templateVersion = 2,
              data =
                ProtoPaidReport.newBuilder().setReportBuildDate(Timestamp.newBuilder().setSeconds(1).build()).build()
            )
          }
          .futureValue

        val updatedPr = paidReportDao.getById(paidReport.paidReportId).futureValue

        assert(updatedPr.paymentStatus == newPaymentStatus)
        assert(updatedPr.lastMessage == newLastMessage)
        assert(updatedPr.data.getReportBuildDate.getSeconds == 1)
      }
    }

    "select paid report with address info" in {
      val addressInfos = addressInfoGen.next(3).toList
      val paidReports = paidReportGen(Some(addressInfos)).next(3).toList
      truncateData()

      // create items
      addressInfoDao.create(addressInfos).futureValue
      paidReportDao.create(paidReports).futureValue

      // check created paid reports by id
      paidReports.foreach { paidReport =>
        val pr = paidReportDao.getWithAddressById(paidReport.paidReportId).futureValue
        val address = addressInfos.find(_.addressInfoId == pr.addressInfoId)

        assert(pr.address == address)
      }

      def checkResult(expected: List[PaidReport], selected: List[PaidReport]): Unit = {
        assert(expected.size == selected.size)

        val expecedIds = expected.map(_.paidReportId)
        selected.foreach { selectedPaidReport =>
          assert(expecedIds.contains(selectedPaidReport.paidReportId))
          assert(selectedPaidReport.address.isDefined)
          assert(selectedPaidReport.address.get.addressInfoId == selectedPaidReport.addressInfoId)
        }
      }

      // check created paid reports by owner
      paidReports.groupBy(_.owner).foreach {
        case (owner, expectedPaidReports) =>
          val filter = PaidReportFilter(Set.empty, None, Some(owner), Set.empty, Set.empty, None)
          val selectedPaidReports = paidReportDao.getWithAddressByFilters(filter).futureValue.toList
          checkResult(expectedPaidReports, selectedPaidReports)
      }

      // check created paid reports by payment status
      paidReports.groupBy(_.paymentStatus).foreach {
        case (status, expectedPaidReports) =>
          val filter = PaidReportFilter(Set.empty, None, None, Set(status), Set.empty, None)
          val selectedPaidReports = paidReportDao.getWithAddressByFilters(filter).futureValue.toList
          checkResult(expectedPaidReports, selectedPaidReports)
      }

      // check created paid reports by offerId
      paidReports
        .filter(p => p.address.isDefined && p.address.get.userObjectInfo.offerId.isDefined)
        .groupBy(_.address.get.userObjectInfo.offerId.get)
        .foreach {
          case (offerId, expectedPaidReports) =>
            val filter = PaidReportFilter(Set.empty, Some(offerId), None, Set.empty, Set.empty, None)
            val selectedPaidReports = paidReportDao.getWithAddressByFilters(filter).futureValue.toList
            checkResult(expectedPaidReports, selectedPaidReports)
        }
    }

    "work with paid report votes" in {
      val votes = paidReportVoteGen.next(3).toList

      votes.foreach(paidReportVoteDao.create(_).futureValue)

      votes.foreach { vote =>
        val res = paidReportVoteDao.getById(vote.paidReportId).futureValue
        assert(res.vote == vote.vote)
      }
    }

    "insert, select, update reports and excerpts" in {
      val reports = reportGen().next(3).toList
      truncateData()

      reports.foreach(reportDao.create(_).futureValue)
      reports.foreach { report =>
        val r1 = reportDao.getById(report.reportId).futureValue
        assert(r1.reportId == report.reportId)

        val r2 = Try(reportDao.getLatestPublicByCadastralNumber(report.flatCadastralNumber).futureValue)
        val r3 = reportDao.getAllPublicByCadastralNumber(report.flatCadastralNumber).futureValue

        if (report.paidReportId.isEmpty) {
          assert(r2.isSuccess)
          assert(r2.get.reportId == report.reportId)
          assert(r3.exists(_.cadastralNumber == report.cadastralNumber))
        } else {
          assert(r2.isFailure)
          assert(!r3.exists(_.cadastralNumber == report.cadastralNumber))
        }
      }

      val updatedReports = reports
        .map(_.copy(data = Some(notEmptyReportData)))
        .map(ReportUpdate.extract)
      reportDao.update(updatedReports).futureValue

      reports.foreach { report =>
        val r1 = reportDao.getById(report.reportId).futureValue
        assert(r1.reportId == report.reportId)
        assert(r1.data.get.getFlatReport.getCostInfo.getCadastralCost == 12345678)
      }

      reports.foreach { report =>
        val excerpts = list(1, 2, excerptGen(report.reportId)).next

        excerptDao.create(excerpts).futureValue
        val count = excerptDao.get(report.reportId).futureValue.size

        assert(count == excerpts.length)
      }
    }

    "select latest reports by cadastral numbers" in {
      val now = DateTimeUtil.now()
      val cadastralNumber = "432:5462:43232"

      val reports = reportGen().next(3).toList
      val r1 = reportWithContent(reports.head, cadastralNumber, Some(notEmptyReportData), now.minusMonths(2))
      val r2 = reportWithContent(reports(1), cadastralNumber, Some(notEmptyReportData), now.minusMonths(1))
      val r3 = reportWithContent(reports(2), cadastralNumber, None, now)

      Seq(r1, r2, r3).foreach(reportDao.create(_).futureValue)

      val emptyReportList =
        reportDao.getLatestPublicByCadastralNumbers(Set(cadastralNumber), excludeEmpty = false).futureValue
      assert(emptyReportList.size == 1)
      assert(emptyReportList.head.reportId == r3.reportId)

      val nonEmptyReportList =
        reportDao.getLatestPublicByCadastralNumbers(Set(cadastralNumber), excludeEmpty = true).futureValue
      assert(nonEmptyReportList.size == 1)
      assert(nonEmptyReportList.head.reportId == r2.reportId)
    }

    "select offers by cadastral number" in {
      val cadastralNumberToFind = "432:5462:43232"
      val anotherCadastralNumber = "745:5423:97866"

      val offers = offerGen.next(3).toList
      val o1 = offerWithNumber(offers.head, Some(cadastralNumberToFind))
      val o2 = offerWithNumber(offers(1), Some(anotherCadastralNumber))
      val o3 = offerWithNumber(offers(2), None)

      val allOffers = Seq(o1, o2, o3)
      val offersToBeSelected = allOffers.take(1)

      offerDao.create(allOffers).futureValue
      val selectedOffers = offerDao.getByCadastralNumber(cadastralNumberToFind).futureValue
      assert(selectedOffers.map(_.id).toSet == offersToBeSelected.map(_.id).toSet)
    }
  }

  private def offerWithNumber(offer: Offer, cadastralNumber: Option[String]): Offer =
    offer.withUpdatedSearch(_.copy(cadastralNumber = cadastralNumber))

  private def reportWithContent(
    report: Report,
    cadastralNumber: String,
    data: Option[ExcerptReport],
    created: DateTime
  ): Report =
    report.copy(cadastralNumber = Some(cadastralNumber), paidReportId = None, data = data, created = created)

  private def notEmptyReportData: ExcerptReport =
    ExcerptReport
      .newBuilder()
      .setFlatReport(
        FlatExcerptReport
          .newBuilder()
          .setCostInfo(
            CostInfo
              .newBuilder()
              .setCadastralCost(12345678)
          )
      )
      .build()
}
