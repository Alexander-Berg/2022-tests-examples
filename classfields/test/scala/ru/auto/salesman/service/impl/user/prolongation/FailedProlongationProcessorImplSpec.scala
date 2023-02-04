package ru.auto.salesman.service.impl.user.prolongation

import org.joda.time.{DateTime, Period}
import org.scalacheck.Gen
import org.scalacheck.Gen.{listOf, nonEmptyListOf}
import org.scalatest.Inspectors
import ru.auto.salesman.model.user.PaidProduct
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.user.product.Products
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.ProductApplyService
import ru.auto.salesman.service.ProductApplyService.Response.{
  PaymentError,
  UnexpectedError
}
import ru.auto.salesman.service.impl.user.notify.NotificationService
import ru.auto.salesman.service.impl.user.prolongation.FailedProlongationProcessor.ProlongationFailures
import ru.auto.salesman.service.user.UserProductService.ObjectUpdated
import ru.auto.salesman.service.user.{TransactionService, UserProductService}
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}
import ru.auto.salesman.util.AutomatedContext
import ru.yandex.vertis.banker.model.ApiModel.ApiError
import ru.yandex.vertis.banker.model.ApiModel.ApiError.{
  CancellationPaymentError,
  RemoteError
}
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

class FailedProlongationProcessorImplSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators
    with IntegrationPropertyCheckConfig {

  private val productService = mock[UserProductService]
  private val transactionService = mock[TransactionService]
  private val deactivateDeadlinePeriod = Period.minutes(15)

  private val notificationService = mock[NotificationService]

  private val mockDeactivateProduct =
    toMockFunction1(transactionService.deactivate(_: PaidProduct))

  val failedProlongationProcessor = new FailedProlongationProcessor(
    productService,
    transactionService,
    notificationService,
    deactivateDeadlinePeriod
  ) with InstrumentedFailedProlongationProcessor {
    override def ops: OperationalSupport = TestOperationalSupport
  }

  implicit val rc = AutomatedContext("test")

  "process failed results should complete successfully" in {
    forAll(nonEmptyListOf(GoodsBundleGen)) { products =>
      val notAppliedProductWithResultList =
        products.map(product => product -> UnexpectedError(new RuntimeException("Test")))

      failedProlongationProcessor
        .processFailedResults(DateTime.now())(notAppliedProductWithResultList)
        .success
    }

  }

  "call NotificationSender in deactivateAndNotify for placement" in {
    forAll(nonEmptyListOf(concreteProlongableGoodsGen(Set(Placement)))) { placements =>
      inSequence {
        Inspectors.forEvery(placements) { placement =>
          mockDeactivateProduct
            .expects(placement)
            .returningZ(())
          (notificationService
            .notifyProlongationFailed(_: PaidProduct))
            .expects(placement)
            .returningZ(())
        }
      }
      failedProlongationProcessor.deactivateAndNotify(placements).success
    }
  }

  "call NotificationSender after deactivation" in {
    forAll(
      nonEmptyListOf(
        concreteProlongableGoodsGen(
          Products.allOfType[Products.Goods].filterNot(_ == Placement)
        )
      )
    ) { notPlacements =>
      inSequence {
        Inspectors.forEvery(notPlacements) { notPlacement =>
          mockDeactivateProduct
            .expects(notPlacement)
            .returningZ(())
          (notificationService
            .notifyProlongationFailed(_: PaidProduct))
            .expects(notPlacement)
            .returningZ(())
        }
      }
      failedProlongationProcessor.deactivateAndNotify(notPlacements).success
    }
  }

  "not NotificationSender sender if deactivation failed" in {
    forAll(
      nonEmptyListOf(
        concreteProlongableGoodsGen(
          Products.allOfType[Products.Goods].filterNot(_ == Placement)
        )
      )
    ) { notPlacements =>
      inSequence {
        Inspectors.forEvery(notPlacements) { notPlacement =>
          mockDeactivateProduct
            .expects(notPlacement)
            .throwingZ(new Exception("test exception"))
        }
      }
      failedProlongationProcessor.deactivateAndNotify(notPlacements).failed
    }
  }

  "call NotificationSender after turning prolongation off, for placement" in {
    forAll(listOf(concreteProlongableGoodsGen(Set(Placement)))) { placements =>
      Inspectors.forAll(placements) { placement =>
        inSequence {

          (productService.setProlongable _)
            .expects(*)
            .returningZ(ObjectUpdated)

          (notificationService.notifyProlongationFailed _)
            .expects(*)
            .returningZ(())
        }
      }
      Inspectors.forEvery(placements) { product =>
        failedProlongationProcessor
          .turnProlongationOffAndNotify(product)
          .success
      }
    }
  }

  "call NotificationSender after turning prolongation off, for product" in {
    forAll(
      listOf(
        concreteProlongableGoodsGen(
          Products.allOfType[Products.Goods].filterNot(_ == Placement)
        )
      ),
      listOf(ProlongableGoodsBundleGen),
      ActiveOfferGen
    ) { (goods, bundles, offer) =>
      val products = goods ++ bundles
      Inspectors.forEvery(products) { product =>
        inSequence {
          (productService.setProlongable _)
            .expects(*)
            .returningZ(ObjectUpdated)

          (notificationService.notifyProlongationFailed _)
            .expects(product)
            .returningZ(())
        }
      }

      Inspectors.forEvery(products) { product =>
        failedProlongationProcessor
          .turnProlongationOffAndNotify(product)
          .success
      }
    }
  }

  "not call NotificationSender if turning prolongation off failed" in {
    forAll(
      listOf(
        concreteProlongableGoodsGen(
          Products.allOfType[Products.Goods].filterNot(_ == Placement)
        )
      ),
      listOf(ProlongableGoodsBundleGen)
    ) { (goods, bundles) =>
      val products = goods ++ bundles
      Inspectors.forEvery(products) { _ =>
        (productService.setProlongable _)
          .expects(*)
          .throwingZ(new Exception("test exception"))

        (notificationService.notifyProlongationFailed _)
          .expects(*)
          .returningZ(())
      }

      Inspectors.forEvery(products) { product =>
        failedProlongationProcessor
          .turnProlongationOffAndNotify(product)
          .failure
      }
    }
  }

  val apiErrorNotEnoughFunds = ApiError
    .newBuilder()
    .setCardError(ApiError.RecurrentPaymentError.CARD_HAS_NO_ENOUGH_FUNDS)
    .build
  val paymentErrorNotEnoughFunds = PaymentError(List(apiErrorNotEnoughFunds))

  "split by failure result have not empty notEnoughFunds list" in {
    forAll(GoodsGen) { good =>
      val ProlongationFailures(notEnoughFunds, notApplied) =
        failedProlongationProcessor
          .splitByFailureTypes(List(good).zip(List(paymentErrorNotEnoughFunds)))
      notEnoughFunds.size shouldBe 1
      notApplied.size shouldBe 0
    }
  }

  val remoteErrorGen: Gen[ApiError] = Gen
    .oneOf(RemoteError.values().filterNot(_ == RemoteError.UNRECOGNIZED))
    .map { err =>
      ApiError.newBuilder().setRemoteError(err).build
    }

  val cancellationPaymentErrorGen: Gen[ApiError] = Gen
    .oneOf(
      CancellationPaymentError
        .values()
        .filterNot(_ == CancellationPaymentError.UNRECOGNIZED)
    )
    .map { err =>
      ApiError.newBuilder().setPaymentCancellationError(err).build
    }

  val apiErrorGen: Gen[ApiError] =
    Gen.oneOf(remoteErrorGen, cancellationPaymentErrorGen)

  val paymentErrorGen: Gen[ProductApplyService.Response] = apiErrorGen.map { err =>
    PaymentError(List(err))
  }

  "split by failure result have not empty expectedNotApplied list" in {
    forAll(GoodsGen, paymentErrorGen) { (good, paymentError) =>
      val ProlongationFailures(notEnoughFunds, notApplied) =
        failedProlongationProcessor.splitByFailureTypes(
          List(good).zip(List(paymentError))
        )
      notEnoughFunds.size shouldBe 0
      notApplied.size shouldBe 1
    }
  }

  val unexpectedError = UnexpectedError(new Exception("unexpected exception"))

  "filter UnexpectedErrors" in {
    forAll(GoodsGen) { good =>
      val ProlongationFailures(notEnoughFunds, notApplied) =
        failedProlongationProcessor.splitByFailureTypes(
          List(good).zip(List(unexpectedError))
        )
      notEnoughFunds.size shouldBe 0
      notApplied.size shouldBe 0
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
