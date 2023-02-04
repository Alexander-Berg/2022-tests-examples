package ru.yandex.auto.vin.decoder.report.converters.formats.proto.builder.additional

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import ru.auto.api.vin.VinReportModel.RawVinReport
import ru.auto.api.vin.VinResolutionEnums.Status
import ru.yandex.auto.vin.decoder.report.converters.raw.AdditionalReportData

class ContentForReadyReportBuilderTest extends AnyFunSuite with Matchers {
  val boolChoices = Set(false, true)

  // только для покупателя бесплатного отчета
  val showMinusOne = Table(
    ("hasDtp", "isBeaten"),
    (true, false),
    (true, true),
    (false, true)
  )

  // только при наличи дтп
  val showDtpCounter = Table(
    ("requestFromOwner", "isFull"),
    (false, true),
    (true, false),
    (true, true)
  )

  // только когда нет дтп
  val showZeroDtp = Table(
    ("requestFromOwner", "isFull", "isBeaten"),
    (false, false, false),
    (false, true, false),
    (false, true, true),
    (true, false, false),
    (true, false, true),
    (true, true, false),
    (true, true, true)
  )

  private def buildRawVinReport(addDtp: Boolean, addAccident: Boolean) = {
    val builder = RawVinReport.newBuilder()

    val dtpBuilder = builder.getDtpBuilder
    dtpBuilder.getHeaderBuilder.setTitle("")

    if (addDtp) {
      dtpBuilder.addItemsBuilder(0)
    }

    if (addAccident) {
      builder.getHistoryBuilder
        .addOwnersBuilder(0)
        .addHistoryRecordsBuilder(0)
        .getTotalAuctionRecordBuilder
        .setAuction("")
    }

    builder.build()
  }

  private def buildAdditionalReportData(requestFromOwner: Boolean, isFull: Boolean) = {
    AdditionalReportData.Empty.copy(isFree = !isFull, requestFromOwner = requestFromOwner)
  }

  private def buildDtp(
      requestFromOwner: Boolean,
      isFull: Boolean,
      hasDtp: Boolean,
      isBeaten: Boolean) = {
    ContentForReadyReportBuilder.buildDtp(
      buildRawVinReport(hasDtp, isBeaten),
      buildAdditionalReportData(requestFromOwner, isFull),
      dtpCountRaw = Some(if (hasDtp) 1 else 0),
      existsBeaten = isBeaten,
      isFree = !isFull
    )
  }

  forAll(showMinusOne) { (hasDtp, isBeaten) =>
    test(s"show unknown dtp if hasDtp=$hasDtp, isBeaten=$isBeaten") {
      val maybeDtp = buildDtp(requestFromOwner = false, isFull = false, hasDtp, isBeaten)
      assert(maybeDtp.nonEmpty)

      for (dtp <- maybeDtp) {
        assert(dtp.getRecordCount == -1)
        assert(dtp.getStatus == Status.UNDEFINED)
      }
    }
  }

  forAll(showDtpCounter) { (requestFromOwner, isFull) =>
    boolChoices.foreach { isBeaten =>
      test(s"show dtp counter if requestFromOwner=$requestFromOwner, isFull=$isFull, isBeaten=$isBeaten") {
        val maybeDtp = buildDtp(requestFromOwner, isFull, hasDtp = true, isBeaten)
        assert(maybeDtp.nonEmpty)

        for (dtp <- maybeDtp) {
          assert(dtp.getRecordCount > 0)
          assert(dtp.getStatus == Status.ERROR)
        }
      }
    }
  }

  forAll(showZeroDtp) { (requestFromOwner, isFull, isBeaten) =>
    test(s"show zero dtp counter if requestFromOwner=$requestFromOwner, isFull=$isFull, isBeaten=$isBeaten") {
      val maybeDtp = buildDtp(requestFromOwner, isFull, hasDtp = false, isBeaten)
      assert(maybeDtp.nonEmpty)

      for (dtp <- maybeDtp) {
        assert(dtp.getStatus == Status.OK)
        assert(dtp.getRecordCount == 0)
      }
    }
  }
}
