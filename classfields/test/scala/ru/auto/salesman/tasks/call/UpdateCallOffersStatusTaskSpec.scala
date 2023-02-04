package ru.auto.salesman.tasks.call

import org.mockito.Mockito._
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.billing.CampaignsClient
import ru.auto.salesman.dao.ClientDao.ForStatusNotDeleted
import ru.auto.salesman.dao.OfferDao.Condition.OfferIdCategory
import ru.auto.salesman.dao.OfferDao.OfferPatch
import ru.auto.salesman.dao.{BalanceClientDao, ClientDao, GoodsDao, OfferDao}
import ru.auto.salesman.environment._
import ru.auto.salesman.model.AutoruDealer
import ru.auto.salesman.model.OfferCategories._
import ru.auto.salesman.model.OfferCurrencies._
import ru.auto.salesman.model.OfferStatuses._
import ru.auto.salesman.model.ProductId.Placement
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.service.DealerProductApplyService.ProductApplyResult.Applied
import ru.auto.salesman.service.{
  CallOfferUpdater,
  DealerProductApplyService,
  EpochService,
  GoodsDaoProvider
}
import ru.auto.salesman.tasks.call.TestData.{
  campaignHeader,
  clientBaseRecord,
  clientRecordActive,
  clientRecordStopped
}
import ru.auto.salesman.test.DeprecatedMockitoBaseSpec
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.yandex.vertis.billing.Model.InactiveReason
import ru.yandex.vertis.mockito.util._

import scala.util.Success

class UpdateCallOffersStatusTaskSpec extends DeprecatedMockitoBaseSpec {

  val currTime = now()

  val balanceClientDao = mock[BalanceClientDao]
  val clientDao = mock[ClientDao]
  val offerDao = mock[OfferDao]
  val goodsDao = mock[GoodsDao]
  val campaignsClient = mock[CampaignsClient]
  val offerFilter = mock[OfferDao.Filter]
  val productApplyService = mock[DealerProductApplyService]
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

  private val taskName = classOf[UpdateCallOffersStatusTask].getName

  "UpdateCallOffersStatusTask" should {
    "update call offer waiting for activation" in {

      val task = new UpdateCallOffersStatusTask(
        offerDao,
        goodsDao,
        clientDao,
        balanceClientDao,
        campaignsClient,
        offerFilter,
        callOfferUpdater,
        mock[EpochService],
        paymentModelFactory,
        currTime,
        None,
        taskName
      )

      stubGoodsDaoProvider()
      when(balanceClientDao.getNotDeleted())
        .thenReturn(Success(List(clientBaseRecord)))
      when(offerDao.get(?)).thenReturn(Success(List(offer)))
      when(goodsDao.update(?, ?)).thenReturn(Success(()))
      when(clientDao.get(ForStatusNotDeleted))
        .thenReturnZ(List(clientRecordActive))
      when(campaignsClient.getCampaignHeaders)
        .thenReturn(Success(List(campaignHeader.build())))
      when(productApplyService.applyProduct(?, ?, ?, ?)(?)).thenReturnT(Applied)

      task.execute()

      verify(productApplyService).applyProduct(
        eq(AutoruOfferId("1-abc")),
        eq(Placement),
        eq(AutoruDealer(1)),
        customPrice = eq(None)
      )(?)
    }

    "update call offer from active to expired if campaign is inactive" in {

      val task = new UpdateCallOffersStatusTask(
        offerDao,
        goodsDao,
        clientDao,
        balanceClientDao,
        campaignsClient,
        offerFilter,
        callOfferUpdater,
        mock[EpochService],
        paymentModelFactory,
        currTime,
        None,
        taskName
      )

      when(balanceClientDao.getNotDeleted())
        .thenReturn(Success(List(clientBaseRecord)))
      when(offerDao.get(?)).thenReturn(
        Success(List(offer.copy(expireDate = currTime.minusMillis(1))))
      )
      when(goodsDao.update(?, ?)).thenReturn(Success(()))
      when(clientDao.get(?)).thenReturnZ(List(clientRecordActive))

      when(campaignsClient.getCampaignHeaders)
        .thenReturn(
          Success(
            List(
              campaignHeader
                .setInactiveReason(InactiveReason.DEPOSIT_LIMIT_EXCEEDED)
                .build()
            )
          )
        )

      when(offerDao.update(?, ?)).thenReturn(Success(()))

      task.execute()

      verify(offerDao).update(
        OfferIdCategory(offer.id, Cars),
        OfferPatch(status = Some(Expired), setDate = Some(currTime))
      )
    }

    "update call offer from active to hidden if campaign is active but client is not active" in {

      val task = new UpdateCallOffersStatusTask(
        offerDao,
        goodsDao,
        clientDao,
        balanceClientDao,
        campaignsClient,
        offerFilter,
        callOfferUpdater,
        mock[EpochService],
        paymentModelFactory,
        currTime,
        None,
        taskName
      )

      when(balanceClientDao.getNotDeleted())
        .thenReturn(Success(List(clientBaseRecord)))
      when(offerDao.get(?)).thenReturn(
        Success(List(offer.copy(expireDate = currTime.minusMillis(1))))
      )
      when(goodsDao.update(?, ?)).thenReturn(Success(()))
      when(clientDao.get(?)).thenReturnZ(List(clientRecordStopped))

      when(campaignsClient.getCampaignHeaders)
        .thenReturn(Success(List(campaignHeader.build())))

      when(offerDao.update(?, ?)).thenReturn(Success(()))

      task.execute()

      verify(offerDao).update(
        OfferIdCategory(offer.id, Cars),
        OfferPatch(status = Some(Hidden), setDate = Some(currTime))
      )
    }
  }

  private def stubGoodsDaoProvider(): Unit = {
    when(goodsDao.get(?)).thenReturnT(Nil)
    when(goodsDaoProvider.chooseDao(?)).thenReturn(goodsDao)
  }
}
