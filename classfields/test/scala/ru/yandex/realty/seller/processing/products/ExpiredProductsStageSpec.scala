package ru.yandex.realty.seller.processing.products

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product._
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.generators.DateTimeGenerators
import ru.yandex.realty.seller.watchers.SellerProcessingState
import ru.yandex.realty.tracing.Traced

import scala.concurrent.duration._

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class ExpiredProductsStageSpec extends SpecBase with PropertyChecks with AsyncSpecBase {

  import ExpiredProductsStageSpec._
  implicit val traced: Traced = Traced.empty
  private val expiredProductsStage = new ExpiredProductsStage(new SimpleFeatures)

  "ExpiredProductsStage" should {
    "do nothing" when {
      "product has NoOp expiration policy" in {
        forAll(productWithExpirationPolicy(NoOp)) { product =>
          val state = ProcessingState(product)
          expiredProductsStage.process(state).futureValue shouldBe state
        }
      }
    }

    "update visit time" when {
      "product is not expired" in {
        forAll(ActiveNotExpiredProductGen) { product =>
          val state = ProcessingState(product)
          expiredProductsStage.process(state).futureValue.entry.visitTime shouldBe product.endTime
        }
      }
    }

    "stop product" when {
      "product is expired and pending" in {
        forAll(PendingExpiredProductGen) { product =>
          val state = ProcessingState(product)
          val result = expiredProductsStage.process(state).futureValue
          result.entry.status shouldBe PurchasedProductStatuses.Cancelled
          result.entry.visitTime shouldBe None
        }
      }

      "expiration policy is Stop" in {
        forAll(productWithExpirationPolicy(Stop)) { product =>
          val state = ProcessingState(product)
          val result = expiredProductsStage.process(state).futureValue
          result.createdEntry shouldBe None
          result.entry.status shouldBe PurchasedProductStatuses.Expired
          result.entry.visitTime shouldBe None
        }
      }
    }

    "stop product and create the new one" when {
      "expiration policy is Prolong" in {
        forAll(productWithExpirationPolicy(Prolong(1000))) { product =>
          val state = ProcessingState(product)
          val result = expiredProductsStage.process(state).futureValue
          result.entry.status shouldBe PurchasedProductStatuses.Expired
          result.entry.visitTime shouldBe None

          val created = result.createdEntry.get
          created.purchaseId shouldBe product.purchaseId
          created.owner shouldBe product.owner
          created.product shouldBe product.product
          created.target shouldBe product.target
          created.source shouldBe product.source
          created.startTime shouldBe product.endTime
          created.endTime shouldBe created.startTime.map(_.plus(1000))
          created.status shouldBe PurchasedProductStatuses.Pending
          created.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.NoOp
          created.priceContext shouldBe None
          created.billingContext shouldBe None
          created.context shouldBe product.context
          created.expirationPolicy shouldBe product.expirationPolicy
          created.visitTime shouldBe Some(created.createTime)
        }
      }

      "expiration policy is ProlongAsJuridical" in {
        forAll(productWithExpirationPolicy(ProlongAsJuridical(1000))) { product =>
          val state = ProcessingState(product)
          val result = expiredProductsStage.process(state).futureValue
          result.entry.status shouldBe PurchasedProductStatuses.Expired
          result.entry.visitTime shouldBe None

          val created = result.createdEntry.get
          created.purchaseId shouldBe product.purchaseId
          created.owner shouldBe product.owner
          created.product shouldBe product.product
          created.target shouldBe product.target
          created.source shouldBe product.source
          created.startTime shouldBe product.endTime
          created.endTime shouldBe created.startTime.map(_.plus(1000))
          created.status shouldBe PurchasedProductStatuses.Pending
          created.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.NoOp
          created.priceContext shouldBe None
          created.billingContext shouldBe None
          created.context.paymentType shouldBe Some(PaymentType.JURIDICAL_PERSON)
          created.context.duration shouldBe 1000.millis
          created.expirationPolicy shouldBe product.expirationPolicy
          created.visitTime shouldBe Some(created.createTime)
        }
      }
    }
  }
}

object ExpiredProductsStageSpec extends SellerModelGenerators {

  val ActiveNotExpiredProductGen: Gen[PurchasedProduct] = for {
    product <- purchasedProductGen
  } yield product.copy(status = PurchasedProductStatuses.Active)

  val PendingExpiredProductGen: Gen[PurchasedProduct] = for {
    product <- purchasedProductGen
    endTime <- DateTimeGenerators.dateTimeInPast
  } yield product.copy(status = PurchasedProductStatuses.Pending, endTime = Some(endTime), expirationPolicy = Stop)

  val ActiveExpiredProductGen: Gen[PurchasedProduct] = for {
    product <- ActiveNotExpiredProductGen
    endTime <- DateTimeGenerators.dateTimeInPast
  } yield product.copy(endTime = Some(endTime))

  def productWithExpirationPolicy(ep: ExpirationPolicy): Gen[PurchasedProduct] =
    for {
      product <- ActiveExpiredProductGen
    } yield product.copy(expirationPolicy = ep)
}
