package ru.auto.salesman.service.application_credit

import ru.auto.salesman.billing.CampaignsClient
import ru.auto.salesman.client.billing.model.BillingProductType.ApplicationCreditSingleTariffCarsNew
import ru.auto.salesman.dao.BalanceClientDao
import ru.auto.salesman.model.{AutoruDealer, DetailedClient, ProductId, ProductTariff}
import ru.auto.salesman.service.{BillingService, PriceExtractor}
import ru.auto.salesman.service.application_credit.TariffScope.CarsNew
import ru.auto.salesman.tariffs.CreditTariffs.AccessTariff.Status.TURNED_OFF_ACTIVE
import ru.auto.salesman.tariffs.CreditTariffs.SingleTariff
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.money.Money.Kopecks
import ru.yandex.vertis.billing.Model.InactiveReason.NO_ENOUGH_FUNDS

import scala.util.Success

class CreditSingleTariffReadServiceImplSpec extends BaseSpec {

  private val campaignsClient = mock[CampaignsClient]
  private val billingService = mock[BillingService]
  private val balanceClientDao = mock[BalanceClientDao]
  private val productInfoService = mock[ProductInfoService]

  private val service =
    new CreditSingleTariffReadServiceImpl(
      billingService,
      campaignsClient,
      balanceClientDao,
      productInfoService
    )

  private val dealer1 = AutoruDealer("dealer:123")
  private val dealer2 = AutoruDealer("dealer:1234")
  private val dealer3 = AutoruDealer("dealer:12345")

  private val ownerId1 = 1023L
  private val ownerId2 = 10234L
  private val ownerId3 = 102345L
  private val ownerId4 = 1023456L

  private val goodCarsNew = buildGood(
    "application-credit:single:tariff:cars:new"
  )

  private val goodCarsUsed = buildGood(
    "application-credit:single:tariff:cars:used"
  )
  private val extraGood = buildGood("extra:custom")

  private val productExtra = buildProduct(List(extraGood))
  private val productNew = buildProduct(List(goodCarsNew))
  private val productUsed = buildProduct(List(goodCarsUsed))

  private val owner1 = buildOwner(ownerId1)
  private val owner2 = buildOwner(ownerId2)
  private val owner3 = buildOwner(ownerId3)
  private val owner4 = buildOwner(ownerId4)

  private val campaign1 = buildActiveCampaign(productNew, owner1)
  private val campaign2 = buildActiveCampaign(productUsed, owner2)
  private val campaign3 = buildActiveCampaign(productUsed, owner3)
  private val campaign4 = buildActiveCampaign(productNew, owner2)
  private val campaign5 = buildActiveCampaign(productExtra, owner4)

  private val balanceClientId = 111L
  private val balanceAgencyId = Some(11111L)
  private val orderId = 1111L

  private val client1 =
    buildDetailedClient(dealer1.id, balanceClientId, balanceAgencyId, orderId)

  private val singleCreditProductInfo = PriceExtractor.ProductInfo(
    ProductId.CreditApplication,
    Kopecks(250000L),
    None,
    None,
    None,
    appliedExperiment = None,
    policyId = None
  )

  "CreditSingleTariffServiceImpl.getTariff" should {

    "return tariff" in {
      (billingService.getCampaign _)
        .expects(client1, ApplicationCreditSingleTariffCarsNew)
        .returningZ(Some(campaign1))

      (productInfoService
        .getSingleProductInfo(
          _: DetailedClient,
          _: ProductTariff
        ))
        .expects(client1, ProductTariff.ApplicationCreditSingleTariffCarsNew)
        .returningZ(singleCreditProductInfo)

      service
        .getTariff(client1, CarsNew, TURNED_OFF_ACTIVE)
        .map { tariff =>
          tariff.getBasePriceRubles shouldBe 2500
          tariff.getStatus shouldBe SingleTariff.Status.NEXT_ACTIVE
        }
        .success
        .value
    }
  }

  "CreditSingleTariffServiceImpl.dealers" should {

    "return dealers with active single tariffs partitioned by scopes" in {
      val appropriateCampaigns =
        List(campaign1, campaign2, campaign3, campaign4)

      (campaignsClient.getCampaignHeaders _)
        .expects()
        .returning(Success(campaign5 :: appropriateCampaigns))

      (balanceClientDao.getBillingAutoruClientsMapping _)
        .expects(appropriateCampaigns.map(_.getOwner.getId.getClientId).toSet)
        .returningZ(
          Map(
            ownerId1 -> dealer1.id,
            ownerId2 -> dealer2.id,
            ownerId3 -> dealer3.id
          )
        )

      val rs = service.dealers.success.value

      rs.dealersWithCarsNew shouldEqual Set(dealer1, dealer2)
      rs.dealersWithCarsUsed shouldEqual Set(dealer2, dealer3)
    }

    "not return dealer with inactive campaign" in {
      val inactiveCampaign =
        buildInactiveCampaign(productNew, owner1, NO_ENOUGH_FUNDS)

      (campaignsClient.getCampaignHeaders _)
        .expects()
        .returningT(List(inactiveCampaign))

      (balanceClientDao.getBillingAutoruClientsMapping _)
        .expects(*)
        .returningZ(Map(ownerId1 -> dealer1.id))

      val rs = service.dealers.success.value

      rs.dealersWithCarsNew shouldEqual Set()
    }
  }

}
