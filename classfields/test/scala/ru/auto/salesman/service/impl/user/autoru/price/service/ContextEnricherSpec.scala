package ru.auto.salesman.service.impl.user.autoru.price.service

import cats.data.NonEmptySet
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Category.CARS
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.model.user.product.{AutoruProduct, ProductProvider, Products}
import ru.auto.salesman.model.user.{Experiment, Experiments}
import ru.auto.salesman.model.{
  AutoruUser,
  DeprecatedDomain,
  DeprecatedDomains,
  Master,
  RegionId,
  Slave
}
import ru.auto.salesman.service.geoservice.RegionService
import ru.auto.salesman.service.impl.user.NoProductPriceInfoException
import ru.auto.salesman.service.user.PriceService.UserContext
import ru.auto.salesman.service.user.UserFeatureService
import ru.auto.salesman.service.user.autoru.price.service.QuotaLeftCalculator
import ru.auto.salesman.service.user.autoru.price.service.sale.UserPeriodicalDiscountService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators

class ContextEnricherSpec extends BaseSpec with ServiceModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  private val vosClient = mock[VosClient]
  private val regionService = mock[RegionService]
  private val quotaLeftCalculator = mock[QuotaLeftCalculator]
  private val periodicalDiscountService = mock[UserPeriodicalDiscountService]
  private val featureService = mock[UserFeatureService]

  (quotaLeftCalculator
    .calculate(
      _: List[AutoruProduct],
      _: Option[AutoruUser]
    ))
    .expects(*, *)
    .returningZ(Map())
    .anyNumberOfTimes()

  val contextEnricher = new ContextEnricherImpl(
    vosClient,
    regionService,
    quotaLeftCalculator,
    periodicalDiscountService,
    featureService
  )

  def genOffer(): ApiOfferModel.Offer = offerGen(offerCategoryGen = CARS).next

  def genOfferOpt(): Option[ApiOfferModel.Offer] = Gen.some(genOffer()).next

  "ContextEnricher" should {
    "don't go to vos when provided offer in context, even with offer id" in {
      forAll(genOffer(), userPeriodicalDiscountGen()) { (offer, discount) =>
        (regionService
          .expandGeoIds(_: RegionId))
          .expects(*)
          .returningZ(List())

        (vosClient.getOptOffer _).expects(*, *).never()

        (periodicalDiscountService
          .getActiveDiscountFor(_: Option[AutoruUser]))
          .expects(*)
          .returningZ(discount)

        val context =
          priceRequestContextGen.suchThat(_.offerId.isDefined).next

        val enrichedContext = context.copy(autoruOffer = Some(offer))

        val user = AutoruUserGen.next
        val userContext = PriceServiceUserContextGen.next

        contextEnricher
          .enrichContext(
            List(UserProductGen.next),
            enrichedContext,
            Some(user),
            Some(userContext)
          )
          .success

      }
    }

    "don't go to vos when no offer in context and no offer id" in {
      forAll(
        priceRequestContextGen,
        PriceServiceUserContextGen,
        userPeriodicalDiscountGen()
      ) { (priceRequestContextGenerated, userContext, discount) =>
        val context = priceRequestContextGenerated.copy(offerId = None)
        val user = AutoruUserGen.next

        (periodicalDiscountService
          .getActiveDiscountFor(_: Option[AutoruUser]))
          .expects(*)
          .returningZ(discount)

        (vosClient.getOptOffer _).expects(*, *).never()

        contextEnricher
          .enrichContext(
            List(UserProductGen.next),
            context,
            Some(user),
            Some(userContext)
          )
          .success
      }
    }

    "go to vos when offer not provided, but has offer id" in {
      (vosClient.getOptOffer _).expects(*, Slave).returningZ(genOfferOpt())

      (featureService.requestPriceFromVosMasterEnabled _).expects().never()

      (regionService
        .expandGeoIds(_: RegionId))
        .expects(*)
        .returningZ(List())

      (periodicalDiscountService
        .getActiveDiscountFor(_: Option[AutoruUser]))
        .expects(*)
        .returningZ(userPeriodicalDiscountGen().next)

      val context = priceRequestContextGen.suchThat(_.offerId.isDefined).next

      val userContext = PriceServiceUserContextGen.next
      val user = AutoruUserGen.next

      contextEnricher
        .enrichContext(
          List(UserProductGen.next),
          context,
          Some(user),
          Some(userContext)
        )
        .success
    }

    "go to vos slave when offer not provided and retry with master on 404 if feature is enabled" in {
      (periodicalDiscountService
        .getActiveDiscountFor(_: Option[AutoruUser]))
        .expects(*)
        .returningZ(userPeriodicalDiscountGen().next)

      (vosClient.getOptOffer _).expects(*, Slave).returningZ(None)

      (vosClient.getOptOffer _).expects(*, Master).returningZ(genOfferOpt())

      (featureService.requestPriceFromVosMasterEnabled _)
        .expects()
        .returning(true)

      (regionService
        .expandGeoIds(_: RegionId))
        .expects(*)
        .returningZ(List())

      val context = priceRequestContextGen.suchThat(_.offerId.isDefined).next

      val userContext = PriceServiceUserContextGen.next
      val user = AutoruUserGen.next

      contextEnricher
        .enrichContext(
          List(UserProductGen.next),
          context,
          Some(user),
          Some(userContext)
        )
        .success
    }

    "go to vos slave when offer not provided and not retry with master if feature is disabled" in {
      (vosClient.getOptOffer _).expects(*, Slave).returningZ(None)

      (vosClient.getOptOffer _).expects(*, Master).never()

      (featureService.requestPriceFromVosMasterEnabled _)
        .expects()
        .returning(false)

      val context = priceRequestContextGen.suchThat(_.offerId.isDefined).next

      val userContext = PriceServiceUserContextGen.next
      val user = AutoruUserGen.next

      contextEnricher
        .enrichContext(
          List(UserProductGen.next),
          context,
          Some(user),
          Some(userContext)
        )
        .failure
    }
  }

  "enrichContext experiments" should {
    "include userExperiments from userContext and load allRegions from database" in {
      forAll(UserProductGen, AutoruUserGen, userPeriodicalDiscountGen()) {
        (product, userRef, discount) =>
          (periodicalDiscountService
            .getActiveDiscountFor(_: Option[AutoruUser]))
            .expects(*)
            .returningZ(discount)

          val context =
            priceRequestContextGen
              .suchThat(_.offerId.isDefined)
              .next
              .copy(autoruOffer = Some(genOffer()))

          val allUserExperiments = Some(
            Experiments(
              "boxes",
              List(
                Experiment(
                  "experiment-1",
                  geoIds = Some(NonEmptySet.one(RegionId(22))),
                  experimentProducts =
                    Some(NonEmptySet.one(ProductProvider.AutoruGoods.Placement))
                ),
                Experiment(
                  "experiment-2",
                  geoIds = Some(NonEmptySet.one(RegionId(77))),
                  experimentProducts =
                    Some(NonEmptySet.one(ProductProvider.AutoruGoods.Badge))
                ),
                Experiment(
                  "experiment-3",
                  geoIds = Some(NonEmptySet.one(RegionId(2))),
                  experimentProducts =
                    Some(NonEmptySet.one(Products.withName[AutoruProduct](product.name)))
                )
              )
            )
          )

          val userContext: UserContext = PriceServiceUserContextGen.next.copy(
            geoIds = List(RegionId(1)),
            allUserExperiments = allUserExperiments
          )

          (regionService
            .expandGeoIds(_: RegionId))
            .expects(*)
            .returningZ(List(RegionId(2)))

          val result = contextEnricher
            .enrichContext(
              List(product),
              context,
              Some(userRef),
              Some(userContext)
            )
            .success
            .value

          result.allUserExperiments shouldBe allUserExperiments
          result.goodsPriceRequest.get.context.experiment shouldBe ""
      }
    }

    "return default price in enriched offer part if there is no priceInfo in the offer, and offer is draft" in {
      forAll(genOffer(), userPeriodicalDiscountGen()) { (offer, discount) =>
        val offerWithoutPriceInfo =
          offer.toBuilder.clearPriceInfo.setStatus(OfferStatus.DRAFT).build

        (periodicalDiscountService
          .getActiveDiscountFor(_: Option[AutoruUser]))
          .expects(*)
          .returningZ(discount)

        (regionService
          .expandGeoIds(_: RegionId))
          .expects(*)
          .returningZ(List())

        (vosClient.getOptOffer _).expects(*, *).never()

        val context =
          priceRequestContextGen.suchThat(_.offerId.isDefined).next

        val enrichedContext =
          context.copy(autoruOffer = Some(offerWithoutPriceInfo))

        val userContext = PriceServiceUserContextGen.next
        val user = AutoruUserGen.next

        contextEnricher
          .enrichContext(
            List(UserProductGen.next),
            enrichedContext,
            Some(user),
            Some(userContext)
          )
          .success
          .value
          .offer
          .get
          .price shouldBe 100

      }
    }

    "fail if there is no priceInfo and it is offer, not draft" in {
      forAll(genOffer(), OfferStatusGen.filter(_ != OfferStatus.DRAFT)) {
        (offer, status) =>
          val offerWithoutPriceInfo =
            offer.toBuilder.clearPriceInfo.setStatus(status).build

          (regionService
            .expandGeoIds(_: RegionId))
            .expects(*)
            .returningZ(List())

          val context =
            priceRequestContextGen.suchThat(_.offerId.isDefined).next

          val enrichedContext =
            context.copy(autoruOffer = Some(offerWithoutPriceInfo))

          val userContext = PriceServiceUserContextGen.next
          val user = AutoruUserGen.next

          contextEnricher
            .enrichContext(
              List(UserProductGen.next),
              enrichedContext,
              Some(user),
              Some(userContext)
            )
            .failure
            .exception shouldBe an[NoProductPriceInfoException]
      }
    }

  }

}
