package ru.auto.salesman.tasks.credit

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.Inspectors
import ru.auto.salesman.billing.{RequestContext => BillingRequestContext}
import ru.auto.salesman.dao.ProductDao
import ru.auto.salesman.exceptions.CompositeException
import ru.auto.salesman.model.ProductId.NonPromotionProduct
import ru.auto.salesman.model.{
  AutoruDealer,
  CityId,
  DetailedClient,
  Product,
  ProductDuration,
  ProductId,
  ProductTariff,
  RegionId,
  TransactionId
}
import ru.auto.salesman.service.BillingEventProcessor.{
  BillingEventResponse,
  NotEnoughFunds
}
import ru.auto.salesman.service.{
  BillingEventProcessor,
  DetailedClientSource,
  PriceEstimateService,
  PriceExtractor
}
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}
import ru.auto.salesman.tasks.credit.ProductsBillingTaskSpec._
import ru.auto.salesman.test.dao.gens.clientDetailsGen
import ru.auto.salesman.test.model.gens.ProductGenerators
import ru.auto.salesman.util.money.Money.Kopecks
import ru.auto.salesman.util.RequestContext
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.util.Try

class ProductsBillingTaskSpec
    extends BaseSpec
    with ProductGenerators
    with IntegrationPropertyCheckConfig {
  private val clientSource = mock[DetailedClientSource]
  private val productDao = mock[ProductDao]
  private val billingEventProcessor = mock[BillingEventProcessor]
  private val priceEstimator = mock[PriceEstimateService]
  private val priceExtractor = mock[PriceExtractor]

  implicit val bc: BillingRequestContext = BillingRequestContext {
    "credit-application-billing-spec"
  }

  private val task = new ProductsBillingTask(
    clientSource,
    productDao,
    billingEventProcessor,
    priceEstimator
  )

  private val processBillingEvent = toMockFunction8 {
    billingEventProcessor
      .process(
        _: DetailedClient,
        _: NonPromotionProduct,
        _: PriceEstimateService.PriceRequest,
        _: TransactionId,
        _: Option[ProductTariff]
      )(_: BillingEventResponse => Try[_])(
        _: RequestContext,
        _: BillingRequestContext
      )
  }

  val clientIdGen: Gen[Long] = Gen.posNum[Long]
  val clientsDetailsGen: Gen[DetailedClient] = clientDetailsGen(clientIdGen)

  val activeExpiredNotProlongableProductGen: Gen[Product] =
    expiredProductGen(activeProductGen)
      .map(_.copy(prolongable = false))

  val activeExpiredProlongableProductGen: Gen[Product] =
    expiredProductGen(activeProductGen)
      .map(_.copy(prolongable = true))

  def clientProductPairGen(
      idGen: Gen[Long] = clientIdGen,
      productGen: Gen[Product] = basicProductGen()
  ): Gen[(DetailedClient, Product)] =
    for {
      id <- idGen
      client <- clientDetailsGen(id)
      product <- productForDealer(id, productGen)
    } yield (client, product)

  "ProductsBillingTask" when {
    "runs execute()" should {
      "do nothing if there's no pending or expired products" in {

        (productDao.getActiveExpiredProducts _)
          .expects()
          .returningZ(Nil)

        (productDao.getWaitingForPayment _)
          .expects()
          .returningZ(Nil)

        task
          .execute()
          .success
      }

      "deactivate expired products" in {

        forAll(Gen.nonEmptyListOf(activeExpiredNotProlongableProductGen)) {
          testProducts =>
            (productDao.getActiveExpiredProducts _)
              .expects()
              .returningZ(testProducts)

            (productDao.getWaitingForPayment _)
              .expects()
              .returningZ(Nil)

            Inspectors.forEvery(testProducts) { p =>
              (productDao.deactivate _)
                .expects(p.id, "expired")
                .returningZ(unit)
            }

            task
              .execute()
              .success
        }
      }

      "try to deactivate all not-prolongable products and fail if dao.deactivate fails" in {
        forAll(Gen.nonEmptyListOf(activeExpiredNotProlongableProductGen)) {
          testProducts =>
            (productDao.getActiveExpiredProducts _)
              .expects()
              .returningZ(testProducts)

            (productDao.getWaitingForPayment _)
              .expects()
              .returningZ(Nil)

            Inspectors.forEvery(testProducts) { p =>
              (productDao.deactivate _)
                .expects(p.id, "expired")
                .throwingZ(DaoDeactivateException)
            }

            task
              .execute()
              .failure
              .exception shouldBe a[CompositeException]
        }
      }

      "prolong expired prolongable products" in {
        forAll(
          Gen.nonEmptyListOf(
            clientProductPairGen(productGen = activeExpiredProlongableProductGen)
          )
        ) { clientProductPairs =>
          (productDao.getActiveExpiredProducts _)
            .expects()
            .returningZ(clientProductPairs.map(_._2))

          (productDao.getWaitingForPayment _)
            .expects()
            .returningZ(Nil)

          Inspectors.forEvery(clientProductPairs) { case (client, product) =>
            mockResolveClient(client)

            product.productType.right.value.externalProductId match {
              case Right(nonPromotional: NonPromotionProduct) =>
                processBillingEvent
                  .expects(
                    client,
                    nonPromotional,
                    *,
                    *,
                    *,
                    *,
                    *,
                    *
                  )
                  .returningT(TestBillingEventResponse)
              case _ => ()
            }

            (productDao.prolong _)
              .expects(product, *, *)
              .returningZ(unit)
          }

          task
            .execute()
            .success
        }
      }

      "try to prolong all prolongable products and fail if dao.prolong fails" in {
        forAll(
          Gen.nonEmptyListOf(
            clientProductPairGen(productGen = activeExpiredProlongableProductGen)
          )
        ) { clientProductPairs =>
          (productDao.getActiveExpiredProducts _)
            .expects()
            .returningZ(clientProductPairs.map(_._2))

          (productDao.getWaitingForPayment _)
            .expects()
            .returningZ(Nil)

          Inspectors.forEvery(clientProductPairs) { case (client, product) =>
            mockResolveClient(client)

            product.productType.right.value.externalProductId match {
              case Right(nonPromotional: NonPromotionProduct) =>
                processBillingEvent
                  .expects(
                    client,
                    nonPromotional,
                    *,
                    *,
                    *,
                    *,
                    *,
                    *
                  )
                  .returningT(TestBillingEventResponse)
              case _ => ()
            }

            (productDao.prolong _)
              .expects(product, *, *)
              .throwingZ(DaoProlongException)
          }

          task
            .execute()
            .failure
            .exception shouldBe a[CompositeException]
        }
      }

      "complete successfully if BillingEventProcessor.process completes" in {
        forAll(
          Gen.nonEmptyListOf(
            clientProductPairGen(productGen = needPaymentProductGen)
          )
        ) { clientProductPairs =>
          (productDao.getActiveExpiredProducts _)
            .expects()
            .returningZ(Nil)

          (productDao.getWaitingForPayment _)
            .expects()
            .returningZ(clientProductPairs.map(_._2))

          Inspectors.forEvery(clientProductPairs) { case (client, product) =>
            mockHaveActiveProduct(client, product, returning = false)

            mockResolveClient(client)

            product.productType.right.value.externalProductId match {
              case Right(nonPromotional: NonPromotionProduct) =>
                processBillingEvent
                  .expects(
                    client,
                    nonPromotional,
                    *,
                    *,
                    *,
                    *,
                    *,
                    *
                  )
                  .returningT(TestBillingEventResponse)

                (productDao.activate _)
                  .expects(product.id, TestBillingEventResponse.deadline)
                  .returningZ(unit)
              case _ => ()
            }
          }

          task
            .execute()
            .success
        }
      }

      "try to bill all new products and fail if billing request fails" in {
        forAll(
          Gen.nonEmptyListOf(
            clientProductPairGen(productGen = needPaymentProductGen)
          )
        ) { clientProductPairs =>
          (productDao.getActiveExpiredProducts _)
            .expects()
            .returningZ(Nil)

          (productDao.getWaitingForPayment _)
            .expects()
            .returningZ(clientProductPairs.map(_._2))

          Inspectors.forEvery(clientProductPairs) { case (client, product) =>
            mockHaveActiveProduct(client, product, returning = false)

            mockResolveClient(client)

            product.productType.right.value.externalProductId match {
              case Right(nonPromotional: NonPromotionProduct) =>
                processBillingEvent
                  .expects(
                    client,
                    nonPromotional,
                    *,
                    *,
                    *,
                    *,
                    *,
                    *
                  )
                  .throwing(BillingRequestException)

              case _ => ()
            }

          }

          task
            .execute()
            .failure
            .exception shouldBe a[CompositeException]
        }
      }

      "update application status with FAILED if billing request fails with NoRetryBillingEvent" in {
        forAll(
          Gen.nonEmptyListOf(
            clientProductPairGen(productGen = needPaymentProductGen)
          )
        ) { clientProductPairs =>
          (productDao.getActiveExpiredProducts _)
            .expects()
            .returningZ(Nil)

          (productDao.getWaitingForPayment _)
            .expects()
            .returningZ(clientProductPairs.map(_._2))

          Inspectors.forEvery(clientProductPairs) { case (client, product) =>
            mockHaveActiveProduct(client, product, returning = false)

            mockResolveClient(client)

            product.productType.right.value.externalProductId match {
              case Right(nonPromotional: NonPromotionProduct) =>
                processBillingEvent
                  .expects(
                    client,
                    nonPromotional,
                    *,
                    *,
                    *,
                    *,
                    *,
                    *
                  )
                  .throwing(noRetryBillingEvent)

                (productDao.markFailed _)
                  .expects(product.id)
                  .returningZ(unit)

              case _ => ()
            }
          }

          task
            .execute()
            .failure
            .exception shouldBe a[CompositeException]
        }
      }

      "complete successfully without billing if product paid by other product" in {
        forAll(
          Gen.nonEmptyListOf(
            clientProductPairGen(productGen = applicationCreditSingleGen)
          )
        ) { clientProductPairs =>
          (productDao.getActiveExpiredProducts _)
            .expects()
            .returningZ(Nil)

          (productDao.getWaitingForPayment _)
            .expects()
            .returningZ(clientProductPairs.map(_._2))

          Inspectors.forEvery(clientProductPairs) { case (client, product) =>
            mockHaveActiveProduct(client, product, returning = true)

            mockResolveClient(client)

            (priceEstimator.estimate _)
              .expects(*)
              .returningZ(TestPriceResponse)

            val testProductInfo =
              PriceExtractor.ProductInfo(
                ProductId.SingleCreditApplication,
                Kopecks(777L),
                None,
                Some(ProductDuration.days(1)),
                None,
                appliedExperiment = None,
                policyId = None
              )

            (priceExtractor.productInfo _)
              .expects(ProductId.SingleCreditApplication, *)
              .returningZ(testProductInfo)

            (priceEstimator.extractor _)
              .expects(TestPriceResponse)
              .returning(priceExtractor)

            (productDao.activate _)
              .expects(product.id, *)
              .returningZ(unit)
          }

          task
            .execute()
            .success
        }
      }

      "try to got duration without billing if product paid by other product and fails if priceEstimator fails" in {
        forAll(
          Gen.nonEmptyListOf(
            clientProductPairGen(productGen = applicationCreditSingleGen)
          )
        ) { clientProductPairs =>
          (productDao.getActiveExpiredProducts _)
            .expects()
            .returningZ(Nil)

          (productDao.getWaitingForPayment _)
            .expects()
            .returningZ(clientProductPairs.map(_._2))

          Inspectors.forEvery(clientProductPairs) { case (client, product) =>
            mockHaveActiveProduct(client, product, returning = true)

            mockResolveClient(client)

            (priceEstimator.estimate _)
              .expects(*)
              .throwingZ(new Exception("something goes wrong"))
          }

          task
            .execute()
            .failure
            .exception shouldBe a[CompositeException]
        }
      }

      "bill FullHistoryReport as VinHistory" in {
        forAll(
          Gen.nonEmptyListOf(
            clientProductPairGen(productGen = fullHistoryReportGen)
          )
        ) { clientProductPairs =>
          (productDao.getActiveExpiredProducts _)
            .expects()
            .returningZ(Nil)

          (productDao.getWaitingForPayment _)
            .expects()
            .returningZ(clientProductPairs.map(_._2))

          Inspectors.forEvery(clientProductPairs) { case (client, product) =>
            mockHaveActiveProduct(client, product, returning = false)

            mockResolveClient(client)

            processBillingEvent
              .expects(
                client,
                ProductId.VinHistory,
                *, //<- price request id is too hard to mock here
                *,
                *,
                *,
                *,
                *
              )
              .returningT(TestBillingEventResponse)

            (productDao.activate _)
              .expects(product.id, TestBillingEventResponse.deadline)
              .returningZ(unit)

          }

          task
            .execute()
            .success
        }
      }

    }
  }

  def mockResolveClient(client: DetailedClient): Unit =
    (clientSource.unsafeResolve _)
      .expects(client.clientId, *)
      .returningZ(client)

  def mockHaveActiveProduct(
      client: DetailedClient,
      product: Product,
      returning: Boolean
  ): Unit =
    product.productType match {
      case Right(pt) if pt.canBePaidBy.nonEmpty =>
        (productDao.haveActiveProduct _)
          .expects(AutoruDealer(client.clientId).toString, pt.canBePaidBy.get)
          .returningZ(returning)
      case _ =>
    }
}

object ProductsBillingTaskSpec {

  private val Now = DateTimeUtil.now()

  val TestProductId = 101L
  val TestClientId = 20101L
  val TestClientRegionId: RegionId = RegionId(1L)
  val TestClientCityId: CityId = CityId(2L)

  private val TestBillingEventResponse = BillingEventResponse(
    Now,
    Some(100L),
    Some(100L),
    promocodeFeatures = List.empty,
    Some("hold-id"),
    Some(TestClientId),
    agencyId = None,
    companyId = None,
    regionId = Some(TestClientRegionId)
  )

  val TestPriceResponse = new PriceEstimateService.PriceResponse(
    new Array[Byte](0),
    DateTime.now()
  )

  val noRetryBillingEvent = new NotEnoughFunds("Test wallet is empty")

  case object BillingRequestException extends Exception

  case object DaoDeactivateException extends Exception
  case object DaoProlongException extends Exception
}
