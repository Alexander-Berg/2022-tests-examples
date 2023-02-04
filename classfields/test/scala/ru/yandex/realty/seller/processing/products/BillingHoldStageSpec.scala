package ru.yandex.realty.seller.processing.products

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.billing.BillingInternalApiClient
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.PurchasedProductStatuses
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model.CampaignHeader
import ru.yandex.vertis.billing.Model.HoldResponse.ResponseStatus
import ru.yandex.vertis.protobuf.ProtoInstanceProvider
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.control.NoStackTrace

/**
  * @author Vsevolod Levin
  */
class BillingHoldStageSpec
  extends AsyncSpecBase
  with SellerModelGenerators
  with PropertyChecks
  with ProtobufMessageGenerators
  with ProtoInstanceProvider {

  val billingClient: BillingInternalApiClient = mock[BillingInternalApiClient]
  val stage = new BillingHoldStage(billingClient, new SimpleFeatures)
  implicit val traced: Traced = Traced.empty

  private val suitableProductGen = for {
    p <- purchasedProductGen
    st <- dateTimeInPast
    d <- Gen.choose(1.day, 30.days)
    pc <- priceContextGen
    bc <- billingContextGen
  } yield {
    val ch = generate[CampaignHeader](10).next
    p.copy(
      status = PurchasedProductStatuses.Pending,
      startTime = Some(st),
      context = p.context.copy(paymentType = Some(PaymentType.JURIDICAL_PERSON), duration = d),
      priceContext = Some(pc),
      billingContext = Some(bc.copy(campaignHeader = Some(ch)))
    )
  }

  "BillingHoldStage" should {
    "do nothing for non-pending products" in {
      forAll(purchasedProductGen.filter(_.status != PurchasedProductStatuses.Pending)) { product =>
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }

    "do nothing for products of non-juridical" in {
      forAll(purchasedProductGen.filter(!_.context.paymentType.contains(PaymentType.JURIDICAL_PERSON))) { product =>
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }

    "do nothing if no price context" in {
      forAll(purchasedProductGen.filter(_.priceContext.isEmpty)) { product =>
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }

    "do nothing if no billing context" in {
      forAll(purchasedProductGen.filter(_.billingContext.isEmpty)) { product =>
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }

    "do nothing if no campaign header" in {
      forAll(purchasedProductGen.filter(_.billingContext.nonEmpty)) { product =>
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }

    "do nothing if effective price is zero" in {
      forAll(suitableProductGen, priceContextGen) { (p, pc) =>
        val product = p.copy(priceContext = Some(pc.copy(effectivePrice = 0)))
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }

    "hold funds and activate product" in {
      forAll(suitableProductGen, generate[Model.HoldResponse](10)) { (product, resp) =>
        whenever(resp.getStatus == ResponseStatus.OK || resp.getStatus == ResponseStatus.ALREADY_EXISTS) {
          val holdResponse = resp.toBuilder
            .clearErrorMessage()
            .build()

          (billingClient
            .hold(_: Seq[Model.HoldRequest])(_: Traced))
            .expects(where { (requests, _) =>
              requests.size == 1 &&
              requests.head.getOrderId == product.billingContext.get.campaignHeader.get.getOrder.getId &&
              requests.head.getTtlSec == product.context.duration.toSeconds &&
              requests.head.getAmount == product.priceContext.get.effectivePrice
            })
            .returning(Future.successful(Seq(holdResponse)))

          val state = ProcessingState(product)
          val result = stage.process(state).futureValue.entry
          result.billingContext.get.holdRequest.nonEmpty shouldBe true
        }
      }
    }

    "reschedule on error" in {
      forAll(suitableProductGen, generate[Model.HoldResponse](10)) { (product, holdResponse) =>
        whenever(holdResponse.getStatus == ResponseStatus.NO_ENOUGH_FUNDS || holdResponse.hasErrorMessage) {

          (billingClient
            .hold(_: Seq[Model.HoldRequest])(_: Traced))
            .expects(*, *)
            .returning(Future.successful(Seq(holdResponse)))

          val state = ProcessingState(product)

          val result = stage.process(state).futureValue
          result.entry.copy(visitTime = product.visitTime) shouldBe product
          result.entry.visitTime.nonEmpty shouldBe true
        }
      }
    }

    "reschedule on fail" in {
      val product = suitableProductGen.next
      (billingClient
        .hold(_: Seq[Model.HoldRequest])(_: Traced))
        .expects(*, *)
        .returning(Future.failed(new RuntimeException("artificial") with NoStackTrace))
      val state = ProcessingState(product)

      val result = stage.process(state).futureValue
      result.entry.copy(visitTime = product.visitTime) shouldBe product
      result.entry.visitTime.nonEmpty shouldBe true
    }

  }

}
