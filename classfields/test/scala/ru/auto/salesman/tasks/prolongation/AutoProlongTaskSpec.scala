package ru.auto.salesman.tasks.prolongation

import com.github.nscala_time.time.Imports._
import org.scalacheck.Gen
import org.scalacheck.Gen.listOf
import org.scalatest.Inspectors
import ru.auto.salesman.dao.user.BundleDao.{Filter => BundleFilter}
import ru.auto.salesman.dao.user.GoodsDao.{Filter => GoodsFilter}
import ru.auto.salesman.dao.user.SubscriptionDao.{Filter => SubscriptionFilter}
import ru.auto.salesman.environment
import ru.auto.salesman.model.RetryCount
import ru.auto.salesman.model.user._
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Boost
import ru.auto.salesman.model.user.product.ProductSource.AutoProlong
import ru.auto.salesman.service.ProductApplyService
import ru.auto.salesman.service.ProductApplyService.Request
import ru.auto.salesman.service.ProductApplyService.Response.{
  Applied,
  NotAvailableLinkedCardsAndFunds,
  PaymentError
}
import ru.auto.salesman.service.impl.user.AutoProlongPriceCalculator
import ru.auto.salesman.service.impl.user.prolongation.FailedProlongationProcessor
import ru.auto.salesman.service.user.{BundleService, GoodsService, SubscriptionService}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.RequestContext
import ru.yandex.vertis.banker.model.ApiModel.ApiError
import ru.yandex.vertis.banker.model.ApiModel.ApiError.{
  RecurrentPaymentError,
  RemoteError
}

import scala.language.existentials

trait AutoProlongTaskSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators {

  private val goodsService = mock[GoodsService]
  private val bundleService = mock[BundleService]
  private val subscriptionService = mock[SubscriptionService]
  private val productApplyService = mock[ProductApplyService]
  private val autoProlongPriceCalculator = mock[AutoProlongPriceCalculator]
  private val maxRetries = RetryCount(2)
  private val maxDeadlinePeriod = 60.minutes
  private val now = environment.now()
  private val failedAction = mock[FailedProlongationProcessor]

  private val task =
    new AutoProlongTask(
      goodsService,
      bundleService,
      subscriptionService,
      autoProlongPriceCalculator: AutoProlongPriceCalculator,
      productApplyService,
      failedAction,
      maxRetries,
      maxDeadlinePeriod
    )

