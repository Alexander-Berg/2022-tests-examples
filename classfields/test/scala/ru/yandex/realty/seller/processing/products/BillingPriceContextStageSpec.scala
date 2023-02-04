package ru.yandex.realty.seller.processing.products

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.persistence.PartnerId
import ru.yandex.realty.seller.dao.TariffDao
import ru.yandex.realty.seller.model.ProductType
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.{BillingContext, PurchasedProduct, PurchasedProductStatuses}
import ru.yandex.realty.seller.service.{CampaignHeaderService, PriceService}
import ru.yandex.realty.seller.service.PriceService.GetContextRequest
import ru.yandex.realty.vos.model.user.{ExtendedUserType, User}
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future
import scala.util.control.NoStackTrace

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class BillingPriceContextStageSpec extends AsyncSpecBase with SellerModelGenerators with PropertyChecks {

  private val priceService = mock[PriceService]
  private val vosClient = mock[VosClientNG]
  private val stage = new BillingPriceContextStage(priceService, vosClient)
  implicit val traced: Traced = Traced.empty

  private val productWithStartTimeInPast: Gen[PurchasedProduct] =
    for {
      p <- purchasedProductGen
      startTime <- dateTimeInPast
    } yield p.copy(startTime = Some(startTime))

  private val suitableProductGen = for {
    p <- productWithStartTimeInPast
  } yield p.copy(
    status = PurchasedProductStatuses.Pending,
    context = p.context.copy(paymentType = Some(PaymentType.JURIDICAL_PERSON)),
    priceContext = None,
    billingContext = Some(BillingContext(None, None, None, None, None))
  )

  "BillingPriceContextStageSpec" should {
    "revisit product in future" in {
      forAll(suitableProductGen, instantInFuture.map(_.toDateTime)) { (p, startTime) =>
        val state = ProcessingState(p.copy(startTime = Some(startTime.toDateTime)))
        stage.process(state).futureValue.entry.visitTime shouldBe Some(startTime)
      }
    }

    "do nothing for non-pending products" in {
      forAll(productWithStartTimeInPast.filter(_.status != PurchasedProductStatuses.Pending)) { product =>
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }

    "do nothing for products of non-juridical" in {
      forAll(
        productWithStartTimeInPast
          .filter(!_.context.paymentType.contains(PaymentType.JURIDICAL_PERSON))
      ) { product =>
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }

    "do nothing if price context with billing context already present" in {
      forAll(productWithStartTimeInPast, priceContextGen, billingContextGen) { (product, pc, bc) =>
        val state = ProcessingState(
          product.copy(priceContext = Some(pc), billingContext = Some(bc))
        )
        stage.process(state).futureValue shouldBe state
      }
    }

    "set correct price context" in {
      forAll(suitableProductGen, contextsGen) { (product, contexts) =>
        (vosClient
          .getUser(_: String, _: Boolean, _: Iterable[User.Feature])(_: Traced))
          .expects(*, *, *, *)
          .returning(Future.successful(Some(User.newBuilder().build())))

        (priceService
          .getContexts(
            _: UserRef,
            _: Iterable[GetContextRequest],
            _: Boolean,
            _: Option[ExtendedUserType],
            _: Boolean
          )(_: Traced))
          .expects(product.owner, Seq(GetContextRequest(product.target, product.product)), *, *, true, *)
          .returning(Future.successful(Seq(contexts)))

        val state = ProcessingState(product)
        val result = stage.process(state).futureValue

        result.entry.priceContext shouldBe Some(contexts.priceContext)
        result.entry.billingContext shouldBe contexts.billingContext
      }
    }

    "reschedule on fail" in {
      val product = suitableProductGen.next
      (vosClient
        .getUser(_: String, _: Boolean, _: Iterable[User.Feature])(_: Traced))
        .expects(*, *, *, *)
        .returning(Future.successful(Some(User.newBuilder().build())))

      (priceService
        .getContexts(
          _: UserRef,
          _: Iterable[GetContextRequest],
          _: Boolean,
          _: Option[ExtendedUserType],
          _: Boolean
        )(_: Traced))
        .expects(product.owner, Seq(GetContextRequest(product.target, product.product)), *, *, true, *)
        .returning(Future.failed(new RuntimeException("artificial") with NoStackTrace))
      val state = ProcessingState(product)

      val result = stage.process(state).futureValue
      result.entry.copy(visitTime = product.visitTime) shouldBe product
      result.entry.visitTime.nonEmpty shouldBe true
    }
  }
}
