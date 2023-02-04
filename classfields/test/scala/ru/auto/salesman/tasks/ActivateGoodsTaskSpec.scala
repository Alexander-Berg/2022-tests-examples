package ru.auto.salesman.tasks

import org.mockito.Mockito.verify
import ru.auto.api.ApiOfferModel.Section.USED
import ru.auto.api.ApiOfferModel.{Category, OfferStatus, Section}
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.dao.GoodsDao.Filter.NeedActivation
import ru.auto.salesman.model.OfferCategories.Cars
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.payment_model.PaymentModelFactory
import ru.auto.salesman.model.{
  AdsRequestType,
  AdsRequestTypes,
  CampaignSource,
  CityId,
  DetailedClient,
  ProductId,
  RegionId
}
import ru.auto.salesman.service.ClientGoodsSource.OfferRecords
import ru.auto.salesman.service._
import ru.auto.salesman.tasks.ActivateGoodsTaskSpec._
import ru.auto.salesman.tasks.instrumented.InstrumentedActivateGoodsTask
import ru.auto.salesman.test.dao.gens._
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.auto.salesman.test.{DeprecatedMockitoBaseSpec, TestException}
import ru.auto.salesman.util.AutomatedContext
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class ActivateGoodsTaskSpec extends DeprecatedMockitoBaseSpec with OfferModelGenerators {

  implicit val rc = AutomatedContext("test")

  val TestOfferId = AutoruOfferId(456L, Some("123df"))
  val TestClientId = 123L

  val TestCampaignHeader = {
    val TestAccountId = 1L
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
      .setId(ProductId.alias(ProductId.Placement))
      .setCost(cost)
      .build()

    val good = Model.Good.newBuilder().setVersion(1).setCustom(custom).build()

    val product = Model.Product.newBuilder.setVersion(1).addGoods(good).build()

    val settings = Model.CampaignSettings
      .newBuilder()
      .setVersion(1)
      .setIsEnabled(true)
      .build()

    Model.CampaignHeader
      .newBuilder()
      .setVersion(1)
      .setOwner(ch)
      .setId("campaign")
      .setOrder(order)
      .setProduct(product)
      .setSettings(settings)
      .build()
  }

  val TestClient = DetailedClient(
    TestClientId,
    None,
    111L,
    None,
    None,
    None,
    RegionId(1L),
    CityId(1123L),
    1111L,
    isActive = true,
    firstModerated = true,
    singlePayment = Set(AdsRequestTypes.CarsUsed)
  )

  val TestCampaigns = Iterable(TestCampaignHeader)

  val TestGoods = GoodRecordGen
    .next(3)
    .map(_.copy(product = ProductId.Premium, clientId = TestClientId))
    .toList

  val offerRecords = List(OfferRecords(TestOfferId, TestGoods))

  private def mockClientGoodsSource() = {
    val m = mock[ClientGoodsSource]
    stub(m.scan _) { case (_, handler) =>
      offerRecords.foreach(handler)
      Success(())
    }
    m
  }

  private val paymentModelFactory =
    TestPaymentModelFactory.withoutSingleWithCalls()

  private val singleWithCallsPaymentModelFactory =
    TestPaymentModelFactory.withSingleWithCalls()

  private val callsPlacement =
    GoodRecordGen.next
      .copy(product = ProductId.Placement, clientId = TestClientId)

  private val singleWithCallsPlacement =
    GoodRecordGen.next
      .copy(
        product = ProductId.Placement,
        clientId = TestClientId,
        category = Cars,
        section = USED
      )

  private def mockCallsPlacementSource() = {
    val m = mock[ClientGoodsSource]
    stub(m.scan _) { case (_, handler) =>
      handler(OfferRecords(TestOfferId, List(callsPlacement)))
      Success(())
    }
    m
  }

  private val singleWithCallsPlacementSource = {
    val m = mock[ClientGoodsSource]
    stub(m.scan _) { case (_, handler) =>
      handler(OfferRecords(TestOfferId, List(singleWithCallsPlacement)))
      Success(())
    }
    m
  }

  val faultyClientGoodsSource = {
    val m = mock[ClientGoodsSource]
    when(m.scan(?)(?))
      .thenReturn(Failure(new TestException))
    m
  }

  val goodsService = {
    val m = mock[GoodsService]
    when(m.prolong(?, ?)(?))
      .thenReturn(Success(()))
    m
  }

  val faultyGoodsService = {
    val m = mock[GoodsService]
    when(m.prolong(?, ?)(?))
      .thenReturn(Failure(new TestException))
    m
  }

  def clientSource(
      singlePayment: Set[AdsRequestType] = Set(AdsRequestTypes.CarsUsed),
      paidCallsAvailable: Boolean = false
  ) = {
    val m = mock[DetailedClientSource]
    when(m.unsafeResolve(?, eq(false)))
      .thenReturnZ(
        TestClient.copy(
          singlePayment = singlePayment,
          paidCallsAvailable = paidCallsAvailable
        )
      )
    m
  }

  def mockVosClient(
      category: Category,
      section: Section,
      status: OfferStatus
  ) = {
    val m = mock[VosClient]
    val offer = offerGen(
      offerIdGen = TestOfferId,
      offerCategoryGen = category,
      offerSectionGen = section,
      statusGen = status
    ).next

    when(m.getOffer(?, ?)).thenReturnZ(offer)
    m -> offer
  }

  val TestCampaignCreationService: CampaignCreationService =
    (_: CampaignSource) => Try(TestCampaignHeader)

  "ActivateGoodsTask" should {
    "prolong goods" in {
      val (vosClient, offer) =
        mockVosClient(Category.CARS, Section.USED, OfferStatus.ACTIVE)
      val client = clientSource()
      val clientGoodsSource = mockClientGoodsSource()
      val task = newTask(
        clientGoodsSource,
        goodsService,
        vosClient,
        client,
        paymentModelFactory
      )
      task.execute().success.value shouldBe (())
      verify(clientGoodsSource)
        .scan(eq(NeedActivation(10.minutes, defaultPartition)))(?)
      verify(goodsService)
        .prolong(
          eq(offer),
          eq(TestGoods)
        )(?)
    }

    "prolong goods with SingleWithCalls payment model" in {
      val (vosClient, offer) =
        mockVosClient(Category.CARS, Section.USED, OfferStatus.ACTIVE)
      val client = clientSource()
      val clientGoodsSource = mockClientGoodsSource()
      val task = newTask(
        clientGoodsSource,
        goodsService,
        vosClient,
        client,
        singleWithCallsPaymentModelFactory
      )
      task.execute().success.value shouldBe (())
      verify(clientGoodsSource)
        .scan(eq(NeedActivation(10.minutes, defaultPartition)))(?)
      verify(goodsService)
        .prolong(
          eq(offer),
          eq(TestGoods)
        )(?)
    }

    "prolong calls placement" in {
      val (vosClient, offer) =
        mockVosClient(Category.CARS, Section.NEW, OfferStatus.ACTIVE)
      val client = clientSource(paidCallsAvailable = true)
      val callsPlacementSource = mockCallsPlacementSource()
      val task = newTask(
        callsPlacementSource,
        goodsService,
        vosClient,
        client,
        paymentModelFactory
      )
      task.execute().success.value shouldBe (())
      verify(callsPlacementSource)
        .scan(eq(NeedActivation(10.minutes, defaultPartition)))(?)
      verify(goodsService)
        .prolong(
          eq(offer),
          eq(List(callsPlacement))
        )(?)
    }

    "prolong SingleWithCalls placement" in {
      val (vosClient, offer) =
        mockVosClient(Category.CARS, Section.USED, OfferStatus.ACTIVE)
      val client = clientSource()
      val task = newTask(
        singleWithCallsPlacementSource,
        goodsService,
        vosClient,
        client,
        singleWithCallsPaymentModelFactory
      )
      task.execute().success.value shouldBe (())
      verify(singleWithCallsPlacementSource)
        .scan(eq(NeedActivation(10.minutes, defaultPartition)))(?)
      verify(goodsService)
        .prolong(
          eq(offer),
          eq(List(singleWithCallsPlacement))
        )(?)
    }

    "succeed on goodsService failure" in {
      val (vosClient, _) =
        mockVosClient(Category.CARS, Section.USED, OfferStatus.ACTIVE)
      val client = clientSource()

      val clientGoodsSource = mockClientGoodsSource()
      newTask(
        clientGoodsSource,
        faultyGoodsService,
        vosClient,
        client,
        paymentModelFactory
      )
        .execute()
        .success
        .value shouldBe (())
    }

    "fail on clientGoodsSource failure" in {
      val (vosClient, _) =
        mockVosClient(Category.CARS, Section.USED, OfferStatus.ACTIVE)
      val client = clientSource()

      newTask(
        faultyClientGoodsSource,
        goodsService,
        vosClient,
        client,
        paymentModelFactory
      )
        .execute()
        .failure
        .exception shouldBe a[TestException]
    }
  }
}

object ActivateGoodsTaskSpec {

  private val defaultPartition = Partition.all(1).head

  def newTask(
      goodsSource: ClientGoodsSource,
      goods: GoodsService,
      vosClient: VosClient,
      clientSource: DetailedClientSource,
      paymentModelFactory: PaymentModelFactory
  ): ActivateGoodsTask =
    new ActivateGoodsTask(
      goodsSource,
      goods,
      vosClient,
      clientSource,
      paymentModelFactory,
      "test-task-name",
      defaultPartition
    ) with InstrumentedActivateGoodsTask {
      override def ops: OperationalSupport = TestOperationalSupport
    }
}
