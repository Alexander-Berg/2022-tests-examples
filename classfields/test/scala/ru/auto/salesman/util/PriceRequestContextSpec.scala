package ru.auto.salesman.util

import ru.auto.api.vin.VinApiModel.ReportParams
import ru.auto.salesman.model.{AutoruUser, VinReportParams}
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.PriceRequestModel.PriceRequest
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.util.PriceRequestContextType.{OfferHistory, VinHistory}

class PriceRequestContextSpec extends BaseSpec with OfferModelGenerators {

  "apply() for PriceRequestModel" should {
    "return context for OfferHistory [empty params]" in {
      val priceRequest = PriceRequest
        .newBuilder()
        .setOfferHistory {
          PriceRequest.OfferHistory.newBuilder()
        }
        .build()

      val result = PriceRequestContext(
        user = Some(AutoruUser(123)),
        applyMoneyFeature = false,
        priceRequest
      )

      result shouldBe PriceRequestContext(
        Some(OfferHistory),
        userModerationStatus = None,
        Some(AutoruUser(123)),
        offerId = None,
        autoruOffer = None,
        category = None,
        section = None,
        geoId = None,
        vin = None,
        vinReportParams = None,
        licensePlate = None,
        contentQuality = None,
        applyMoneyFeature = false,
        applyProlongInterval = true
      )
    }

    "return context for OfferHistory [offerId]" in {
      val priceRequest = PriceRequest
        .newBuilder()
        .setOfferHistory {
          PriceRequest.OfferHistory
            .newBuilder()
            .setOfferId("1234-fff")
        }
        .build()

      val result = PriceRequestContext(
        user = Some(AutoruUser(123)),
        applyMoneyFeature = false,
        priceRequest
      )

      result shouldBe PriceRequestContext(
        Some(OfferHistory),
        userModerationStatus = None,
        Some(AutoruUser(123)),
        Some(AutoruOfferId("1234-fff")),
        autoruOffer = None,
        category = None,
        section = None,
        geoId = None,
        vin = None,
        vinReportParams = None,
        licensePlate = None,
        contentQuality = None,
        applyMoneyFeature = false,
        applyProlongInterval = true
      )
    }

    "return context for OfferHistory [offer]" in {
      val offer = ActiveOfferGen.next

      val priceRequest = PriceRequest
        .newBuilder()
        .setOfferHistory {
          PriceRequest.OfferHistory
            .newBuilder()
            .setOffer(offer)
            .setContentQuality(20L)
        }
        .build()

      val result = PriceRequestContext(
        user = Some(AutoruUser(123)),
        applyMoneyFeature = false,
        priceRequest
      )

      result shouldBe PriceRequestContext(
        Some(OfferHistory),
        userModerationStatus = None,
        Some(AutoruUser(123)),
        offerId = None,
        Some(offer),
        category = None,
        section = None,
        geoId = None,
        vin = None,
        vinReportParams = None,
        licensePlate = None,
        contentQuality = Some(20),
        applyMoneyFeature = false,
        applyProlongInterval = true
      )
    }

    "return context for OfferHistory [oneOf offerId vs offer]" in {
      val offer = ActiveOfferGen.next

      val priceRequest = PriceRequest
        .newBuilder()
        .setOfferHistory {
          PriceRequest.OfferHistory
            .newBuilder()
            .setOfferId("123-fff")
            .setOffer(offer)
        }
        .build()

      val result = PriceRequestContext(
        user = Some(AutoruUser(123)),
        applyMoneyFeature = false,
        priceRequest
      )

      result shouldBe PriceRequestContext(
        Some(OfferHistory),
        userModerationStatus = None,
        Some(AutoruUser(123)),
        offerId = None,
        Some(offer),
        category = None,
        section = None,
        geoId = None,
        vin = None,
        vinReportParams = None,
        licensePlate = None,
        contentQuality = None,
        applyMoneyFeature = false,
        applyProlongInterval = true
      )
    }

    "return context for OfferHistory [oneOf offer vs offerId]" in {
      val offer = ActiveOfferGen.next

      val priceRequest = PriceRequest
        .newBuilder()
        .setOfferHistory {
          PriceRequest.OfferHistory
            .newBuilder()
            .setOffer(offer)
            .setOfferId("123-fff")
        }
        .build()

      val result = PriceRequestContext(
        user = Some(AutoruUser(123)),
        applyMoneyFeature = false,
        priceRequest
      )

      result shouldBe PriceRequestContext(
        Some(OfferHistory),
        userModerationStatus = None,
        Some(AutoruUser(123)),
        Some(AutoruOfferId("123-fff")),
        autoruOffer = None,
        category = None,
        section = None,
        geoId = None,
        vin = None,
        vinReportParams = None,
        licensePlate = None,
        contentQuality = None,
        applyMoneyFeature = false,
        applyProlongInterval = true
      )
    }

    "return context for VinHistory [empty params]" in {
      val priceRequest = PriceRequest
        .newBuilder()
        .setVinHistory {
          PriceRequest.VinHistory.newBuilder()
        }
        .build()

      val result = PriceRequestContext(
        user = Some(AutoruUser(123)),
        applyMoneyFeature = false,
        priceRequest
      )

      result shouldBe PriceRequestContext(
        Some(VinHistory),
        userModerationStatus = None,
        Some(AutoruUser(123)),
        offerId = None,
        autoruOffer = None,
        category = None,
        section = None,
        geoId = None,
        vin = None,
        vinReportParams = None,
        licensePlate = None,
        contentQuality = None,
        applyMoneyFeature = false,
        applyProlongInterval = true
      )
    }

    "return context for VinHistory [report params]" in {
      val vinReportParamsProto = ReportParams
        .newBuilder()
        .setMark("AUDI")
        .setModel("A8")
        .build()

      val priceRequest = PriceRequest
        .newBuilder()
        .setVinHistory {
          PriceRequest.VinHistory
            .newBuilder()
            .setReportParams(vinReportParamsProto)
        }
        .build()

      val vinReportParams = VinReportParams(
        mark = Some("AUDI"),
        model = Some("A8"),
        year = Some(0)
      )

      val result = PriceRequestContext(
        user = Some(AutoruUser(123)),
        applyMoneyFeature = false,
        priceRequest
      )

      result shouldBe PriceRequestContext(
        Some(VinHistory),
        userModerationStatus = None,
        Some(AutoruUser(123)),
        offerId = None,
        autoruOffer = None,
        category = None,
        section = None,
        geoId = None,
        vin = None,
        vinReportParams = Some(vinReportParams),
        licensePlate = None,
        contentQuality = None,
        applyMoneyFeature = false,
        applyProlongInterval = true
      )
    }

    "return context for VinHistory [vin VS licensePlate]" in {
      val priceRequest = PriceRequest
        .newBuilder()
        .setVinHistory {
          PriceRequest.VinHistory
            .newBuilder()
            .setVin("XXX123")
            .setLicensePlate("B777OP77")
        }
        .build()

      val result = PriceRequestContext(
        user = Some(AutoruUser(123)),
        applyMoneyFeature = false,
        priceRequest
      )

      result shouldBe PriceRequestContext(
        Some(VinHistory),
        userModerationStatus = None,
        Some(AutoruUser(123)),
        offerId = None,
        autoruOffer = None,
        category = None,
        section = None,
        geoId = None,
        vin = None,
        vinReportParams = None,
        licensePlate = Some("B777OP77"),
        contentQuality = None,
        applyMoneyFeature = false,
        applyProlongInterval = true
      )
    }

    "return context for VinHistory [licensePlate VS vin]" in {
      val priceRequest = PriceRequest
        .newBuilder()
        .setVinHistory {
          PriceRequest.VinHistory
            .newBuilder()
            .setLicensePlate("B777OP77")
            .setVin("XXX123")
        }
        .build()

      val result = PriceRequestContext(
        user = Some(AutoruUser(123)),
        applyMoneyFeature = false,
        priceRequest
      )

      result shouldBe PriceRequestContext(
        Some(VinHistory),
        userModerationStatus = None,
        Some(AutoruUser(123)),
        offerId = None,
        autoruOffer = None,
        category = None,
        section = None,
        geoId = None,
        vin = Some("XXX123"),
        vinReportParams = None,
        licensePlate = None,
        contentQuality = None,
        applyMoneyFeature = false,
        applyProlongInterval = true
      )
    }

    "return context for VinHistory [vin VS reportParams]" in {
      val vinReportParamsProto = ReportParams
        .newBuilder()
        .setMark("AUDI")
        .setModel("A8")
        .build()

      val priceRequest = PriceRequest
        .newBuilder()
        .setVinHistory {
          PriceRequest.VinHistory
            .newBuilder()
            .setVin("XXX123")
            .setReportParams(vinReportParamsProto)
        }
        .build()

      val result = PriceRequestContext(
        user = Some(AutoruUser(123)),
        applyMoneyFeature = false,
        priceRequest
      )

      val vinReportParams = VinReportParams(
        mark = Some("AUDI"),
        model = Some("A8"),
        year = Some(0)
      )

      result shouldBe PriceRequestContext(
        Some(VinHistory),
        userModerationStatus = None,
        Some(AutoruUser(123)),
        offerId = None,
        autoruOffer = None,
        category = None,
        section = None,
        geoId = None,
        vin = None,
        vinReportParams = Some(vinReportParams),
        licensePlate = None,
        contentQuality = None,
        applyMoneyFeature = false,
        applyProlongInterval = true
      )
    }

    "return context for VinHistory [reportParams VS vin]" in {
      val vinReportParams = ReportParams
        .newBuilder()
        .setMark("AUDI")
        .setModel("A8")
        .build()

      val priceRequest = PriceRequest
        .newBuilder()
        .setVinHistory {
          PriceRequest.VinHistory
            .newBuilder()
            .setReportParams(vinReportParams)
            .setVin("XXX123")
        }
        .build()

      val result = PriceRequestContext(
        user = Some(AutoruUser(123)),
        applyMoneyFeature = false,
        priceRequest
      )

      result shouldBe PriceRequestContext(
        Some(VinHistory),
        userModerationStatus = None,
        Some(AutoruUser(123)),
        offerId = None,
        autoruOffer = None,
        category = None,
        section = None,
        geoId = None,
        vin = Some("XXX123"),
        vinReportParams = None,
        licensePlate = None,
        contentQuality = None,
        applyMoneyFeature = false,
        applyProlongInterval = true
      )
    }

  }

}
