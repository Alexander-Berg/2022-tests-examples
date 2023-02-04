package ru.auto.salesman.service.impl.user.autoru.price.service

import org.scalacheck.Gen
import ru.auto.salesman.model.user.periodical_discount_exclusion.User.NoActiveDiscount
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  RegionId,
  UserSellerType
}
import ru.auto.salesman.service.user.PriceService.{
  EnrichedPriceRequestContext,
  Prolongation
}
import ru.auto.salesman.service.ProlongableExtractor
import ru.auto.salesman.test.model.gens.user.{
  BunkerModelGenerator,
  ServiceModelGenerators
}
import ru.auto.salesman.test.{BaseSpec, TestException}

class ProlongationExtractorSpec
    extends BaseSpec
    with ServiceModelGenerators
    with BunkerModelGenerator {

  private val prolongableExtractor = mock[ProlongableExtractor]

  private val prolongationExtractor = new ProlongationExtractorImpl(
    prolongableExtractor
  )

  private val priceRequestContext = priceRequestContextGen.next

  private val someEnrichedContext = EnrichedPriceRequestContext(
    source = priceRequestContext,
    contextType = None,
    user = None,
    userType = UserSellerType.Usual,
    offer = None,
    geoId = List(RegionId(1)),
    contentQuality = None,
    category = None,
    section = None,
    allUserExperiments = None,
    goodsPriceRequest = None,
    userPeriodicalDiscount = NoActiveDiscount,
    promocoderFeatures = Map(),
    applyMoneyFeature = false,
    applyProlongInterval = true,
    bonusMoneyFeature = List.empty,
    quotaLeft = Map(),
    userContextOpt = None
  )

  import prolongationExtractor.extractProlongation

  "extractProlongation" should {
    "return expected Prolongation model" in {
      forAll(
        UserProductGen,
        ProlongableGen,
        ProlongableGen,
        ProlongableGen,
        Gen.option(userTariffGen())
      ) {
        (
            product,
            prolongationAllowed,
            prolongationForced,
            prolongationForcedNotTogglable,
            tariff
        ) =>
          (prolongableExtractor.prolongationAllowed _)
            .expects(*, *, *, *)
            .returningZ(prolongationAllowed)

          (prolongableExtractor.prolongationForced _)
            .expects(*, *)
            .returningZ(prolongationForced)

          (prolongableExtractor.prolongationForcedNotTogglable _)
            .expects(*, *, *)
            .returningZ(prolongationForcedNotTogglable)

          val expected = Prolongation(
            prolongationAllowed,
            prolongationForced,
            prolongationForcedNotTogglable
          )
          extractProlongation(
            product,
            tariff,
            someEnrichedContext
          ).success.value shouldBe expected
      }
    }

    "fail on prolongationAllowed fetch error" in {
      forAll(UserProductGen, ProlongableGen, Gen.option(userTariffGen())) {
        (product, prolongationForced, tariff) =>
          (prolongableExtractor.prolongationAllowed _)
            .expects(*, *, *, *)
            .throwingZ(new TestException)

          (prolongableExtractor.prolongationForced _)
            .expects(*, *)
            .returningZ(prolongationForced)
            .never

          val result =
            extractProlongation(product, tariff, someEnrichedContext)
          result.failure.exception shouldBe an[TestException]
      }
    }

    "fail on prolongationForced fetch error" in {
      forAll(UserProductGen, ProlongableGen, Gen.option(userTariffGen())) {
        (product, prolongationAllowed, tariff) =>
          (prolongableExtractor.prolongationAllowed _)
            .expects(*, *, *, *)
            .returningZ(prolongationAllowed)

          (prolongableExtractor.prolongationForced _)
            .expects(*, *)
            .throwingZ(new TestException)

          val result =
            extractProlongation(product, tariff, someEnrichedContext)
          result.failure.exception shouldBe an[TestException]
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
