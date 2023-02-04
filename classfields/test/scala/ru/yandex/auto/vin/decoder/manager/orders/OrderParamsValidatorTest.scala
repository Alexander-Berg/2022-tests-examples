package ru.yandex.auto.vin.decoder.manager.orders

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.orders.OrdersApiModel.{FailReason, OrderIdentifierType, ReportType}
import ru.yandex.auto.vin.decoder.manager.orders.OrderParamsValidator.ValidatedOrderParams
import ru.yandex.auto.vin.decoder.model.{AutoruOfferId, LicensePlate, MockedFeatures, VinCode}
import ru.yandex.auto.vin.decoder.report.ReportDefinition
import ru.yandex.auto.vin.decoder.report.ReportDefinition.{AdvancedReportType, EnumReportType}
import ru.yandex.auto.vin.decoder.report.processors.report.ReportDefinitionManager
import ru.yandex.vertis.mockito.MockitoSupport

class OrderParamsValidatorTest extends AnyWordSpecLike with Matchers with MockedFeatures with MockitoSupport {

  private val validator = new OrderParamsValidator(features)
  private val reportDefinitionManager = mock[ReportDefinitionManager]
  private val TestVin = VinCode("SALVA2BG8CH610042")
  private val TestLp = LicensePlate("A124BC77")
  private val TestOfferId = AutoruOfferId.check("123-abc").get

  "validator" should {
    "return INVALID_IDENTIFIER" when {
      "invalid vin" in {
        val res = validator.validate(
          EnumReportType(ReportType.FULL_REPORT),
          "A",
          OrderIdentifierType.VIN
        )
        res shouldBe Left(FailReason.INVALID_IDENTIFIER)
      }
      "invalid lp" in {
        val res = validator.validate(
          EnumReportType(ReportType.FULL_REPORT),
          "A",
          OrderIdentifierType.LICENSE_PLATE
        )
        res shouldBe Left(FailReason.INVALID_IDENTIFIER)
      }
      "invalid offer id" in {
        val res = validator.validate(
          EnumReportType(ReportType.FULL_REPORT),
          "A",
          OrderIdentifierType.AUTORU_OFFER_ID
        )
        res shouldBe Left(FailReason.INVALID_IDENTIFIER)
      }
    }
    "return NOT_SUPPORTED_IDENTIFIER_TYPE" when {
      for (rt <- List(ReportType.FULL_REPORT, ReportType.GIBDD_REPORT))
        s"UNKNOWN_IDENTIFIER_TYPE for $rt" in {
          val res = validator.validate(
            EnumReportType(ReportType.GIBDD_REPORT),
            "A",
            OrderIdentifierType.UNKNOWN_ORDER_IDENTIFIER
          )
          res shouldBe Left(FailReason.NOT_SUPPORTED_IDENTIFIER_TYPE)
        }
      "license_plate for gibdd_report" in {
        val res = validator.validate(
          EnumReportType(ReportType.GIBDD_REPORT),
          TestLp.toString,
          OrderIdentifierType.LICENSE_PLATE
        )
        res shouldBe Left(FailReason.NOT_SUPPORTED_IDENTIFIER_TYPE)
      }
      "offer_id for gibdd_report" in {
        val res = validator.validate(
          EnumReportType(ReportType.GIBDD_REPORT),
          TestOfferId.toString,
          OrderIdentifierType.AUTORU_OFFER_ID
        )
        res shouldBe Left(FailReason.NOT_SUPPORTED_IDENTIFIER_TYPE)
      }
    }
    "return NOT_SUPPORTED_REPORT_TYPE" when {
      "unknown report type" in {
        val res = validator.validate(
          EnumReportType(ReportType.UNKNOWN_REPORT),
          TestVin.toString,
          OrderIdentifierType.VIN
        )
        res shouldBe Left(FailReason.NOT_SUPPORTED_REPORT_TYPE)
      }
      "advanced report type is disabled" in {
        val reportDefinition = mock[ReportDefinition]
        when(features.EnableOrderByStringReportId).thenReturn(disabledFeature)
        val res = validator.validate(
          AdvancedReportType(reportDefinition),
          TestVin.toString,
          OrderIdentifierType.VIN
        )
        res shouldBe Left(FailReason.NOT_SUPPORTED_REPORT_TYPE)
      }
      "requested report type does not exist" in {
        val reportDefinition = mock[ReportDefinition]
        when(reportDefinition.id).thenReturn("any_id")
        when(features.EnableOrderByStringReportId).thenReturn(disabledFeature)
        val res = validator.validate(
          AdvancedReportType(reportDefinition),
          TestVin.toString,
          OrderIdentifierType.VIN
        )
        res shouldBe Left(FailReason.NOT_SUPPORTED_REPORT_TYPE)
      }
    }
    "return success result" when {
      "valid gibdd_report order" in {
        val res = validator.validate(
          EnumReportType(ReportType.GIBDD_REPORT),
          TestVin.toString,
          OrderIdentifierType.VIN
        )
        res shouldBe Right(
          ValidatedOrderParams(
            TestVin,
            EnumReportType(ReportType.GIBDD_REPORT),
            OrderIdentifierType.VIN
          )
        )
      }
      "valid full_report order with vin" in {
        val res = validator.validate(
          EnumReportType(ReportType.GIBDD_REPORT),
          TestVin.toString,
          OrderIdentifierType.VIN
        )
        res shouldBe Right(
          ValidatedOrderParams(
            TestVin,
            EnumReportType(ReportType.GIBDD_REPORT),
            OrderIdentifierType.VIN
          )
        )
      }
      "valid full_report order with lp" in {
        val res = validator.validate(
          EnumReportType(ReportType.FULL_REPORT),
          TestLp.toString,
          OrderIdentifierType.LICENSE_PLATE
        )
        res shouldBe Right(
          ValidatedOrderParams(
            TestLp,
            EnumReportType(ReportType.FULL_REPORT),
            OrderIdentifierType.LICENSE_PLATE
          )
        )
      }
      "valid full_report order with offer_id" in {
        val res = validator.validate(
          EnumReportType(ReportType.FULL_REPORT),
          TestOfferId.toString,
          OrderIdentifierType.AUTORU_OFFER_ID
        )
        res shouldBe Right(
          ValidatedOrderParams(
            TestOfferId,
            EnumReportType(ReportType.FULL_REPORT),
            OrderIdentifierType.AUTORU_OFFER_ID
          )
        )
      }
    }
  }

}
