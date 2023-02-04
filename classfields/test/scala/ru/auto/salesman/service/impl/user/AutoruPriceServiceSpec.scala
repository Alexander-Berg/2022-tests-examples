package ru.auto.salesman.service.impl.user

import org.scalatest.{BeforeAndAfter, Inspectors}
import ru.auto.salesman.model._
import ru.auto.salesman.model.user.product.AutoruProduct
import ru.auto.salesman.service.user.PriceService._
import ru.auto.salesman.service.user.autoru.price.service.{
  ContextEnricher,
  ProductPriceCalculator,
  UserContextCollector
}
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.auto.salesman.util.{PriceRequestContext}
import ru.yandex.vertis.scalatest.BetterTryValues

class AutoruPriceServiceSpec
    extends BaseSpec
    with ServiceModelGenerators
    with OfferModelGenerators
    with BetterTryValues
    with BeforeAndAfter {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 3)

  "AutoruPriceService" when {

    "calculatePricesForMultipleOffers" should {
      "success when all called methods successful" in {
        forAll(
          ProductPriceGen,
          UserProductGen,
          PriceRequestContextOffersGen,
          PriceServiceUserContextGen,
          EnrichedPriceRequestContextGen
        ) {
          (
              productPrice,
              product,
              contextOffers,
              userContext,
              enrichedPriceRequestContext
          ) =>
            val userContextCollector: UserContextCollector =
              mock[UserContextCollector]
            val contextEnricher: ContextEnricher = mock[ContextEnricher]
            val productPriceCalculator: ProductPriceCalculator =
              mock[ProductPriceCalculator]

            val autoruPriceService = new AutoruPriceService(
              userContextCollector,
              contextEnricher,
              productPriceCalculator
            )

            (userContextCollector
              .getUserContextOpt(
                _: Option[AutoruUser],
                _: Option[RegionId],
                _: Boolean
              ))
              .expects(*, *, *)
              .returningZ(Some(userContext))

            (contextEnricher
              .enrichContext(
                _: List[AutoruProduct],
                _: PriceRequestContext,
                _: Option[AutoruUser],
                _: Option[UserContext]
              ))
              .expects(*, *, *, *)
              .returningZ(enrichedPriceRequestContext)
              .anyNumberOfTimes()

            (productPriceCalculator
              .calculateProductPrice(
                _: AutoruProduct,
                _: EnrichedPriceRequestContext
              ))
              .expects(*, *)
              .returningZ(productPrice)
              .anyNumberOfTimes()

            val v = autoruPriceService
              .calculatePricesForMultipleOffers(List(product), contextOffers)
              .success
              .value
            v should not be empty
            v.foreach(productPrices =>
              Inspectors.forEvery(productPrices.prices) {
                _ shouldBe productPrice
              }
            )
        }
      }
      "failure when calculateProductPrice method failed" in {
        forAll(
          UserProductGen,
          PriceRequestContextOffersGen,
          PriceServiceUserContextGen,
          EnrichedPriceRequestContextGen
        ) { (product, contextOffers, userContext, enrichedPriceRequestContext) =>
          val userContextCollector: UserContextCollector =
            mock[UserContextCollector]
          val contextEnricher: ContextEnricher = mock[ContextEnricher]
          val productPriceCalculator: ProductPriceCalculator =
            mock[ProductPriceCalculator]

          val autoruPriceService = new AutoruPriceService(
            userContextCollector,
            contextEnricher,
            productPriceCalculator
          )

          (userContextCollector
            .getUserContextOpt(
              _: Option[AutoruUser],
              _: Option[RegionId],
              _: Boolean
            ))
            .expects(*, *, *)
            .returningZ(Some(userContext))

          (contextEnricher
            .enrichContext(
              _: List[AutoruProduct],
              _: PriceRequestContext,
              _: Option[AutoruUser],
              _: Option[UserContext]
            ))
            .expects(*, *, *, *)
            .returningZ(enrichedPriceRequestContext)
            .anyNumberOfTimes()

          val testException = new TestException
          (productPriceCalculator
            .calculateProductPrice(
              _: AutoruProduct,
              _: EnrichedPriceRequestContext
            ))
            .expects(*, *)
            .throwingZ(testException)

          autoruPriceService
            .calculatePricesForMultipleOffers(List(product), contextOffers)
            .failure
            .exception shouldBe testException
        }
      }
      "failure when enrichGeneralContext failed" in {
        forAll(
          UserProductGen,
          PriceRequestContextOffersGen
        ) { (product, contextOffers) =>
          val userContextCollector: UserContextCollector =
            mock[UserContextCollector]
          val contextEnricher: ContextEnricher = mock[ContextEnricher]
          val productPriceCalculator: ProductPriceCalculator =
            mock[ProductPriceCalculator]

          val autoruPriceService = new AutoruPriceService(
            userContextCollector,
            contextEnricher,
            productPriceCalculator
          )

          val testException = new TestException()
          (userContextCollector
            .getUserContextOpt(
              _: Option[AutoruUser],
              _: Option[RegionId],
              _: Boolean
            ))
            .expects(*, *, *)
            .throwingZ(testException)

          (productPriceCalculator
            .calculateProductPrice(
              _: AutoruProduct,
              _: EnrichedPriceRequestContext
            ))
            .expects(*, *)
            .never

          (contextEnricher
            .enrichContext(
              _: List[AutoruProduct],
              _: PriceRequestContext,
              _: Option[AutoruUser],
              _: Option[UserContext]
            ))
            .expects(*, *, *, *)
            .never()

          autoruPriceService
            .calculatePricesForMultipleOffers(List(product), contextOffers)
            .failure
            .exception shouldBe testException
        }
      }
      "failure when enrichContext failed" in {
        forAll(
          UserProductGen,
          PriceRequestContextOffersGen,
          ProductPriceGen,
          PriceServiceUserContextGen
        ) { (product, contextOffers, _, userContext) =>
          val userContextCollector: UserContextCollector =
            mock[UserContextCollector]
          val contextEnricher: ContextEnricher = mock[ContextEnricher]
          val productPriceCalculator: ProductPriceCalculator =
            mock[ProductPriceCalculator]

          val autoruPriceService = new AutoruPriceService(
            userContextCollector,
            contextEnricher,
            productPriceCalculator
          )

          (userContextCollector
            .getUserContextOpt(
              _: Option[AutoruUser],
              _: Option[RegionId],
              _: Boolean
            ))
            .expects(*, *, *)
            .returningZ(Some(userContext))

          val testException = new TestException
          (contextEnricher
            .enrichContext(
              _: List[AutoruProduct],
              _: PriceRequestContext,
              _: Option[AutoruUser],
              _: Option[UserContext]
            ))
            .expects(*, *, *, *)
            .throwingZ(testException)

          (productPriceCalculator
            .calculateProductPrice(
              _: AutoruProduct,
              _: EnrichedPriceRequestContext
            ))
            .expects(*, *)
            .never

          autoruPriceService
            .calculatePricesForMultipleOffers(List(product), contextOffers)
            .failure
            .exception shouldBe testException
        }
      }

      "call enrichContext with right arguments" in {
        forAll(
          UserProductGen,
          PriceRequestContextOffersOneOfferGen,
          ProductPriceGen,
          PriceServiceUserContextGen,
          EnrichedPriceRequestContextGen
        ) {
          (
              product,
              contextOffers,
              productPrice,
              userContext,
              enrichedPriceRequestContext
          ) =>
            val userContextCollector: UserContextCollector =
              mock[UserContextCollector]
            val contextEnricher: ContextEnricher = mock[ContextEnricher]
            val productPriceCalculator: ProductPriceCalculator =
              mock[ProductPriceCalculator]

            val autoruPriceService = new AutoruPriceService(
              userContextCollector,
              contextEnricher,
              productPriceCalculator
            )

            (productPriceCalculator
              .calculateProductPrice(
                _: AutoruProduct,
                _: EnrichedPriceRequestContext
              ))
              .expects(*, *)
              .returningZ(productPrice)

            (userContextCollector
              .getUserContextOpt(
                _: Option[AutoruUser],
                _: Option[RegionId],
                _: Boolean
              ))
              .expects(*, *, *)
              .returningZ(Some(userContext))

            (contextEnricher
              .enrichContext(
                _: List[AutoruProduct],
                _: PriceRequestContext,
                _: Option[AutoruUser],
                _: Option[UserContext]
              ))
              .expects(
                List(product),
                PriceRequestContext(
                  contextType = None,
                  userModerationStatus = None,
                  contextOffers.user,
                  offerId = None,
                  Some(contextOffers.offers.head),
                  category = None,
                  section = None,
                  geoId = None,
                  vin = None,
                  vinReportParams = None,
                  licensePlate = None,
                  contentQuality = None,
                  contextOffers.applyMoneyFeature,
                  contextOffers.applyProlongInterval
                ),
                contextOffers.user,
                Some(userContext)
              )
              .returningZ(enrichedPriceRequestContext)

            autoruPriceService
              .calculatePricesForMultipleOffers(List(product), contextOffers)
              .success
              .value
              .map(_.prices.map(_.price shouldBe productPrice.price))
        }
      }
    }
  }
}
