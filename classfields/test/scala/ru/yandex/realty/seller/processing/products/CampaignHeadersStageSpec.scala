package ru.yandex.realty.seller.processing.products

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.seller.model.ProductType
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.PurchasedProductStatuses
import ru.yandex.realty.seller.service.CampaignHeaderService
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.billing.Model.CampaignHeader
import ru.yandex.vertis.protobuf.ProtoInstanceProvider

import scala.util.control.NoStackTrace

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class CampaignHeadersStageSpec
  extends AsyncSpecBase
  with SellerModelGenerators
  with PropertyChecks
  with ProtobufMessageGenerators
  with ProtoInstanceProvider {

  private val campaignHeaderService = mock[CampaignHeaderService]
  private val stage = new CampaignHeadersStage(campaignHeaderService)
  implicit val traced: Traced = Traced.empty
  private val suitableProductGen = for {
    p <- purchasedProductGen
    pc <- priceContextGen
    bc <- billingContextGen
  } yield p.copy(
    status = PurchasedProductStatuses.Pending,
    context = p.context.copy(paymentType = Some(PaymentType.JURIDICAL_PERSON)),
    priceContext = Some(pc),
    billingContext = Some(bc.copy(campaignHeader = None))
  )

  "CampaignHeadersStage" should {
    "do nothing fot non-pending products" in {
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
      forAll(purchasedProductGen) { p =>
        val product = p.copy(priceContext = None)
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }

    "do nothing if no billing context" in {
      forAll(purchasedProductGen) { p =>
        val product = p.copy(billingContext = None)
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }

    "do nothing if campaign header already present" in {
      forAll(purchasedProductGen.filter(_.billingContext.nonEmpty)) { p =>
        val header = generate[CampaignHeader](15).next
        val billingContext = p.billingContext.get.copy(campaignHeader = Some(header))
        val product = p.copy(billingContext = Some(billingContext))
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }

    "set correct campaign header" in {
      forAll(suitableProductGen) { product =>
        val header = generate[CampaignHeader](15).next
        (campaignHeaderService
          .getHeader(_: UserRef, _: Option[Long], _: ProductType))
          .expects(product.owner, product.billingContext.get.partnerId, product.product)
          .returning(Some(header))

        val state = ProcessingState(product)
        val result = stage.process(state).futureValue

        result.entry.billingContext.get.campaignHeader shouldBe Some(header)
      }
    }

    "reschedule if no header found" in {
      forAll(suitableProductGen) { product =>
        (campaignHeaderService
          .getHeader(_: UserRef, _: Option[Long], _: ProductType))
          .expects(product.owner, product.billingContext.get.partnerId, product.product)
          .returning(None)

        val state = ProcessingState(product)
        val result = stage.process(state).futureValue

        result.entry.copy(visitTime = product.visitTime) shouldBe product
        result.entry.visitTime.nonEmpty shouldBe true
      }
    }

    "reschedule on fail" in {
      val product = suitableProductGen.next
      (campaignHeaderService
        .getHeader(_: UserRef, _: Option[Long], _: ProductType))
        .expects(product.owner, product.billingContext.get.partnerId, product.product)
        .throwing(new RuntimeException("artificial") with NoStackTrace)
      val state = ProcessingState(product)

      val result = stage.process(state).futureValue
      result.entry.copy(visitTime = product.visitTime) shouldBe product
      result.entry.visitTime.nonEmpty shouldBe true
    }
  }
}
