package ru.auto.salesman.service.impl.user.autoru.price.service

import org.scalacheck.Gen
import ru.auto.salesman._
import ru.auto.salesman.dao.user.GoodsDao
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, OfferTypes}
import ru.auto.salesman.service.impl.user.autoru.price.service.placement.{
  PlacementQuotaCalculatorImpl,
  PlacementQuotaServiceImpl
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.test.dao.gens.VosRequestGenerators
import ru.yandex.vertis.generators.BasicGenerators
import PlacementQuotaCalculatorImpl.RichProductQuota
import ru.auto.salesman.model.user.PaymentReasons
import zio.ZIO

class PlacementQuotaCalculatorImplSpec
    extends BaseSpec
    with ServiceModelGenerators
    with VosRequestGenerators
    with BasicGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val quotaService: PlacementQuotaServiceImpl = mock[PlacementQuotaServiceImpl]
  val goodsDao: GoodsDao = mock[GoodsDao]

  val freeOffersCountCalculator: FreeOffersCountCalculatorImpl =
    mock[FreeOffersCountCalculatorImpl]

  val quotaCalculator =
    new PlacementQuotaCalculatorImpl(quotaService)

  "enrichWithQuotaByGeneration" should {
    "return quota with quotaByGeneration fields" in {
      forAll(
        (ProductQuotaGen, "quota"),
        (ProductQuotaGen, "quotaByGeneration")
      ) { (quota, quotaByGeneration) =>
        val q = quota.enrichWithQuotaByGeneration(Some(quotaByGeneration))
        q.duration shouldBe quotaByGeneration.duration
        q.reason shouldBe quotaByGeneration.reason
        q.size shouldBe quotaByGeneration.size
      }
    }

    "use quota if quotaByGeneration empty" in {
      forAll(ProductQuotaGen) { quota =>
        quota.enrichWithQuotaByGeneration(None) shouldBe quota
      }
    }
  }

  "enrichWithDefaultPaymentReason" should {
    "return same quota if quota with paymentReason" in {
      forAll(PaymentReasonGen) { paymentReason =>
        forAll(ProductQuotaGen.map(_.copy(reason = Some(paymentReason)))) { quota =>
          quota.enrichWithDefaultPaymentReason() shouldBe quota
        }
      }
    }

    "return same quota if quota size > 0" in {
      forAll(Gen.posNum[Int]) { size =>
        forAll(ProductQuotaGen.map(_.copy(size))) { quota =>
          quota.enrichWithDefaultPaymentReason() shouldBe quota
        }
      }
    }

    "return enriched quota" in {
      forAll(ProductQuotaGen.map(_.copy(reason = None, size = 0))) { quota =>
        quota.enrichWithDefaultPaymentReason() shouldBe quota.copy(
          reason = Some(PaymentReasons.FreeLimit)
        )
      }
    }
  }

  val enrichedOffer = EnrichedOfferGen.next
  val userSellerType = UserSellerTypeGen.next
  "placementQuota" should {
    "return quota with duration and offerType from quotaByGeneration" in {
      forAll(
        (ProductQuotaGen, "qByGeneration"),
        (Gen.option(ProductQuotaGen), "qBySameOffers"),
        (Gen.option(ProductQuotaGen), "qByUser"),
        (Gen.option(ProductQuotaGen), "qByCategory")
      ) { (qByGeneration, qBySameOffers, qByUser, qByCategory) =>
        (quotaService.getQuotaByUserType _)
          .expects(*)
          .returningZ(qByUser)

        if (qByUser.isEmpty)
          (quotaService.getQuotaBySameOffers _)
            .expects(*)
            .returningZ(qBySameOffers)
        (quotaService.getQuotaByCategory _)
          .expects(*)
          .returningZ(qByCategory)

        (quotaService.getQuotaByGeneration _)
          .expects(*)
          .returningZ(Some(qByGeneration))

        (quotaService.reduceQuotaByUsedFreeOffers _)
          .expects(*, *, *)
          .onCall((quota, _, _) => ZIO.succeed(quota))
          .anyNumberOfTimes()

        val calculatedQuota = quotaCalculator
          .placementQuota(enrichedOffer, userSellerType)
          .success
          .value

        calculatedQuota.duration shouldBe qByGeneration.duration
        calculatedQuota.offerType.get shouldBe quotaCalculator.getOfferType(
          qByGeneration
        )
      }

    }

    "return quota with duration and offerType from quotaByCategory" in {
      forAll(
        (Gen.option(ProductQuotaGen), "qBySameOffers"),
        (Gen.option(ProductQuotaGen), "qByUser"),
        (ProductQuotaGen, "qByCategory")
      ) { (qBySameOffers, qByUser, qByCategory) =>
        (quotaService.getQuotaByUserType _)
          .expects(*)
          .returningZ(qByUser)

        if (qByUser.isEmpty)
          (quotaService.getQuotaBySameOffers _)
            .expects(*)
            .returningZ(qBySameOffers)
        (quotaService.getQuotaByCategory _)
          .expects(*)
          .returningZ(Some(qByCategory))

        (quotaService.getQuotaByGeneration _)
          .expects(*)
          .returningZ(None)

        (quotaService.reduceQuotaByUsedFreeOffers _)
          .expects(*, *, *)
          .onCall((quota, _, _) => ZIO.succeed(quota))
          .anyNumberOfTimes()

        val calculatedQuota = quotaCalculator
          .placementQuota(enrichedOffer, userSellerType)
          .success
          .value

        calculatedQuota.duration shouldBe qByCategory.duration
        calculatedQuota.offerType.get shouldBe quotaCalculator.getOfferType(
          qByCategory
        )
      }
    }

    "use default duration and offerType if quotaByGeneration and quotaByCategory are empty" in {
      forAll(
        (Gen.option(ProductQuotaGen), "qBySameOffers"),
        (Gen.option(ProductQuotaGen), "qByUser")
      ) { (qBySameOffers, qByUser) =>
        (quotaService.getQuotaByUserType _)
          .expects(*)
          .returningZ(qByUser)

        if (qByUser.isEmpty)
          (quotaService.getQuotaBySameOffers _)
            .expects(*)
            .returningZ(qBySameOffers)
        (quotaService.getQuotaByCategory _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByGeneration _)
          .expects(*)
          .returningZ(None)

        (quotaService.reduceQuotaByUsedFreeOffers _)
          .expects(*, *, *)
          .onCall((quota, _, _) => ZIO.succeed(quota))
          .anyNumberOfTimes()

        val calculatedQuota = quotaCalculator
          .placementQuota(enrichedOffer, userSellerType)
          .success
          .value

        calculatedQuota.duration shouldBe None
        calculatedQuota.offerType.get shouldBe OfferTypes.Regular
      }
    }

    "usingQuotaByGeneration = true, expects quotaBySameOffers empty, quotaByUserAndOffer empty" in {
      forAll((ProductQuotaGen.map(_.copy(size = 2)), "qByGeneration")) { qByGeneration =>
        val usingQuotaByGenerationExpected = true

        (quotaService.getQuotaBySameOffers _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByUserType _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByCategory _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByGeneration _)
          .expects(*)
          .returningZ(Some(qByGeneration))

        //main check in this test
        (quotaService.reduceQuotaByUsedFreeOffers _)
          .expects(*, *, usingQuotaByGenerationExpected)
          .onCall((quota, _, _) => ZIO.succeed(quota))

        quotaCalculator
          .placementQuota(enrichedOffer, userSellerType)
          .success
      }
    }

    "usingQuotaByGeneration = false, expects using quotaBySameOffers or quotaByUserAndOffer" in {
      forAll(
        (Gen.option(ProductQuotaGen.map(_.copy(size = 1))), "qBySameOffers"),
        (Gen.option(ProductQuotaGen.map(_.copy(size = 1))), "qByUser"),
        (Gen.option(ProductQuotaGen.map(_.copy(size = 1))), "qByCategory")
      ) { (qBySameOffers, qByUser, qByCategory) =>
        val usingQuotaByGenerationExpected = false

        (quotaService.getQuotaByUserType _)
          .expects(*)
          .returningZ(qByUser)

        if (qByUser.isEmpty)
          (quotaService.getQuotaBySameOffers _)
            .expects(*)
            .returningZ(qBySameOffers)

        (quotaService.getQuotaByCategory _)
          .expects(*)
          .returningZ(qByCategory)

        (quotaService.getQuotaByGeneration _)
          .expects(*)
          .returningZ(None)

        //main check in this test
        (quotaService.reduceQuotaByUsedFreeOffers _)
          .expects(*, *, usingQuotaByGenerationExpected)
          .onCall((quota, _, _) => ZIO.succeed(quota))

        quotaCalculator
          .placementQuota(enrichedOffer, userSellerType)
          .success
      }
    }

    "usingQuotaByGeneration = false, expects not using quotaBySameOffers or quotaByUserAndOffer and quotaByGeneration is empty" in {
      forAll(
        (Gen.option(ProductQuotaGen.map(_.copy(size = 1))), "qByCategory")
      ) { qByCategory =>
        val usingQuotaByGenerationExpected = false

        (quotaService.getQuotaBySameOffers _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByUserType _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByCategory _)
          .expects(*)
          .returningZ(qByCategory)

        (quotaService.getQuotaByGeneration _)
          .expects(*)
          .returningZ(None)

        //main check in this test
        (quotaService.reduceQuotaByUsedFreeOffers _)
          .expects(*, *, usingQuotaByGenerationExpected)
          .onCall((quota, _, _) => ZIO.succeed(quota))

        quotaCalculator
          .placementQuota(enrichedOffer, userSellerType)
          .success
      }
    }

    "qByUser not empty -> in result same type of qByUser" in {
      forAll(
        (Gen.some(ProductQuotaGen), "qByUser"),
        (Gen.option(ProductQuotaGen), "qByCategory"),
        (Gen.option(ProductQuotaGen), "qByGeneration")
      ) { (qByUser, qByCategory, qByGeneration) =>
        (quotaService.getQuotaByUserType _)
          .expects(*)
          .returningZ(qByUser)

        (quotaService.getQuotaByCategory _)
          .expects(*)
          .returningZ(qByCategory)

        (quotaService.getQuotaByGeneration _)
          .expects(*)
          .returningZ(qByGeneration)

        (quotaService.reduceQuotaByUsedFreeOffers _)
          .expects(*, *, *)
          .onCall((quota, _, _) => ZIO.succeed(quota))
          .anyNumberOfTimes()

        val q = quotaCalculator
          .placementQuota(enrichedOffer, userSellerType)
          .success
          .value

        q.reason shouldBe qByUser.get.reason
      }
    }

    "qByUser empty, quotaBySameOffer not empty -> in result same type of quotaBySameOffer" in {
      forAll(
        (Gen.some(ProductQuotaGen), "qBySameOffer"),
        (Gen.option(ProductQuotaGen), "qByCategory"),
        (Gen.option(ProductQuotaGen), "qByGeneration")
      ) { (qBySameOffer, qByCategory, qByGeneration) =>
        (quotaService.getQuotaByUserType _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaBySameOffers _)
          .expects(*)
          .returningZ(qBySameOffer)

        (quotaService.getQuotaByCategory _)
          .expects(*)
          .returningZ(qByCategory)

        (quotaService.getQuotaByGeneration _)
          .expects(*)
          .returningZ(qByGeneration)

        (quotaService.reduceQuotaByUsedFreeOffers _)
          .expects(*, *, *)
          .onCall((quota, _, _) => ZIO.succeed(quota))
          .anyNumberOfTimes()

        val q = quotaCalculator
          .placementQuota(enrichedOffer, userSellerType)
          .success
          .value

        q.reason shouldBe qBySameOffer.get.reason
      }
    }

    "quotaBySameOffer empty, quotaByUser empty -> use combined from default, byCategory, byGeneration quota" in {
      forAll(
        (ProductQuotaGen, "qByCategory"),
        (ProductQuotaGen, "qByGeneration")
      ) { (qByCategory, qByGeneration) =>
        (quotaService.getQuotaBySameOffers _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByUserType _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByCategory _)
          .expects(*)
          .returningZ(Some(qByCategory))

        (quotaService.getQuotaByGeneration _)
          .expects(*)
          .returningZ(Some(qByGeneration))

        (quotaService.reduceQuotaByUsedFreeOffers _)
          .expects(*, *, *)
          .onCall((quota, _, _) => ZIO.succeed(quota))
          .anyNumberOfTimes()

        val q = quotaCalculator
          .placementQuota(enrichedOffer, userSellerType)
          .success
          .value

        q.size shouldBe qByGeneration.size
        q.reason shouldBe qByGeneration.reason.orElse {
          Option.when(q.size == 0)(PaymentReasons.FreeLimit)
        }
        q.baseQuota shouldBe qByCategory.baseQuota
        q.duration shouldBe qByGeneration.duration
        q.offerType shouldBe Some(quotaCalculator.getOfferType(qByGeneration))
      }
    }

    "quotaBySameOffer empty, quotaByUser empty -> use combined from default, byCategory quota (expects byGeneration is empty)" in {
      forAll((ProductQuotaGen, "qByCategory")) { qByCategory =>
        (quotaService.getQuotaBySameOffers _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByUserType _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByCategory _)
          .expects(*)
          .returningZ(Some(qByCategory))

        (quotaService.getQuotaByGeneration _)
          .expects(*)
          .returningZ(None)

        (quotaService.reduceQuotaByUsedFreeOffers _)
          .expects(*, *, *)
          .onCall((quota, _, _) => ZIO.succeed(quota))
          .anyNumberOfTimes()

        val q = quotaCalculator
          .placementQuota(enrichedOffer, userSellerType)
          .success
          .value

        q.size shouldBe qByCategory.size
        q.reason shouldBe qByCategory.reason.orElse {
          Option.when(q.size == 0)(PaymentReasons.FreeLimit)
        }
        q.baseQuota shouldBe qByCategory.baseQuota
        q.duration shouldBe qByCategory.duration
        q.offerType shouldBe Some(quotaCalculator.getOfferType(qByCategory))
      }
    }

    "quotaBySameOffer empty, quotaByUser empty -> use combined from default, byGeneration quota (expects byCategory is empty)" in {
      forAll((ProductQuotaGen, "qByGeneration")) { qByGeneration =>
        (quotaService.getQuotaBySameOffers _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByUserType _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByCategory _)
          .expects(*)
          .returningZ(None)

        (quotaService.getQuotaByGeneration _)
          .expects(*)
          .returningZ(Some(qByGeneration))

        (quotaService.reduceQuotaByUsedFreeOffers _)
          .expects(*, *, *)
          .onCall((quota, _, _) => ZIO.succeed(quota))
          .anyNumberOfTimes()

        val q = quotaCalculator
          .placementQuota(enrichedOffer, userSellerType)
          .success
          .value

        q.size shouldBe qByGeneration.size
        q.reason shouldBe qByGeneration.reason.orElse {
          Option.when(q.size == 0)(PaymentReasons.FreeLimit)
        }
        q.baseQuota shouldBe Placement.freeLimit
        q.duration shouldBe qByGeneration.duration
        q.offerType shouldBe Some(quotaCalculator.getOfferType(qByGeneration))
      }
    }

    "quotaBySameOffer empty, quotaByUser empty -> use default (expects byCategory, byGeneration is empty)" in {
      (quotaService.getQuotaBySameOffers _)
        .expects(*)
        .returningZ(None)

      (quotaService.getQuotaByUserType _)
        .expects(*)
        .returningZ(None)

      (quotaService.getQuotaByCategory _)
        .expects(*)
        .returningZ(None)

      (quotaService.getQuotaByGeneration _)
        .expects(*)
        .returningZ(None)

      (quotaService.reduceQuotaByUsedFreeOffers _)
        .expects(*, *, *)
        .onCall((quota, _, _) => ZIO.succeed(quota))
        .anyNumberOfTimes()

      val q = quotaCalculator
        .placementQuota(enrichedOffer, userSellerType)
        .success
        .value

      q.size shouldBe Placement.freeLimit
      q.reason shouldBe None
      q.baseQuota shouldBe Placement.freeLimit
      q.duration shouldBe None
      q.offerType shouldBe Some(OfferTypes.Regular)
    }

  }
}
