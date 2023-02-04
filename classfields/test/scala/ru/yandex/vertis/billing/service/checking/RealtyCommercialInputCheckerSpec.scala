package ru.yandex.vertis.billing.service.checking

import org.mockito.Mockito.{mock, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.EmptyTransactionContext
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core.FundsConversions.FundsLong
import ru.yandex.vertis.billing.model_core.gens.{CampaignHeaderGen, Producer}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.CampaignService
import ru.yandex.vertis.billing.service.CampaignService.Filter.ForCustomer
import ru.yandex.vertis.billing.service.CampaignService.Source
import ru.yandex.vertis.billing.util.OperatorContext

import scala.util.Success

/**
  * Tests for [[RealtyCommercialInputChecker]]
  *
  * @author zvez
  */
class RealtyCommercialInputCheckerSpec extends AnyWordSpec with Matchers {

  implicit val op = OperatorContext("test", Uid(1L))

  implicit val tc = EmptyTransactionContext

  import RealtyCommercialInputCheckerSpec._

  val FeedRaise =
    Product(Custom(FeedRaiseGoodId, CostPerIndexing(FixPrice(100L))))
  val Customer = CustomerId(1000L, None)
  val ExistCampaign = CampaignHeaderGen.next.copy(product = FeedRaise)
  val Customer2 = CustomerId(1001L, None)

  val campaignsMock = {
    val m = mock(classOf[CampaignService])
    when(m.get(ForCustomer(Customer))(op, tc)).thenReturn(Success(Iterable.empty))
    when(m.get(ForCustomer(Customer2))(op, tc)).thenReturn(Success(Iterable(ExistCampaign)))
    m
  }

  val checker = new RealtyCommercialInputChecker(campaignsMock)

  def placement(cost: Funds) = Placement(CostPerIndexing(cost))

  "RealtyCommercialInputChecker" should {

    "pass feed products" in {
      for (
        id <- Seq(
          FeedRaiseGoodId,
          FeedPremiumGoodId,
          FeedPromotionGoodId
        )
      ) checker.checkProduct(Product(Custom(id, CostPerIndexing(10.rubles)))).get
    }

    "pass call products" in {
      val withFixPrice = Product(Custom("with_fix_price", CostPerCall(FixPrice(1000L))))
      val withDynamicPrice = Product(Custom("with_fix_price", CostPerCall(DynamicPrice())))
      checker.checkProduct(withFixPrice).get
      checker.checkProduct(withDynamicPrice).get
    }

    "pass custom placement" in {
      checker.checkProduct(Product(Custom(FeedPlacementGoodId, CostPerIndexing(10.rubles)))).get
    }
    "fail on non-format custom" in {
      intercept[IllegalArgumentException] {
        checker.checkProduct(Product(Custom(FeedPlacementGoodId, CostPerOffer(10.rubles)))).get
      }
    }
    "pass placement" in {
      checker.checkProduct(Product(placement(0))).get
      checker.checkProduct(Product(placement(1.ruble))).get
      checker.checkProduct(Product(placement(500.rubles))).get
      checker.checkProduct(Product(placement(1.thousand))).get
    }

    "allow create only one campaign for each product" in {
      val source = Source(
        None,
        1000,
        Product(Custom(FeedRaiseGoodId, CostPerIndexing(FixPrice(1000L)))),
        CampaignSettings.Default,
        None,
        Iterable.empty
      )
      checker.checkSource(Customer, source).get
      checker.checkSource(Customer2, source).get

      val existProductSource = source.copy(product = ExistCampaign.product)
      intercept[IllegalArgumentException] {
        checker.checkSource(Customer2, existProductSource).get
      }

      val patch = CampaignService.Patch(
        product = Some(ExistCampaign.product)
      )
      checker.checkPatch(Customer2, ExistCampaign.id, patch).get

      intercept[IllegalArgumentException] {
        checker.checkPatch(Customer2, "test", patch).get
      }
    }
    "pass payments in allowed range" in {
      checker.checkPayment(OrderPayment(5.thousand)).get
      checker.checkPayment(OrderPayment(100.thousand)).get
      checker.checkPayment(OrderPayment(500.thousand)).get
      checker.checkPayment(OrderPayment(1.million)).get
      checker.checkPayment(OrderPayment(2.million)).get
      checker.checkPayment(OrderPayment(1.thousand)).get

      intercept[IllegalArgumentException] {
        checker.checkPayment(OrderPayment(1)).get
      }
      intercept[IllegalArgumentException] {
        checker.checkPayment(OrderPayment(1.ruble)).get
      }
      intercept[IllegalArgumentException] {
        checker.checkPayment(OrderPayment(101.million)).get
      }
    }

  }

}

object RealtyCommercialInputCheckerSpec {
  val FeedRaiseGoodId = "feed_raise"
  val FeedPremiumGoodId = "feed_premium"
  val FeedPromotionGoodId = "feed_promotion"
  val FeedPlacementGoodId = "feed_placement"
}
