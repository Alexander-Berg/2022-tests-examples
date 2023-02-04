package ru.auto.salesman.tasks

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import org.scalacheck.Gen
import org.scalatest._
import ru.auto.api.ApiOfferModel.Section._
import ru.auto.salesman.billing.CampaignsClient
import ru.auto.salesman.dao.ClientDao.ForStatusWithPoi7
import ru.auto.salesman.dao.GoodsDao.{Source, SourceDetails}
import ru.auto.salesman.dao.OfferDao.Condition.OfferIdCategory
import ru.auto.salesman.dao.OfferDao.{ForClientIdCategoryStatus, OfferPatch}
import ru.auto.salesman.dao._
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.AdsRequestTypes.{CarsUsed, Commercial}
import ru.auto.salesman.model.GoodStatuses.Active
import ru.auto.salesman.model.OfferCategories.Cars
import ru.auto.salesman.model.OfferStatuses.{Expired, WaitingActivation}
import ru.auto.salesman.model.Poi7Property.{
  AutoActivateCarsOffers,
  AutoActivateCommercialOffers
}
import ru.auto.salesman.model.ProductId.Placement
import ru.auto.salesman.model.{DetailedClient, _}
import ru.auto.salesman.service.{AdsRequestService, BillingService, DetailedClientSource}
import ru.auto.salesman.tasks.logging.LoggedActivateExpiredSingleOffersTask
import ru.auto.salesman.test.DeprecatedMockitoBaseSpec
import ru.auto.salesman.test.dao.gens.GoodRecordGen
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model.InactiveReason.MANUALLY_DISABLED
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import ru.yandex.vertis.mockito.util._

