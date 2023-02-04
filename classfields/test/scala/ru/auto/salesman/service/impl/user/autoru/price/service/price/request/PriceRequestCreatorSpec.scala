package ru.auto.salesman.service.impl.user.autoru.price.service.price.request

import cats.data.NonEmptySet
import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.client.VosClient.CountUserOffersQuery
import ru.auto.salesman.client.vin.VinDecoderClient
import ru.auto.salesman.model.user.product.ProductProvider
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions
import ru.auto.salesman.model.user.{Experiment, Experiments}
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  ExperimentId,
  OfferCategories,
  OfferTypes,
  ProductId,
  RegionId,
  UserSellerType
}
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.service.PriceEstimateService.PriceRequest.{
  OffersHistoryReportsContext,
  SubscriptionOffer
}
import ru.auto.salesman.service.impl.user.AutoruPriceService.{
  GoodsPriceRequest,
  OfferHistoryPriceRequest
}
import ru.auto.salesman.service.impl.user.ExperimentSelectServiceImpl
import ru.auto.salesman.service.user.ExperimentSelectService
import ru.auto.salesman.service.user.PriceService.{
  EnrichedPriceRequestContext,
  OfferInfoModel
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.util.AutomatedContext
import ru.auto.salesman.util.PriceRequestContextType.OfferHistory
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.collection.immutable.SortedSet

class PriceRequestCreatorSpec extends BaseSpec with ServiceModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
  implicit val rc = AutomatedContext("test")
  val vosClient: VosClient = mock[VosClient]
  val vinDecoderClient: VinDecoderClient = mock[VinDecoderClient]

  val experimentSelectService: ExperimentSelectService =
    new ExperimentSelectServiceImpl()

  val priceRequestCreator =
    new PriceRequestCreator(
      vosClient,
      vinDecoderClient,
      experimentSelectService
    )

  "preparePrice request" should {
    "throw exception when product is offers history and no offer provided" in {
      forAll(
        EnrichedPriceRequestContextGen,
        enrichedProduct(AutoruSubscriptions.OffersHistoryReports(1))
      ) { (contextGenerated, productGenerated) =>
        val enrichedContext =
          contextGenerated.copy(offer = None)
        val product =
          productGenerated.copy(service = ProductId.OffersHistoryReports)

        val result =
          priceRequestCreator.preparePriceRequest(product, enrichedContext)
        result.failure.exception shouldBe an[IllegalArgumentException]
      }
    }

    "go to vin decoder when no quality" in {
      forAll(
        EnrichedPriceRequestContextGen,
        enrichedProduct(AutoruSubscriptions.OffersHistoryReports(1)),
        EnrichedOfferGen,
        ContentQualityGen
      ) { (contextGenerated, productGenerated, offer, quality) =>
        val enrichedContext =
          contextGenerated.copy(contentQuality = None, offer = Some(offer))

        val product =
          productGenerated.copy(service = ProductId.OffersHistoryReports)

        (vinDecoderClient.getContentQuality _)
          .expects(*, *)
          .returningZ(quality)

        priceRequestCreator
          .preparePriceRequest(product, enrichedContext)
          .success
      }
    }

    "not go to vin decoder when have quality" in {
      forAll(
        EnrichedPriceRequestContextGen,
        enrichedProduct(AutoruSubscriptions.OffersHistoryReports(1)),
        EnrichedOfferGen,
        ContentQualityGen
      ) { (contextGenerated, productGenerated, offer, quality) =>
        val enrichedContext = contextGenerated.copy(
          contentQuality = Some(quality),
          offer = Some(offer)
        )

        val product =
          productGenerated.copy(service = ProductId.OffersHistoryReports)

        (vinDecoderClient.getContentQuality _).expects(*, *).never()

        priceRequestCreator
          .preparePriceRequest(product, enrichedContext)
          .success
      }
    }

    "for placement for usual user, call vos.countUserOffers" in {
      forAll(
        enrichedProduct(Placement).map(
          _.copy(offerType = Some(OfferTypes.Regular))
        ),
        enrichedPriceRequestContext(
          hasOffer = true,
          hasUser = true,
          hasGoodsPriceRequest = true
        )
          .map(_.copy(userType = UserSellerType.Usual)),
        OfferCategoryKnownGen,
        bool
      ) { (placement, contextGenerated, category, is7daysExperiment) =>
        val offer = contextGenerated.offer.get
        val offerWithCategory = offer.copy(
          source = offer.source.toBuilder.setCategory(category).build
        )
        val context = contextGenerated.copy(offer = Some(offerWithCategory))

        (vosClient.countUserOffers _)
          .expects {
            argThat { q: CountUserOffersQuery =>
              // в коде вызывается DateTimeUtil.now().minusDays(60), так что проверка только примерная
              val nowMinusSixtyDays = DateTimeUtil.now().minusDays(60)
              val createDateFrom = q.createDateFrom.get.getMillis
              diffNotMoreThan4Seconds(createDateFrom, nowMinusSixtyDays)
            }
          }
          .returningZ(2)

        priceRequestCreator
          .preparePriceRequest(placement, context)
          .success
      }
    }

    "for placement for usual user and have experiment" in {
      forAll(
        enrichedProduct(Placement).map(
          _.copy(offerType = Some(OfferTypes.Regular))
        ),
        enrichedPriceRequestContext(
          hasGoodsPriceRequest = true
        )
          .map(_.copy(userType = UserSellerType.Usual)),
        OfferCategoryKnownGen
      ) { (placement, contextGenerated, category) =>
        val offer = contextGenerated.offer.get.copy(
          geoId = List(RegionId(42))
        )
        val offerWithCategory = offer.copy(
          source = offer.source.toBuilder.setCategory(category).build
        )

        val experiments = Experiments(
          "boxes",
          List(
            Experiment(
              "offer-experiment-geo",
              geoIds = NonEmptySet.fromSet(SortedSet[RegionId]() ++ offer.geoId),
              experimentProducts = None
            ),
            Experiment(
              "user-experiment",
              geoIds = Some(NonEmptySet.one(RegionId(22))),
              experimentProducts = None
            )
          )
        )
        val context = contextGenerated.copy(
          offer = Some(offerWithCategory),
          allUserExperiments = Some(experiments),
          geoId = List(RegionId(22))
        )

        (vosClient.countUserOffers _)
          .expects {
            argThat { q: CountUserOffersQuery =>
              // в коде вызывается DateTimeUtil.now().minusDays(60), так что проверка только примерная
              val nowMinusSixtyDays = DateTimeUtil.now().minusDays(60)
              val createDateFrom = q.createDateFrom.get.getMillis
              diffNotMoreThan4Seconds(createDateFrom, nowMinusSixtyDays)
            }
          }
          .returningZ(2)

        val result = priceRequestCreator
          .preparePriceRequest(placement, context)
          .success

        result.value.context
          .asInstanceOf[PriceRequest.UserContext]
          .experiment shouldBe "offer-experiment-geo"
      }
    }

    "for placement for reseller, not call vos.countUserOffers" in {
      forAll(
        enrichedProduct(Placement).map(
          _.copy(offerType = Some(OfferTypes.Regular))
        ),
        enrichedPriceRequestContext(
          hasOffer = true,
          hasUser = true,
          hasGoodsPriceRequest = true
        )
          .map(_.copy(userType = UserSellerType.Reseller)),
        OfferCategoryKnownGen,
        bool
      ) { (placement, contextGenerated, category, is7daysExperiment) =>
        val offer = contextGenerated.offer.get
        val offerWithCategory = offer.copy(
          source = offer.source.toBuilder.setCategory(category).build
        )
        val context = contextGenerated.copy(offer = Some(offerWithCategory))

        (vosClient.countUserOffers _).expects(*).returningZ(2).never

        priceRequestCreator
          .preparePriceRequest(placement, context)
          .success
      }
    }

    "for placement for usual user for draft set count into goodsPriceRequest" in {
      forAll(
        enrichedProduct(Placement).map(
          _.copy(offerType = Some(OfferTypes.Regular))
        ),
        enrichedPriceRequestContext(
          hasOffer = true,
          hasUser = true,
          hasGoodsPriceRequest = true
        )
          .map(_.copy(userType = UserSellerType.Usual)),
        OfferCategoryKnownGen,
        bool
      ) { (placement, contextGenerated, category, is7daysExperiment) =>
        val offer = contextGenerated.offer.get
        val offerWithCategory = offer.copy(
          source = offer.source.toBuilder
            .setCategory(category)
            .setStatus(ApiOfferModel.OfferStatus.DRAFT)
            .build
        )
        val context = contextGenerated.copy(offer = Some(offerWithCategory))

        (vosClient.countUserOffers _).expects(*).returningZ(5)

        val res = priceRequestCreator
          .preparePriceRequest(placement, context)
          .success

        res.value match {
          case g: GoodsPriceRequest =>
            g.context.numByModel shouldBe Some(6)
          case _ => throw new Exception("Wrong class in response")
        }
      }
    }

    "for placement for reseller for draft not set count into goodsPriceRequest" in {
      forAll(
        enrichedProduct(Placement).map(
          _.copy(offerType = Some(OfferTypes.Regular))
        ),
        enrichedPriceRequestContext(
          hasOffer = true,
          hasUser = true,
          hasGoodsPriceRequest = true
        )
          .map(_.copy(userType = UserSellerType.Reseller)),
        OfferCategoryKnownGen,
        bool
      ) { (placement, contextGenerated, category, is7daysExperiment) =>
        val offer = contextGenerated.offer.get
        val offerWithCategory = offer.copy(
          source = offer.source.toBuilder
            .setCategory(category)
            .setStatus(ApiOfferModel.OfferStatus.DRAFT)
            .build
        )
        val context = contextGenerated.copy(offer = Some(offerWithCategory))

        (vosClient.countUserOffers _).expects(*).never()

        val res = priceRequestCreator
          .preparePriceRequest(placement, context)
          .success

        res.value match {
          case g: GoodsPriceRequest =>
            g.context.numByModel shouldBe None
          case _ => throw new Exception("Wrong class in response")
        }
      }
    }

    "for placement for usual user for not draft set count into goodsPriceRequest" in {
      forAll(
        enrichedProduct(Placement).map(
          _.copy(offerType = Some(OfferTypes.Regular))
        ),
        enrichedPriceRequestContext(
          hasOffer = true,
          hasUser = true,
          hasGoodsPriceRequest = true
        )
          .map(_.copy(userType = UserSellerType.Usual)),
        OfferCategoryKnownGen,
        OfferStatusNotDraftGen,
        bool
      ) {
        (
            placement,
            contextGenerated,
            category,
            notDraftStatus,
            is7daysExperiment
        ) =>
          val offer = contextGenerated.offer.get
          val offerWithCategory = offer.copy(
            source = offer.source.toBuilder
              .setCategory(category)
              .setStatus(notDraftStatus)
              .build
          )
          val context = contextGenerated.copy(offer = Some(offerWithCategory))

          (vosClient.countUserOffers _)
            .expects {
              argThat { q: CountUserOffersQuery =>
                // в коде вызывается DateTimeUtil.now().minusDays(60), так что проверка только примерная
                val nowMinusSixtyDays = DateTimeUtil.now().minusDays(60)
                val createDateFrom = q.createDateFrom.get.getMillis
                diffNotMoreThan4Seconds(createDateFrom, nowMinusSixtyDays)
              }
            }
            .returningZ(5)

          val res = priceRequestCreator
            .preparePriceRequest(placement, context)
            .success

          res.value match {
            case g: GoodsPriceRequest =>
              g.context.numByModel shouldBe Some(5)
            case _ => throw new Exception("Wrong class in response")
          }
      }
    }

    "for placement with no offerType throw exception" in {
      forAll(
        enrichedProduct(Placement).map(_.copy(offerType = None)),
        enrichedPriceRequestContext(
          hasOffer = true,
          hasUser = true,
          hasGoodsPriceRequest = true
        )
      ) { (placement, context) =>
        priceRequestCreator
          .preparePriceRequest(placement, context)
          .failure
          .exception shouldBe an[IllegalArgumentException]
      }
    }

  }

  "offersHistoryReportsRequest" should {

    "return price request for offers history report with empty contextType" in {
      val enrichedContextGen = EnrichedPriceRequestContextGen.next

      val enrichedOffer = EnrichedOfferGen.next.copy(
        info = OfferInfoModel(
          OfferCategories.Moto,
          Section.NEW,
          mark = Some("AUDI"),
          model = Some("A8"),
          generationId = Some("1234")
        ),
        year = 2017,
        geoId = List(RegionId(1L)),
        price = 10000000L
      )
      val enrichedContext = enrichedContextGen.copy(
        contextType = None,
        category = None,
        section = None,
        contentQuality = None,
        offer = Some(
          enrichedOffer.copy(
            source = ApiOfferModel.Offer
              .newBuilder(enrichedOffer.source)
              .setDocuments(
                ApiOfferModel.Documents
                  .newBuilder(enrichedOffer.source.getDocuments)
                  .setVin("1234")
              )
              .build()
          )
        )
      )

      (vinDecoderClient.getContentQuality _).expects(*, *).returningZ(20)

      val subscriptionOffer = SubscriptionOffer(
        OfferCategories.Cars,
        Section.USED,
        mark = Some("AUDI"),
        model = Some("A8"),
        generation = Some("1234"),
        year = Some(2017),
        geoId = List(RegionId(1L)),
        price = Some(10000000L)
      )

      val offersHistoryReportsContext = OffersHistoryReportsContext(
        reportsCount = 1,
        contextType = None,
        geoId = enrichedContext.geoId.headOption,
        contentQuality = Some(20),
        experiment = buildOfferHistoryExperiment(enrichedContext)
      )

      val expected = OfferHistoryPriceRequest(
        userId = enrichedContext.user,
        userContextOpt = enrichedContext.userContextOpt,
        offer = Some(subscriptionOffer),
        context = offersHistoryReportsContext,
        vinOrLicensePlate = Some("1234"),
        offerId = enrichedContext.offer.map(_.offerId)
      )

      val result = priceRequestCreator
        .offersHistoryReportsRequest(
          AutoruSubscriptions.OffersHistoryReports(1),
          reportsCount = 1,
          enrichedContext
        )
        .success
        .value
      result shouldBe expected
    }

    "return price request for offers history report" in {
      val enrichedContextGen = EnrichedPriceRequestContextGen.next
      val enrichedOffer = EnrichedOfferGen.next.copy(
        info = OfferInfoModel(
          OfferCategories.Moto,
          Section.NEW,
          mark = Some("AUDI"),
          model = Some("A8"),
          generationId = Some("1234")
        ),
        year = 2017,
        geoId = List(RegionId(1L)),
        price = 10000000L
      )

      val enrichedContext = enrichedContextGen.copy(
        contextType = Some(OfferHistory),
        contentQuality = None,
        category = None,
        section = None,
        offer = Some(
          enrichedOffer.copy(
            source = ApiOfferModel.Offer
              .newBuilder(enrichedOffer.source)
              .setDocuments(
                ApiOfferModel.Documents
                  .newBuilder(enrichedOffer.source.getDocuments)
                  .setVin("")
                  .setLicensePlate("dd111")
              )
              .build()
          )
        )
      )

      (vinDecoderClient.getContentQuality _).expects(*, *).returningZ(20)

      val subscriptionOffer = SubscriptionOffer(
        OfferCategories.Cars,
        Section.USED,
        mark = Some("AUDI"),
        model = Some("A8"),
        generation = Some("1234"),
        year = Some(2017),
        geoId = List(RegionId(1L)),
        price = Some(10000000L)
      )
      val offerHistoryContext = OffersHistoryReportsContext(
        reportsCount = 1,
        contextType = Some(OfferHistory),
        geoId = enrichedContext.geoId.headOption,
        contentQuality = Some(20),
        experiment = buildOfferHistoryExperiment(enrichedContext)
      )

      val expected = OfferHistoryPriceRequest(
        userId = enrichedContext.user,
        userContextOpt = enrichedContext.userContextOpt,
        offer = Some(subscriptionOffer),
        context = offerHistoryContext,
        vinOrLicensePlate = Some("dd111"),
        offerId = enrichedContext.offer.map(_.offerId)
      )

      val result = priceRequestCreator
        .offersHistoryReportsRequest(
          AutoruSubscriptions.OffersHistoryReports(1),
          reportsCount = 1,
          enrichedContext
        )
      result.success.value shouldBe expected
    }

    "return price request for offers history report with contentQuality from context" in {
      val enrichedContextGen = EnrichedPriceRequestContextGen.next
      val enrichedOffer = EnrichedOfferGen.next.copy(
        info = OfferInfoModel(
          OfferCategories.Moto,
          Section.NEW,
          mark = Some("AUDI"),
          model = Some("A8"),
          generationId = Some("1234")
        ),
        year = 2017,
        geoId = List(RegionId(1L)),
        price = 10000000L
      )

      val enrichedContext = enrichedContextGen.copy(
        contextType = Some(OfferHistory),
        contentQuality = Some(15),
        category = None,
        section = None,
        offer = Some(
          enrichedOffer.copy(
            source = ApiOfferModel.Offer
              .newBuilder(enrichedOffer.source)
              .setDocuments(
                ApiOfferModel.Documents
                  .newBuilder(enrichedOffer.source.getDocuments)
                  .setVin("0987")
              )
              .build()
          )
        )
      )

      (vinDecoderClient.getContentQuality _).expects(*, *).never()

      val subscriptionOffer = SubscriptionOffer(
        OfferCategories.Cars,
        Section.USED,
        mark = Some("AUDI"),
        model = Some("A8"),
        generation = Some("1234"),
        year = Some(2017),
        geoId = List(RegionId(1L)),
        price = Some(10000000L)
      )
      val offerHistoryReportsContext = OffersHistoryReportsContext(
        reportsCount = 1,
        contextType = Some(OfferHistory),
        geoId = enrichedContext.geoId.headOption,
        contentQuality = Some(15),
        experiment = buildOfferHistoryExperiment(enrichedContext)
      )

      val expected = OfferHistoryPriceRequest(
        userId = enrichedContext.user,
        userContextOpt = enrichedContext.userContextOpt,
        offer = Some(subscriptionOffer),
        context = offerHistoryReportsContext,
        vinOrLicensePlate = Some("0987"),
        offerId = enrichedContext.offer.map(_.offerId)
      )
      val result = priceRequestCreator
        .offersHistoryReportsRequest(
          AutoruSubscriptions.OffersHistoryReports(1),
          reportsCount = 1,
          enrichedContext
        )
      result.success.value shouldBe expected
    }
  }

  private def buildOfferHistoryExperiment(
      enrichedContext: EnrichedPriceRequestContext
  ): Option[ExperimentId] =
    experimentSelectService
      .getExperimentForOffer(
        allUserExperiments = enrichedContext.allUserExperiments,
        autoRuProduct = ProductProvider.AutoruSubscriptions.OffersHistoryReports(1),
        offer = enrichedContext.offer
      )
      .success
      .value
      .flatMap(_.activeExperimentId)

  private def diffNotMoreThan4Seconds(timestamp0: Long, timestamp1: DateTime): Boolean =
    timestamp0 > timestamp1.minusSeconds(2).getMillis &&
    timestamp0 < timestamp1.plusSeconds(2).getMillis

}
