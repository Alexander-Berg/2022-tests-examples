package ru.yandex.realty.cadastr.reportbuilder

import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.cadastr.backend.FlatReportBuilder
import ru.yandex.realty.cadastr.parser.ExcerptParserBase

@RunWith(classOf[JUnitRunner])
class ReportBuilderSpec extends ExcerptParserBase {

  "FlatReportBuilder" should {

    "correctly map previous Owners and Registrations into the Right" in {
      val rights = getSimpleRightsExcerpt
      val rightMovement = getSimpleRightMovementExcerpt

      val report = FlatReportBuilder.buildReport(rights, Some(rightMovement))

      assertEquals(report.getFlatReport.getPreviousRightsCount, 3)

      val prevRight1 = report.getFlatReport.getPreviousRightsList.get(0)
      val prevRight2 = report.getFlatReport.getPreviousRightsList.get(1)
      val prevRight3 = report.getFlatReport.getPreviousRightsList.get(2)

      assertEquals(prevRight1.getOwnersList.get(0).getTypeValue, 3)
      assertEquals(prevRight1.getOwnersCount, 1)
      assertEquals(prevRight1.getRegistration.getTypeValue, 1)
      assertTrue(prevRight1.getRegistration.getEndDate.getSeconds > 0)

      assertEquals(prevRight2.getOwnersList.get(0).getTypeValue, 1)
      assertEquals(prevRight2.getOwnersCount, 1)
      assertEquals(prevRight2.getRegistration.getTypeValue, 2)
      assertTrue(prevRight2.getRegistration.getEndDate.getSeconds > 0)
      assertEquals(prevRight2.getOwnersList.get(0).getName, "")

      assertEquals(prevRight3.getOwnersList.get(0).getTypeValue, 1)
      assertEquals(prevRight3.getOwnersCount, 1)
      assertEquals(prevRight3.getRegistration.getTypeValue, 2)
      assertTrue(prevRight3.getRegistration.getEndDate.getSeconds > 0)
      assertEquals(prevRight3.getOwnersList.get(0).getName, "")

      assertEquals(
        "р-н Чертаново Центральное, ул Чертановская, д 48, корп 2, кв 111",
        report.getFlatReport.getBuildingInfo.getAddress
      )
    }

    "build report for empty excerpts" in {
      val rights = getEmptyRightsExcerpt
      val rightMovement = getEmptyRightMovementExcerpt

      val report = FlatReportBuilder.buildReport(rights, Some(rightMovement))

      assertEquals(report.getFlatReport.getCurrentRightsCount, 0)
      assertEquals(report.getFlatReport.getPreviousRightsCount, 0)
    }
  }
}