class ActivateExpiredSingleOffersTaskSpec
    extends DeprecatedMockitoBaseSpec
    with OneInstancePerTest {

  private val clientDao = mock[ClientDao]
  private val balanceClientDao = mock[BalanceClientDao]
  private val offerDao = mock[OfferDao]
  private val goodsDao = mock[GoodsDao]
  private val billingClient = mock[CampaignsClient]
  private val adsRequestService = mock[AdsRequestService]
  private val goodsRecord = mock[GoodsDao.Record]
  private val detailedClientSource = mock[DetailedClientSource]
  private val billingService = mock[BillingService]
  private val taskStartTime = now()
  private val regionWithSingleWithCalls = RegionId(3228)

  private val paymentModelFactory =
    TestPaymentModelFactory.withMockRegions(Set(regionWithSingleWithCalls))

  def initMocks(
      client: Client,
      adsRequest: Option[AdsRequestDao.Record],
      balanceData: BalanceClient,
      amount: AccountId,
      offer: Offer
  ): Unit = {
    val balance = balanceData.copy(amount = amount)
    when(billingClient.getCampaignHeaders()).thenReturnT(Nil)
    when(clientDao.get(?)).thenReturnZ(List(client))
    when(adsRequestService.get(?, ?)(?)).thenReturnT(adsRequest)
    when(balanceClientDao.get(?)).thenReturnZ(Some(balance))
    when(offerDao.get(?)).thenReturnT(List(offer))
    when(goodsDao.insert(?)).thenReturnZ(goodsRecord)
    when(offerDao.update(?, ?)).thenReturnT(unit)
  }

  "ActivateExpiredSingleOffersTask for cars" should {

    val task = new ActivateExpiredSingleOffersTask(
      clientDao,
      balanceClientDao,
      offerDao,
      goodsDao,
      billingClient,
      adsRequestService,
      CarsUsed,
      paymentModelFactory,
      detailedClientSource,
      billingService
    )

    "execute successfully for Single payment model" in {
      forAll(
        clientRecordGen(singlePaymentsGen = Set(CarsUsed)),
        balanceRecordGen,
        Gen.chooseNum[Long](100, 10000),
        OfferGen
      ) { (client, balanceData, amount, offerData) =>
        val offer = offerData.copy(categoryId = Cars, sectionId = USED)
        val adsRequest = AdsRequestDao.Record(client.clientId, CarsUsed)
        initMocks(client, Some(adsRequest), balanceData, amount, offer)
        task.execute().success.value shouldBe unit
        verify(billingClient, atLeastOnce()).getCampaignHeaders()
        verify(clientDao).get(
          ForStatusWithPoi7(
            ClientStatuses.Active,
            AutoActivateCarsOffers -> Poi7Value("1")
          )
        )
        verify(adsRequestService).get(eq(client.clientId), eq(CarsUsed))(?)
        verify(balanceClientDao).get(client.clientId)
        verify(offerDao).get(
          ForClientIdCategoryStatus(client.clientId, Cars, Expired)
        )
        verify(goodsDao).insert(
          ArgumentMatchers.argThat(
            new ArgumentMatcher[Source] {
              def matches(source: Source): Boolean = source match {
                case Source(
                      offer.id,
                      Placement,
                      Active,
                      "",
                      activateDate,
                      Some(
                        SourceDetails(
                          Some(offer.offerHash),
                          offer.categoryId,
                          offer.sectionId,
                          client.clientId
                        )
                      )
                    ) =>
                  activateDate >= taskStartTime
                case _ => false
              }
            }
          )
        )
        verify(offerDao).update(
          OfferIdCategory(offer.id, offer.categoryId),
          OfferPatch(status = Some(WaitingActivation))
        )
        // don't verify billingClient here, because mockito can't check properly default args
        verifyNoMoreInteractions(
          clientDao,
          adsRequestService,
          balanceClientDao,
          offerDao,
          goodsDao
        )
        reset(
          billingClient,
          adsRequestService,
          clientDao,
          balanceClientDao,
          offerDao,
          goodsDao
        )
      }
    }

    "execute successfully for SingleWithCalls payment model" in {
      forAll(
        clientRecordGen(
          singlePaymentsGen = Set(CarsUsed),
          regionIdGen = regionWithSingleWithCalls
        ),
        balanceRecordGen,
        Gen.chooseNum[Long](100, 10000),
        OfferGen,
        campaignHeaderGen(
          inactiveReasonGen = None,
          isEnabled = true
        )
      ) { (client, balanceData, amount, offerData, campaign) =>
        val detailedClient = DetailedClient(client, balanceData)
        val offer = offerData.copy(categoryId = Cars, sectionId = USED)
        when(billingClient.getCampaignHeaders()).thenReturnT(Nil)
        when(clientDao.get(?)).thenReturnZ(List(client))
        when(balanceClientDao.get(?)).thenReturnZ(Some(balanceData.copy(amount = amount)))
        when(offerDao.get(?)).thenReturnT(List(offer))
        when(goodsDao.insert(?)).thenReturnZ(goodsRecord)
        when(offerDao.update(?, ?)).thenReturnT(unit)
        when(detailedClientSource.unsafeResolve(?, ?))
          .thenReturnZ(detailedClient)
        when(billingService.getProductCampaign(detailedClient, ProductId.CallCarsUsed))
          .thenReturnZ(Some(campaign))
        task.execute().success.value shouldBe unit
        verify(billingClient, atLeastOnce()).getCampaignHeaders()
        verify(clientDao).get(
          ForStatusWithPoi7(
            ClientStatuses.Active,
            AutoActivateCarsOffers -> Poi7Value("1")
          )
        )
        verify(balanceClientDao).get(client.clientId)
        verify(offerDao).get(
          ForClientIdCategoryStatus(client.clientId, Cars, Expired)
        )
        verify(detailedClientSource).unsafeResolve(
          client.clientId,
          withDeleted = false
        )
        verify(billingService).getProductCampaign(
          detailedClient,
          ProductId.CallCarsUsed
        )
        verify(goodsDao).insert(
          ArgumentMatchers.argThat(
            new ArgumentMatcher[Source] {
              def matches(source: Source): Boolean = source match {
                case Source(
                      offer.id,
                      Placement,
                      Active,
                      "",
                      activateDate,
                      Some(
                        SourceDetails(
                          Some(offer.offerHash),
                          offer.categoryId,
                          offer.sectionId,
                          client.clientId
                        )
                      )
                    ) =>
                  activateDate >= taskStartTime
                case _ => false
              }
            }
          )
        )
        verify(offerDao).update(
          OfferIdCategory(offer.id, offer.categoryId),
          OfferPatch(status = Some(WaitingActivation))
        )
        // don't verify billingClient here, because mockito can't check properly default args
        verifyNoMoreInteractions(
          clientDao,
          balanceClientDao,
          offerDao,
          goodsDao,
          detailedClientSource,
          billingService
        )
        reset(
          billingClient,
          clientDao,
          balanceClientDao,
          offerDao,
          goodsDao,
          detailedClientSource,
          billingService
        )
      }
    }

    "not activate offers on client without call:cars:used RC" in {
      forAll(
        clientRecordGen(
          singlePaymentsGen = Set(Commercial),
          regionIdGen = regionWithSingleWithCalls
        ),
        balanceRecordGen
      ) { (client, balance) =>
        val detailedClient = DetailedClient(client, balance)
        when(billingClient.getCampaignHeaders()).thenReturnT(Nil)
        when(clientDao.get(?)).thenReturnZ(List(client))
        when(detailedClientSource.unsafeResolve(?, ?))
          .thenReturnZ(detailedClient)
        when(billingService.getProductCampaign(detailedClient, ProductId.CallCarsUsed))
          .thenReturnZ(None)
        task.execute().success.value shouldBe unit
        verify(clientDao).get(
          ForStatusWithPoi7(
            ClientStatuses.Active,
            AutoActivateCarsOffers -> Poi7Value("1")
          )
        )
        verify(detailedClientSource).unsafeResolve(
          client.clientId,
          withDeleted = false
        )
        verify(billingService).getProductCampaign(
          detailedClient,
          ProductId.CallCarsUsed
        )
        verifyNoMoreInteractions(
          clientDao,
          adsRequestService,
          balanceClientDao,
          offerDao,
          goodsDao,
          billingService,
          detailedClientSource
        )
        reset(
          billingClient,
          adsRequestService,
          clientDao,
          balanceClientDao,
          offerDao,
          goodsDao,
          billingService,
          detailedClientSource
        )
      }
    }

    "not activate offers on client without ENABLED call:cars:used RC" in {
      forAll(
        clientRecordGen(
          singlePaymentsGen = Set(Commercial),
          regionIdGen = regionWithSingleWithCalls
        ),
        balanceRecordGen,
        campaignHeaderGen(inactiveReasonGen = Some(MANUALLY_DISABLED))
      ) { (client, balance, campaign) =>
        val detailedClient = DetailedClient(client, balance)
        when(billingClient.getCampaignHeaders()).thenReturnT(Nil)
        when(clientDao.get(?)).thenReturnZ(List(client))
        when(detailedClientSource.unsafeResolve(?, ?))
          .thenReturnZ(detailedClient)
        when(billingService.getProductCampaign(detailedClient, ProductId.CallCarsUsed))
          .thenReturnZ(Some(campaign))
        task.execute().success.value shouldBe unit
        verify(clientDao).get(
          ForStatusWithPoi7(
            ClientStatuses.Active,
            AutoActivateCarsOffers -> Poi7Value("1")
          )
        )
        verify(detailedClientSource).unsafeResolve(
          client.clientId,
          withDeleted = false
        )
        verify(billingService).getProductCampaign(
          detailedClient,
          ProductId.CallCarsUsed
        )
        verifyNoMoreInteractions(
          clientDao,
          adsRequestService,
          balanceClientDao,
          offerDao,
          goodsDao,
          billingService,
          detailedClientSource
        )
        reset(
          billingClient,
          adsRequestService,
          clientDao,
          balanceClientDao,
          offerDao,
          goodsDao,
          billingService,
          detailedClientSource
        )
      }
    }

    "not activate offers on client without cars:used ads request in salesman" in {
      forAll(clientRecordGen(singlePaymentsGen = Set(CarsUsed))) { client =>
        when(billingClient.getCampaignHeaders()).thenReturnT(Nil)
        when(clientDao.get(?)).thenReturnZ(List(client))
        when(adsRequestService.get(?, ?)(?)).thenReturnT(None)
        task.execute().success.value shouldBe unit
        verify(clientDao).get(
          ForStatusWithPoi7(
            ClientStatuses.Active,
            AutoActivateCarsOffers -> Poi7Value("1")
          )
        )
        verify(adsRequestService).get(?, ?)(?)
        verifyNoMoreInteractions(
          clientDao,
          adsRequestService,
          balanceClientDao,
          offerDao,
          goodsDao
        )
        reset(
          billingClient,
          adsRequestService,
          clientDao,
          balanceClientDao,
          offerDao,
          goodsDao
        )
      }
    }

    "not activate old expired offers" in {
      forAll(
        clientRecordGen(singlePaymentsGen = Set(CarsUsed)),
        balanceRecordGen,
        Gen.chooseNum[Long](100, 10000),
        OfferGen
      ) { (client, balanceData, amount, offerData) =>
        val oldExpireDate = DateTime.now().minusDays(15)
        val offer = offerData.copy(
          categoryId = Cars,
          sectionId = USED,
          expireDate = oldExpireDate
        )
        val adsRequest = AdsRequestDao.Record(client.clientId, CarsUsed)
        initMocks(client, Some(adsRequest), balanceData, amount, offer)
        task.execute().success.value shouldBe unit
        verify(billingClient, atLeastOnce()).getCampaignHeaders()
        verify(clientDao).get(
          ForStatusWithPoi7(
            ClientStatuses.Active,
            AutoActivateCarsOffers -> Poi7Value("1")
          )
        )
        verify(adsRequestService).get(eq(client.clientId), eq(CarsUsed))(?)
        verify(balanceClientDao).get(client.clientId)
        verify(offerDao).get(
          ForClientIdCategoryStatus(client.clientId, Cars, Expired)
        )
        verify(offerDao, times(0)).update(
          OfferIdCategory(offer.id, offer.categoryId),
          OfferPatch(status = Some(WaitingActivation))
        )
        // don't verify billingClient here, because mockito can't check properly default args
        verifyNoMoreInteractions(
          clientDao,
          adsRequestService,
          balanceClientDao,
          offerDao,
          goodsDao
        )
        reset(
          billingClient,
          adsRequestService,
          clientDao,
          balanceClientDao,
          offerDao,
          goodsDao
        )
      }
    }
  }

  "ActivateExpiredSingleOffersTask for commercial" should {

    val task = new ActivateExpiredSingleOffersTask(
      clientDao,
      balanceClientDao,
      offerDao,
      goodsDao,
      billingClient,
      adsRequestService,
      Commercial,
      paymentModelFactory,
      detailedClientSource,
      billingService
    )

    "execute successfully" in {
      val commercialClientGen =
        clientRecordGen(singlePaymentsGen = Set(Commercial))
      forAll(
        commercialClientGen,
        balanceRecordGen,
        Gen.chooseNum[Long](100, 10000),
        OfferGen,
        CommercialOfferCategoryGen,
        Gen.oneOf(NEW, USED)
      ) { (client, balanceData, amount, offerData, category, section) =>
        val offer = offerData.copy(categoryId = category, sectionId = section)
        val adsRequest = AdsRequestDao.Record(client.clientId, Commercial)
        initMocks(client, Some(adsRequest), balanceData, amount, offer)
        task.execute().success.value shouldBe unit
        verify(billingClient, atLeastOnce()).getCampaignHeaders()
        verify(clientDao).get(
          ForStatusWithPoi7(
            ClientStatuses.Active,
            AutoActivateCommercialOffers -> Poi7Value("1")
          )
        )
        verify(adsRequestService).get(eq(client.clientId), eq(Commercial))(?)
        verify(balanceClientDao).get(client.clientId)
        verify(offerDao).get(
          ForClientIdCategoryStatus(
            client.categorizedClientId.value,
            OfferCategories.Commercial,
            Expired
          )
        )
        verify(goodsDao).insert(
          ArgumentMatchers.argThat(
            new ArgumentMatcher[Source] {
              def matches(source: Source): Boolean = source match {
                case Source(
                      offer.id,
                      Placement,
                      Active,
                      "",
                      activateDate,
                      Some(
                        SourceDetails(
                          Some(offer.offerHash),
                          offer.categoryId,
                          offer.sectionId,
                          client.clientId
                        )
                      )
                    ) =>
                  activateDate >= taskStartTime
                case _ => false
              }
            }
          )
        )
        verify(offerDao).update(
          OfferIdCategory(offer.id, offer.categoryId),
          OfferPatch(status = Some(WaitingActivation))
        )
        // don't verify billingClient here, because mockito can't check properly default args
        verifyNoMoreInteractions(
          clientDao,
          adsRequestService,
          balanceClientDao,
          offerDao,
          goodsDao
        )
        reset(
          billingClient,
          adsRequestService,
          clientDao,
          balanceClientDao,
          offerDao,
          goodsDao
        )
      }
    }
  }

  "ActivateExpiredSingleOffersTask" should {

    val TestClientId = 123L
    val TestAgencyId = Option.empty[AgencyId]
    val TestCategorizedClientId = Some(1233L)
    val TestAccountId = 1L

    val TestClient = Client(
      TestClientId,
      TestAgencyId,
      TestCategorizedClientId,
      None,
      RegionId(1L),
      CityId(213L),
      ClientStatuses.Active,
      singlePayment = Set(CarsUsed),
      firstModerated = true,
      paidCallsAvailable = false,
      priorityPlacement = true
    )

    val TestBalanceClient =
      BalanceClient(TestClientId, TestClientId, None, TestAccountId, 1L)

    val date = DateTime.now().plusHours(3)

    val TestOfferId = 456L
    val TestOfferCategory = Cars
    val TestOfferHash = "hfg7hd"
    val TestOffer = Offer(
      TestOfferId,
      TestOfferHash,
      TestOfferCategory,
      USED,
      220000L,
      OfferCurrencies.EUR,
      Expired,
      TestClientId,
      date,
      date,
      date,
      Some(date)
    )

    val clientDao = {
      val m = mock[ClientDao]
      when(
        m.get(
          ForStatusWithPoi7(
            ClientStatuses.Active,
            AutoActivateCarsOffers -> Poi7Value("1")
          )
        )
      )
        .thenReturnZ(List(TestClient))
      m
    }
    val balanceClientDao = mock[BalanceClientDao]

    val offerDao = {
      val m = mock[OfferDao]
      when(m.get(ForClientIdCategoryStatus(TestClientId, Cars, Expired)))
        .thenReturnT(List(TestOffer))
      when(
        m.update(
          OfferIdCategory(TestOfferId, TestOfferCategory),
          OfferPatch(status = Some(WaitingActivation))
        )
      ).thenReturnT(unit)
      m
    }
    val goodsDao = {
      val m = mock[GoodsDao]
      when(m.insert(Source(TestOfferId, Placement, Active, "", ?)))
        .thenReturnZ(GoodRecordGen.next)
      m
    }
    val campaignsClient = {
      val m = mock[CampaignsClient]

      val customer = Model.CustomerId
        .newBuilder()
        .setVersion(1)
        .setClientId(TestClientId)
        .build()
      val ch =
        Model.CustomerHeader.newBuilder.setVersion(1).setId(customer).build()
      val order = Model.Order.newBuilder
        .setVersion(1)
        .setOwner(customer)
        .setId(TestAccountId)
        .setText("order")
        .setCommitAmount(5L)
        .setApproximateAmount(0L)
        .build

      val cost = Model.Cost
        .newBuilder()
        .setVersion(1)
        .setPerIndexing(Model.Cost.PerIndexing.newBuilder.setUnits(222L))
        .build()

      val custom = Model.Good.Custom.newBuilder
        .setId(ProductId.alias(Placement))
        .setCost(cost)
        .build()

      val good = Model.Good.newBuilder().setVersion(1).setCustom(custom).build()

      val product =
        Model.Product.newBuilder.setVersion(1).addGoods(good).build()

      val settings = Model.CampaignSettings
        .newBuilder()
        .setVersion(1)
        .setIsEnabled(true)
        .build()

      val c = Model.CampaignHeader
        .newBuilder()
        .setVersion(1)
        .setOwner(ch)
        .setId("campaign")
        .setOrder(order)
        .setProduct(product)
        .setSettings(settings)
        .build()
      when(m.getCampaignHeaders()).thenReturnT(Iterable(c))
      m
    }

    val adsRequest = AdsRequestDao.Record(TestClientId, CarsUsed)
    when(adsRequestService.get(?, ?)(?)).thenReturnT(Some(adsRequest))

    val task = new ActivateExpiredSingleOffersTask(
      clientDao,
      balanceClientDao,
      offerDao,
      goodsDao,
      campaignsClient,
      adsRequestService,
      CarsUsed,
      paymentModelFactory,
      detailedClientSource,
      billingService
    ) with LoggedActivateExpiredSingleOffersTask {
      def serviceName = "test"
    }

    "execute" in {
      when(balanceClientDao.get(TestClientId))
        .thenReturnZ(Some(TestBalanceClient))

      task.execute().success.value shouldBe unit
    }

    "absence of funds" in {
      when(balanceClientDao.get(TestClientId)).thenReturnZ(None)
      task.execute().success.value shouldBe unit
    }
  }
}
