package ru.yandex.realty.seller.processing.products

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.promocoder.PromocoderAsyncClient
import ru.yandex.realty.promocoder.model.PromocoderGenerators
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product._
import ru.yandex.realty.seller.service.impl.PromocoderManagerImpl
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future
import scala.util.control.NoStackTrace

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class PromoFeaturesDecrementStageSpec
  extends AsyncSpecBase
  with SellerModelGenerators
  with PropertyChecks
  with PromocoderGenerators {

  private val promocoderClient: PromocoderAsyncClient = mock[PromocoderAsyncClient]
  private val promocoderManager = new PromocoderManagerImpl(promocoderClient)
  private val stage = new PromoFeaturesDecrementStage(promocoderManager)
  implicit val traced: Traced = Traced.empty

  private val suitableProductGen = for {
    p <- purchasedProductGen
    pc <- priceContextGen
  } yield p.copy(
    status = PurchasedProductStatuses.Active,
    context = p.context.copy(paymentType = Some(PaymentType.JURIDICAL_PERSON)),
    deliveryStatus = PurchaseProductDeliveryStatuses.Delivered,
    priceContext = Some(pc)
  )

  "do nothing if product is not suitable" in {
    forAll(purchasedProductGen) { product =>
      whenever(
        product.context.paymentType.contains(PaymentType.JURIDICAL_PERSON) &&
          product.status != PurchasedProductStatuses.Active ||
          product.context.paymentType.contains(PaymentType.NATURAL_PERSON) &&
            product.status != PurchasedProductStatuses.Active && product.status != PurchasedProductStatuses.Pending ||
          !product.priceContext.exists(_.modifiers.nonEmpty) ||
          product.deliveryStatus != PurchaseProductDeliveryStatuses.Delivered
      ) {
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }
  }

  "decrement features" in {
    forAll(
      suitableProductGen,
      listUnique(1, 5, promocodeFeatureModifierGen)(_.featureId),
      decrementFeaturesResponseGen()
    ) { (p, modifiers, response) =>
      val priceContext = p.priceContext.map(_.copy(modifiers = modifiers))
      val product = p.copy(priceContext = priceContext)

      modifiers.foreach { m =>
        (promocoderClient
          .decrementFeature(_: String, _: Long, _: String)(_: Traced))
          .expects(m.featureId, getCount(m), product.id, *)
          .returning(Future.successful(response))
      }

      val state = ProcessingState(product)
      stage.process(state).futureValue shouldBe state
    }
  }

  "reschedule on fail" in {
    forAll(suitableProductGen, promocodeFeatureModifierGen) { (p, modifier) =>
      val priceContext = p.priceContext.map(_.copy(modifiers = List(modifier)))
      val product = p.copy(priceContext = priceContext)

      (promocoderClient
        .decrementFeature(_: String, _: Long, _: String)(_: Traced))
        .expects(modifier.featureId, getCount(modifier), product.id, *)
        .returning(Future.failed(new RuntimeException("artificial") with NoStackTrace))

      val state = ProcessingState(product)
      val result = stage.process(state).futureValue
      result.entry.copy(visitTime = product.visitTime) shouldBe product
      result.entry.visitTime.nonEmpty shouldBe true
    }
  }

  private def getCount(modifier: PromocodePriceModifier): Int = modifier match {
    case VasPromocodePriceModifier(featureId, productType, count) => count
    case MoneyPromocodePriceModifier(featureId, sum) => sum
    case _ => throw new IllegalArgumentException("unsupported modifier")
  }
}