  "Autoprolong task" should {

    "succeed" in {
      forAll(
        listOf(ProlongableGoodsGen),
        listOf(ProlongableGoodsBundleGen),
        ProductPriceGen
      ) { (goods, bundles, productPrice) =>
        mockGetProducts(goods, bundles)

        Inspectors.forEvery(goods ++ bundles) { product =>
          (autoProlongPriceCalculator
            .calculate(_: PaidProduct))
            .expects(product)
            .returningZ(productPrice)

          (productApplyService
            .applyProduct(_: Request))
            .expects(
              Request(
                product.bindedProduct,
                productPrice,
                AutoProlong(product.id),
                Some(product.transactionId)
              )
            )
            .returningZ(Applied)
        }
        task.execute(now).success.value shouldBe (())
      }
    }

    "on non-recoverable error: deactivate and succeed" in {
      forAll(
        listOf(ProlongableGoodsGen),
        listOf(ProlongableGoodsBundleGen),
        ProductPriceGen
      ) { (goods, bundles, productPrice) =>
        mockGetProducts(goods, bundles)
        val products = goods ++ bundles

        if (products.nonEmpty)(failedAction
          .processFailedResults(_: DateTime)(
            _: List[(PaidProduct, ProductApplyService.Response)]
          )(_: RequestContext))
          .expects(*, *, *)
          .returningT(())

        Inspectors.forEvery(products) { product =>
          (autoProlongPriceCalculator
            .calculate(_: PaidProduct))
            .expects(product)
            .returningZ(productPrice)

          (productApplyService
            .applyProduct(_: Request))
            .expects(
              Request(
                product.bindedProduct,
                productPrice,
                AutoProlong(product.id),
                Some(product.transactionId)
              )
            )
            .returningZ(NotAvailableLinkedCardsAndFunds)

        }

        task.execute(now).success.value shouldBe (())
      }
    }

    "proceed even with unexpected exception" in {
      forAll(
        ProlongableGoodsGen,
        ProlongableGoodsGen,
        listOf(ProlongableGoodsBundleGen),
        ProductPriceGen
      ) { (good1, good2, bundles, productPrice) =>
        val goods = List(good1, good2)
        mockGetProducts(goods, bundles)
        val products = goods ++ bundles

        if (products.nonEmpty)(failedAction
          .processFailedResults(_: DateTime)(
            _: List[(PaidProduct, ProductApplyService.Response)]
          )(_: RequestContext))
          .expects(*, *, *)
          .returningT(())

        val (head, tail) = products match {
          case h :: t => (h, t)
          case _ => throw new Exception("b")
        }

        (autoProlongPriceCalculator
          .calculate(_: PaidProduct))
          .expects(head)
          .throwingZ(new Exception("Unexpected exception"))

        Inspectors.forEvery(tail) { product =>
          (autoProlongPriceCalculator
            .calculate(_: PaidProduct))
            .expects(product)
            .returningZ(productPrice)

          (productApplyService
            .applyProduct(_: Request))
            .expects(
              Request(
                product.bindedProduct,
                productPrice,
                AutoProlong(product.id),
                Some(product.transactionId)
              )
            )
            .returningZ(NotAvailableLinkedCardsAndFunds)

        }

        task.execute(now).success.value shouldBe (())
      }
    }

    "on CARD_HAS_NO_ENOUGH_FUNDS error turn prolongation off" in {
      forAll(
        listOf(ProlongableGoodsGen),
        listOf(ProlongableGoodsBundleGen),
        ProductPriceGen
      ) { (goods, bundles, productPrice) =>
        mockGetProducts(goods, bundles)
        val products = goods ++ bundles
        val cardHasNoEnoughFunds = PaymentError(
          List(
            ApiError
              .newBuilder()
              .setCardError(RecurrentPaymentError.CARD_HAS_NO_ENOUGH_FUNDS)
              .build()
          )
        )

        if (products.nonEmpty)(failedAction
          .processFailedResults(_: DateTime)(
            _: List[(PaidProduct, ProductApplyService.Response)]
          )(_: RequestContext))
          .expects(*, *, *)
          .returningT(())

        Inspectors.forEvery(products) { product =>
          (autoProlongPriceCalculator
            .calculate(_: PaidProduct))
            .expects(product)
            .returningZ(productPrice)

          (productApplyService
            .applyProduct(_: Request))
            .expects(
              Request(
                product.bindedProduct,
                productPrice,
                AutoProlong(product.id),
                Some(product.transactionId)
              )
            )
            .returningZ(cardHasNoEnoughFunds)

        }
        task.execute(now).success.value shouldBe (())
      }
    }

    "on recoverable error: retry and succeed in case of retry success" in {
      forAll(
        listOf(ProlongableGoodsGen),
        listOf(ProlongableGoodsBundleGen),
        ProductPriceGen
      ) { (goods, bundles, productPrice) =>
        mockGetProducts(goods, bundles)
        val products = goods ++ bundles
        val recoverableError = PaymentError(
          List(ApiError.newBuilder().setRemoteError(RemoteError.IO).build())
        )

        Inspectors.forEvery(products) { product =>
          (autoProlongPriceCalculator
            .calculate(_: PaidProduct))
            .expects(product)
            .returningZ(productPrice)

          (productApplyService
            .applyProduct(_: Request))
            .expects(
              Request(
                product.bindedProduct,
                productPrice,
                AutoProlong(product.id),
                Some(product.transactionId)
              )
            )
            .returningZ(recoverableError)

          (productApplyService
            .applyProduct(_: Request))
            .expects(
              Request(
                product.bindedProduct,
                productPrice,
                AutoProlong(product.id),
                Some(product.transactionId)
              )
            )
            .returningZ(Applied)
        }
        task.execute(now).success.value shouldBe (())
      }
    }

    "not prolong non-prolongable product even if got prolongable=true from db" in {
      forAll(listOf(goodsGen(Gen.oneOf(Seq(Boost)), Prolongable(true)))) { goods =>
        mockGetProducts(goods, Nil)
        task.execute(now).success.value shouldBe (())
      }
    }

  }

  private def mockGetProducts(goods: List[Goods], bundles: List[Bundle]) = {
    (goodsService.get _)
      .expects(
        GoodsFilter.ActiveProlongableDeadlineBefore(now + maxDeadlinePeriod)
      )
      .returningZ(goods)
    (bundleService.get _)
      .expects(
        BundleFilter.ActiveProlongableDeadlineBefore(now + maxDeadlinePeriod)
      )
      .returningZ(bundles)
    (subscriptionService.get _)
      .expects(
        SubscriptionFilter.ActiveProlongableDeadlineBefore(
          now + maxDeadlinePeriod
        )
      )
      .returningZ(List.empty)
  }

}
