package ru.yandex.vertis.billing.service.checking

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.{EmptyTransactionContext, TransactionContext}
import ru.yandex.vertis.billing.model_core.DynamicPrice.{Constraints, NoConstraints}
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{CallSettingsGen, CampaignHeaderGen, Producer}
import ru.yandex.vertis.billing.service.CampaignService.Filter.ForCustomer
import ru.yandex.vertis.billing.service.CampaignService.Source
import ru.yandex.vertis.billing.service.{CampaignService, OrderService}
import ru.yandex.vertis.billing.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

/**
  * Spec on [[AutoRuInputChecker]]
  *
  * @author alesavin
  */
class AutoRuInputCheckerSpec extends AnyWordSpec with Matchers with MockitoSupport {

  implicit val ac = AutomatedContext("test")

  implicit val tc = EmptyTransactionContext

  val Customer = CustomerId(1000L, None)
  val Customer2 = CustomerId(1001L, None)
  val Order = 1L

  val campaignsMock = {
    val m = mock[CampaignService]
    stub(m.get(_: CampaignService.Filter)(_: RequestContext, _: TransactionContext)) {
      case (ForCustomer(Customer), `ac`, _) =>
        Success(Iterable.empty)
      case (ForCustomer(Customer2), `ac`, _) =>
        Success(CampaignHeaderGen.next(AutoRuInputChecker.MaximumCampaigns))
    }
    m
  }

  val ordersMock = mock[OrderService]

  val inputChecker = new AutoRuInputChecker(ordersMock, campaignsMock)

  "AutoRu input checker" should {
    "pass only allowed products" in {
      inputChecker.checkProduct(Product(Custom("1", CostPerIndexing(DynamicPrice())))).get
      inputChecker.checkProduct(Product(Custom("2", CostPerIndexing(DynamicPrice())))).get
      inputChecker.checkProduct(Product(Custom("dmabshahjsdbm", CostPerIndexing(DynamicPrice())))).get
      inputChecker.checkProduct(Product(Custom("quota", CostPerIndexing(DynamicPrice())))).get
      inputChecker.checkProduct(Product(Custom("certification", CostPerIndexing(DynamicPrice())))).get
      inputChecker.checkProduct(Product(Custom("call", CostPerCall(FixPrice(13232))))).get

      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(Custom("1", CostPerIndexing(DynamicPrice(Some(100L)))))).get
      }
      intercept[IllegalArgumentException] {
        inputChecker
          .checkProduct(Product(Placement(CostPerIndexing(DynamicPrice(None, Constraints(max = Some(500L)))))))
          .get
      }
      intercept[IllegalArgumentException] {
        inputChecker
          .checkProduct(Product(Custom("1", CostPerIndexing(DynamicPrice(None, Constraints(max = Some(500L)))))))
          .get
      }

      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(Custom("1", CostPerIndexing(FixPrice(100L))))).get
      }
      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(Raising(CostPerClick(50L)))).get
      }
      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(`Raise+Highlighting`(CostPerDay(50L)))).get
      }
      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(Raising(CostPerClick(60L)))).get
      }
      intercept[IllegalArgumentException] {
        inputChecker
          .checkProduct(
            Product(
              Set[Good](
                Custom("1", CostPerIndexing(DynamicPrice())),
                Custom("1", CostPerIndexing(DynamicPrice(Some(1L))))
              )
            )
          )
          .get
      }
      intercept[IllegalArgumentException] {
        inputChecker
          .checkProduct(
            Product(
              Set[Good](
                Custom("1", CostPerIndexing(DynamicPrice())),
                Raising(CostPerClick(60L))
              )
            )
          )
          .get
      }
      intercept[IllegalArgumentException] {
        inputChecker.checkProduct(Product(Custom("1", CostPerClick(DynamicPrice(Some(100L)))))).get
      }
    }
    "for owner pass only one campaign with allowed product" in {
      val p = Product(Custom("1", CostPerIndexing(DynamicPrice(None, NoConstraints))))
      inputChecker.checkProductCampaignNonExist(Iterable.empty, p, None).get
      inputChecker.checkProductCampaignNonExist(Iterable.empty, p, Some("id")).get

      val c = CampaignHeaderGen.next.copy(product = p)
      intercept[IllegalArgumentException] {
        inputChecker.checkProductCampaignNonExist(Iterable(c), p, None).get
      }
      intercept[IllegalArgumentException] {
        inputChecker.checkProductCampaignNonExist(Iterable(c), p, Some("id")).get
      }
      inputChecker.checkProductCampaignNonExist(Iterable(c), p, Some(c.id)).get
    }

    val source = Source(
      Some("test"),
      Order,
      Product(Custom("1", CostPerIndexing(DynamicPrice()))),
      CampaignSettings.Default,
      None,
      Iterable.empty
    )

    "pass quota product only with quota size" in {
      val incorrectQuota = source.copy(product = Product(Custom("quota", CostPerDay(DynamicPrice(None)))))
      intercept[IllegalArgumentException] {
        inputChecker.checkSource(Customer, incorrectQuota).get
      }

      val correctQuota = incorrectQuota.copy(product = Product(Custom("quota", CostPerIndexing(DynamicPrice(None)))))

      inputChecker.checkSource(Customer, correctQuota).get
    }

    "pass only without call settings, offers" in {
      inputChecker.checkSource(Customer, source).get
      inputChecker
        .checkSource(Customer, source.copy(settings = source.settings.copy(callSettings = Some(CallSettingsGen.next))))
        .get
      intercept[IllegalArgumentException] {
        inputChecker.checkSource(Customer, source.copy(offerIds = Iterable(Business("1")))).get
      }
    }

    "pass only allowed count of orders, campaigns" in {
      intercept[IllegalArgumentException] {
        inputChecker.checkSource(Customer2, source).get
      }
    }

  }
}
