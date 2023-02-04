package ru.auto.salesman.tasks.call

import org.mockito.Mockito._
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.billing.CampaignsClient
import ru.auto.salesman.dao.ClientDao.ForId
import ru.auto.salesman.dao.OfferDao.Condition.OfferIdCategory
import ru.auto.salesman.dao.OfferDao.OfferPatch
import ru.auto.salesman.dao.{BalanceClientDao, ClientDao, GoodsDao, OfferDao}
import ru.auto.salesman.environment._
import ru.auto.salesman.model.AutoruDealer
import ru.auto.salesman.model.OfferCategories.{apply => _, _}
import ru.auto.salesman.model.OfferCurrencies._
import ru.auto.salesman.model.OfferStatuses.{apply => _, _}
import ru.auto.salesman.model.ProductId.Placement
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.service.DealerProductApplyService.ProductApplyResult.Applied
import ru.auto.salesman.service.client.ClientServiceImpl
import ru.auto.salesman.service.{
  CallOfferUpdater,
  DealerProductApplyService,
  GoodsDaoProvider
}
import ru.auto.salesman.test.DeprecatedMockitoBaseSpec
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.yandex.vertis.mockito.util._

import scala.util.Success

class ActivateExpiredCallOffersTaskSpec extends DeprecatedMockitoBaseSpec {

  val currTime = now()

  val balanceClientDao = mock[BalanceClientDao]
  val clientDao = mock[ClientDao]
  val clientService = new ClientServiceImpl(clientDao)
  val offerDao = mock[OfferDao]
  val campaignsClient = mock[CampaignsClient]
  val offerFilter = mock[OfferDao.Filter]
  val productApplyService = mock[DealerProductApplyService]
  val goodsDao = mock[GoodsDao]
  val goodsDaoProvider = mock[GoodsDaoProvider]

  val paymentModelFactory = TestPaymentModelFactory.withoutSingleWithCalls()

  val callOfferUpdater =
    new CallOfferUpdater(productApplyService, offerDao, goodsDaoProvider)

  val offer = ru.auto.salesman.model.Offer(
    1,
    "abc",
    Cars,
    Section.NEW,
    BigDecimal(10000),
    RUR,
    WaitingActivation,
    1,
    currTime,
    currTime,
    currTime,
    Some(currTime)
  )

  private val taskName = classOf[ActivateExpiredCallOffersTask].getName

  "ActivateExpiredCallOffersTask" should {
    "activate expired call offers" in {
      stubGoodsDaoProvider()
      val task = new ActivateExpiredCallOffersTask(
        offerDao,
        clientService,
        balanceClientDao,
        campaignsClient,
        callOfferUpdater,
        paymentModelFactory,
        currTime,
        taskName
      )

      when(balanceClientDao.getNotDeleted())
        .thenReturn(Success(List(TestData.clientBaseRecord)))
      when(offerDao.get(?)).thenReturn(Success(List(offer)))
      when(clientDao.get(ForId(offer.clientId)))
        .thenReturnZ(List(TestData.clientRecordActive))
      when(campaignsClient.getCampaignHeaders)
        .thenReturn(Success(List(TestData.campaignHeader.build())))
      when(productApplyService.applyProduct(?, ?, ?, ?)(?)).thenReturnT(Applied)

      task.execute()

      verify(productApplyService).applyProduct(
        eq(AutoruOfferId("1-abc")),
        eq(Placement),
        eq(AutoruDealer(1)),
        customPrice = eq(None)
      )(?)
    }

    "hide expired call offers if client is not active" in {

      val task = new ActivateExpiredCallOffersTask(
        offerDao,
        clientService,
        balanceClientDao,
        campaignsClient,
        callOfferUpdater,
        paymentModelFactory,
        currTime,
        taskName
      )

      when(balanceClientDao.getNotDeleted())
        .thenReturn(Success(List(TestData.clientBaseRecord)))
      when(offerDao.get(?)).thenReturn(Success(List(offer)))
      when(clientDao.get(ForId(offer.clientId)))
        .thenReturnZ(List(TestData.clientRecordStopped))
      when(campaignsClient.getCampaignHeaders)
        .thenReturn(Success(List(TestData.campaignHeader.build())))
      when(offerDao.update(?, ?)).thenReturn(Success(()))

      task.execute()

      verify(offerDao).update(
        OfferIdCategory(offer.id, Cars),
        OfferPatch(
          expireDate = None,
          status = Some(Hidden),
          setDate = Some(currTime)
        )
      )
    }
  }

  private def stubGoodsDaoProvider(): Unit = {
    when(goodsDao.get(?)).thenReturnT(Nil)
    when(goodsDaoProvider.chooseDao(?)).thenReturn(goodsDao)
  }
}
