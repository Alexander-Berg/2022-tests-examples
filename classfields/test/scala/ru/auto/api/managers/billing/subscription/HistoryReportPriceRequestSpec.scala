package ru.auto.api.managers.billing.subscription

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.{BaseSpec, BetterTryValues}
import ru.auto.api.exceptions.SalesmanBadRequest
import ru.auto.api.model.ModelGenerators._

/**
  * [[HistoryReportPriceRequest.ForReportParams.asSalesmanPriceRequest]] и
  * [[HistoryReportPriceRequest.RichSalesmanPriceRequest.withUserModerationStatus]]
  * покрыты более высокоуровневыми тестами
  * в [[ru.auto.api.managers.carfax.CarfaxWalletManagerSpec]],
  * поэтому в этом тесте не проверяются.
  * Кейс forSingleReportPayment = true тоже покрыт в [[SubscriptionManagerSpec]].
  */
class HistoryReportPriceRequestSpec extends BaseSpec with ScalaCheckPropertyChecks with BetterTryValues {

  "HistoryReportPriceRequest.asSalesmanPriceRequest" should {

    "return PriceRequest with offer with cleared state" in {
      forAll(OfferGen, Gen.posNum[Int]) { (offer, contentQuality) =>
        HistoryReportPriceRequest
          .ForOffer(offer, contentQuality)
          .asSalesmanPriceRequest
          .success
          .value
          .getOfferHistory
          .getOffer shouldBe offer.toBuilder.clearState().build()
      }
    }

    "return PriceRequest with contentQuality" in {
      forAll(OfferGen, Gen.posNum[Int]) { (offer, contentQuality) =>
        HistoryReportPriceRequest
          .ForOffer(offer, contentQuality)
          .asSalesmanPriceRequest
          .success
          .value
          .getOfferHistory
          .getContentQuality shouldBe contentQuality
      }
    }

    "return PriceRequest for valid vin" in {
      val validVin = "WDC1648221A770037"
      HistoryReportPriceRequest
        .ForObjectIds(
          vinOrLicensePlate = Some(validVin),
          offerId = None,
          forSingleReportPayment = false
        )
        .asSalesmanPriceRequest
        .success
        .value
        .getVinHistory
        .getVin shouldBe validVin
    }

    "return PriceRequest for valid license plate" in {
      val validLicensePlate = "A999PK197"
      HistoryReportPriceRequest
        .ForObjectIds(
          vinOrLicensePlate = Some(validLicensePlate),
          offerId = None,
          forSingleReportPayment = false
        )
        .asSalesmanPriceRequest
        .success
        .value
        .getVinHistory
        .getLicensePlate shouldBe validLicensePlate
    }

    "throw SalesmanBadRequest on invalid vin" in {
      val invalidVin = "WDC16"
      HistoryReportPriceRequest
        .ForObjectIds(
          vinOrLicensePlate = Some(invalidVin),
          offerId = None,
          forSingleReportPayment = false
        )
        .asSalesmanPriceRequest
        .failure
        .exception shouldBe a[SalesmanBadRequest]
    }

    "throw SalesmanBadRequest on invalid license plate" in {
      val invalidLicensePlate = "9PK197"
      HistoryReportPriceRequest
        .ForObjectIds(
          vinOrLicensePlate = Some(invalidLicensePlate),
          offerId = None,
          forSingleReportPayment = false
        )
        .asSalesmanPriceRequest
        .failure
        .exception shouldBe a[SalesmanBadRequest]
    }
  }
}
