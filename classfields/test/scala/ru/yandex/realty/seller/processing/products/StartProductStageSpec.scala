package ru.yandex.realty.seller.processing.products

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.{
  PurchaseProductDeliveryStatuses,
  PurchasedProduct,
  PurchasedProductStatuses
}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.generators.DateTimeGenerators
import ru.yandex.vertis.protobuf.ProtoInstanceProvider

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class StartProductStageSpec extends AsyncSpecBase with PropertyChecks {
  implicit val traced: Traced = Traced.empty
  import StartProductStageSpec._

  "StartProductStage" should {
    "do nothing" when {
      "product is not pending" in {
        forAll(NotPendingProductGen) { product =>
          val state = ProcessingState(product)
          StartProductStage.process(state).futureValue shouldBe state
        }
      }

      "status is pending, startTime in past and juridical person without hold and non zero effective price request" in {
        forAll(PendingProductGen, priceContextGen.filter(_.effectivePrice > 0)) { (p, pc) =>
          val paymentType = p.context.paymentType
          whenever(paymentType.contains(PaymentType.JURIDICAL_PERSON)) {
            val product = p.copy(priceContext = Some(pc))
            val state = ProcessingState(product)
            StartProductStage.process(state).futureValue shouldBe state
          }
        }
      }
    }

    "update visit time" when {
      "product has start time in future" in {
        forAll(PendingProductWithHoldRequestGen, dateTimeInFuture()) { (p, startTime) =>
          val product = p.copy(startTime = Some(startTime))
          val state = ProcessingState(product)
          val newState = StartProductStage.process(state).futureValue
          newState.entry.visitTime shouldBe product.startTime
        }
      }
    }

    "start product" when {
      "status is pending, startTime in past and natural person" in {
        forAll(PendingProductGen) { product =>
          val paymentType = product.context.paymentType
          whenever(paymentType.isEmpty || paymentType.contains(PaymentType.NATURAL_PERSON)) {
            val state = ProcessingState(product)
            val result = StartProductStage.process(state).futureValue.entry
            result.status shouldBe PurchasedProductStatuses.Active
            result.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.Pending
          }
        }
      }

      "status is pending, startTime in past and juridical person with hold request" in {
        forAll(PendingProductWithHoldRequestGen) { product =>
          val state = ProcessingState(product)
          val result = StartProductStage.process(state).futureValue.entry
          result.status shouldBe PurchasedProductStatuses.Active
          result.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.Pending
        }
      }

      "status is pending, startTime in past and juridical person with zero effective price" in {
        forAll(PendingProductWithZeroEffectivePriceGen) { product =>
          val state = ProcessingState(product)
          val result = StartProductStage.process(state).futureValue.entry
          result.status shouldBe PurchasedProductStatuses.Active
          result.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.Pending
        }
      }
    }
  }
}

object StartProductStageSpec extends SellerModelGenerators with ProtobufMessageGenerators with ProtoInstanceProvider {

  val PendingWithFutureStartProductGen: Gen[PurchasedProduct] = for {
    product <- purchasedProductGen
    startTime <- DateTimeGenerators.dateTimeInFuture()
  } yield product.copy(status = PurchasedProductStatuses.Pending, startTime = Some(startTime))

  val NotPendingProductGen: Gen[PurchasedProduct] = for {
    product <- purchasedProductGen
    status <- purchasedProductStatusGen.filter(_ != PurchasedProductStatuses.Pending)
  } yield product.copy(status = status)

  val PendingProductGen: Gen[PurchasedProduct] = for {
    product <- purchasedProductGen
    startTime <- DateTimeGenerators.dateTimeInPast()
  } yield product.copy(status = PurchasedProductStatuses.Pending, startTime = Some(startTime))

  val JuridicalPendingProductGen: Gen[PurchasedProduct] = for {
    product <- PendingProductGen
  } yield product.copy(context = product.context.copy(paymentType = Some(PaymentType.JURIDICAL_PERSON)))

  val PendingProductWithHoldRequestGen: Gen[PurchasedProduct] = for {
    product <- purchasedProductGen
    startTime <- DateTimeGenerators.dateTimeInPast()
    context = product.context.copy(paymentType = Some(PaymentType.JURIDICAL_PERSON))
    pc <- priceContextGen.filter(_.effectivePrice > 0)
    bc <- billingContextGen
    hr <- generate[Model.HoldRequest]()
  } yield {
    product.copy(
      status = PurchasedProductStatuses.Pending,
      context = context,
      priceContext = Some(pc),
      billingContext = Some(bc.copy(holdRequest = Some(hr))),
      startTime = Some(startTime)
    )
  }

  val PendingProductWithZeroEffectivePriceGen: Gen[PurchasedProduct] = for {
    product <- purchasedProductGen
    startTime <- DateTimeGenerators.dateTimeInPast()
    context = product.context.copy(paymentType = Some(PaymentType.JURIDICAL_PERSON))
    pc <- priceContextGen
  } yield {
    product.copy(
      status = PurchasedProductStatuses.Pending,
      context = context,
      priceContext = Some(pc.copy(effectivePrice = 0)),
      startTime = Some(startTime)
    )
  }
}
