package ru.auto.salesman.tasks

import cats.data.NonEmptyList
import cats.implicits._
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest._
import ru.auto.salesman.dao.ClientDao.{ForIdWithPoi7, ForStatusWithPoi7}
import ru.auto.salesman.dao.OfferDao.ForIdAndCategory
import ru.auto.salesman.dao.QuotaDao.{ActualActivations, Current}
import ru.auto.salesman.dao.impl.jdbc.{JdbcCategorizedOfferDao, JdbcOfferDao}
import ru.auto.salesman.dao.{ClientDao, GoodsDao, OfferDao, QuotaDao}
import ru.auto.salesman.model.ClientStatuses.Active
import ru.auto.salesman.model._
import ru.auto.salesman.service.client.ClientService
import ru.auto.salesman.service.quota_offers.{
  QuotaOffersActualizer,
  QuotaOffersActualizerImpl,
  QuotaOffersManager
}
import ru.auto.salesman.service.{EpochService, QuotaService}
import ru.auto.salesman.tasks.ActivateOffersByQuotaSpec._
import ru.auto.salesman.test.DeprecatedMockitoBaseSpec
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.auto.salesman.test.template.CategorizedSalesJdbcSpecTemplate
import ru.auto.salesman.util.RequestContext
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import ru.yandex.vertis.mockito.MockitoSupport
import zio.ZIO

import scala.util.{Failure, Success, Try}

class ActivateOffersByQuotaSpec
    extends DeprecatedMockitoBaseSpec
    with Mocking
    with CategorizedSalesJdbcSpecTemplate {

  "ActivateOffersByQuota" should {

    "do not activate for empty offers" in {
      val clients = genClients()
      val offers = List.empty[Offer]
      val quotas = genQuotas(ProductId.QuotaPlacementCarsUsed, 111L, 5)

      whenEpochServiceGet(testMarker, Success(testEpoch))
      whenClientDaoGet(ZIO.succeed(clients.toList))
      whenOfferDaoGet(testEpoch, clients, Success(offers))
      whenQuotasGet(testEpoch, Success(quotas))

      activateTask.execute().success.value shouldBe (())

      verify(epochService).get(?)
      verifyNoMoreInteractions(epochService)
      verifyZeroInteractions(quotaOffersService)
    }

    "activate offers" in {
      val clientId1 = 123L
      val quota1 = genQuotas(ProductId.QuotaPlacementCarsNew, clientId1).head
      val paymentGroup1 = PaymentGroup(quota1.quotaType)
      val offers1 = genOffers(5, paymentGroup1, Some(clientId1))

      val clientId2 = 1234L
      val quota2 = genQuotas(ProductId.QuotaPlacementCarsUsed, clientId2).head
      val paymentGroup2 = PaymentGroup(quota2.quotaType)
      val offers2 = genOffers(4, paymentGroup2, Some(clientId2))

      val offers3 = genOffers(
        4,
        PaymentGroup(ProductId.QuotaPlacementCarsNew),
        Some(clientId2)
      )

      val quota3 = genQuotas(ProductId.QuotaPlacementMoto, clientId1).head
      val quota4 = genQuotas(ProductId.QuotaPlacementCarsNew, 1111111L).head

      val offers = offers1 ++ offers2 ++ offers3
      val quotas = Iterable(quota1, quota2, quota3, quota4)
      val maxEpoch = offers.map(_.setDate.getMillis).max

      val List(client1, client2) = genClients().toList.take(2)
      val clients = NonEmptyList.of(
        client1.copy(clientId = clientId1),
        client2.copy(clientId = clientId2)
      )

      whenEpochServiceGet(testMarker, Success(testEpoch))
      whenEpochServiceSet(testMarker, maxEpoch, Success(()))
      whenClientDaoGet(ZIO.succeed(clients.toList))
      whenOfferDaoGet(testEpoch, clients, Success(offers))
      whenQuotasGet(testEpoch, Success(quotas))

      activateTask.execute().success.value shouldBe (())

      verify(epochService).get(?)
      verify(quotaOffersService, times(2)).actualize(?, ?)
      verify(epochService).set(?, ?)
    }

    "failed quota actualize for one quota" in {
      val clientId1 = 123L
      val quota1 = genQuotas(ProductId.QuotaPlacementCarsNew, clientId1).head
      val paymentGroup1 = PaymentGroup(quota1.quotaType)
      val offers1 = genOffers(5, paymentGroup1, Some(clientId1))

      val clientId2 = 1234L
      val quota2 = genQuotas(ProductId.QuotaPlacementCarsUsed, clientId2).head
      val paymentGroup2 = PaymentGroup(quota2.quotaType)
      val offers2 = genOffers(4, paymentGroup2, Some(clientId2))

      val offers = offers1 ++ offers2
      val quotas = Iterable(quota1, quota2)

      val List(client1, client2) = genClients().toList.take(2)
      val clients = NonEmptyList.of(
        client1.copy(clientId = clientId1),
        client2.copy(clientId = clientId2)
      )

      whenClientDaoGet(ZIO.succeed(clients.toList))
      whenEpochServiceGet(testMarker, Success(testEpoch))
      whenOfferDaoGet(testEpoch, clients, Success(offers))
      whenQuotasGet(testEpoch, Success(quotas))
      whenQuotaOffersActualizeFailed(clientId2)

      activateTask.execute().success.value shouldBe (())

      verify(epochService).get(?)
      verify(quotaOffersService, times(2)).actualize(?, ?)
      verifyNoMoreInteractions(epochService)
    }

    "activate offer with deleted_time" in {
      val carsOfferDao = new JdbcOfferDao(JdbcOfferDao, database)
      val categorizedOfferDao =
        new JdbcOfferDao(JdbcCategorizedOfferDao, database)
      val clientSource = mock[ClientService]
      val jdbcQuotaOffersService = new QuotaOffersActualizerImpl(
        new QuotaOffersManager(
          carsOfferDao,
          categorizedOfferDao,
          clientDao,
          clientSource,
          quotaService,
          paymentModelFactory
        ),
        categorizedOfferDao,
        mock[GoodsDao]
      )
      val activateTask = new ActivateOffersByQuota(
        quotaService,
        jdbcQuotaOffersService,
        categorizedOfferDao,
        clientDao,
        epochService,
        testMarker,
        OfferCategories.Moto
      )
      val clientId = 27200
      val categorizedClientId = 27112
      val quota = genQuotas(ProductId.QuotaPlacementMoto, clientId).head

      val offers = categorizedOfferDao
        .get(ForIdAndCategory(2505564, OfferCategories.Motorcycle))
        .success
        .value
      offers should have size 1
      val offer = offers.head
      offer.status shouldBe OfferStatuses.WaitingActivation

      val client = genClients().toList.head.copy(
        clientId = clientId,
        categorizedClientId = Some(categorizedClientId),
        status = ClientStatuses.Active
      )
      whenEpochServiceGet(testMarker, Success(0))
      whenOfferDaoGet(testEpoch, NonEmptyList.one(client), Success(offers))
      whenClientQuotasGet(testEpoch, quota, clientId)
      when(clientSource.getByIdOrFail(clientId, withDeleted = false)).thenReturnZ(client)
      whenCategorizedClientDaoGet(clientId, categorizedClientId, client)
      activateTask.execute().success.value shouldBe (())

      val updatedOffers = categorizedOfferDao
        .get(ForIdAndCategory(2505564, OfferCategories.Motorcycle))
        .success
        .value
      updatedOffers should have size 1
      val updatedOffer = updatedOffers.head
      updatedOffer.status shouldBe OfferStatuses.Show
    }
  }
}

object ActivateOffersByQuotaSpec {

  val testEpoch = 1234L
  val testFailedQuotaClientId = 111111L
  val testMarker = Markers.ActivateOffersByQuotaEpoch
  val testCategory = OfferCategories.Cars

  def genClients() =
    Gen
      .listOfN(5, ClientRecordGen)
      .next
      .toNel
      .getOrElse(throw new RuntimeException("Unexpected empty list"))

  def genOffers(
      size: Int,
      paymentGroup: PaymentGroup,
      client: Option[ClientId]
  ) =
    Gen
      .listOfN(size, OfferGen)
      .next
      .map(o =>
        o.copy(
          categoryId = paymentGroup.category.flat,
          sectionId = Gen.oneOf(paymentGroup.section.toList).next,
          clientId = client.getOrElse(o.clientId),
          status = OfferStatuses.WaitingActivation
        )
      )

  def genQuotas(quotaType: QuotaType, clientId: ClientId, size: Int = 1) =
    storedQuotaGen
      .next(size)
      .map(_.copy(quotaType = quotaType, clientId = clientId))

  trait Mocking extends BeforeAndAfter with MockitoSupport {
    this: Suite =>

    val quotaService = mock[QuotaService]
    val quotaOffersService = mock[QuotaOffersActualizer]
    val epochService = mock[EpochService]
    val offerDao = mock[OfferDao]
    val clientDao = mock[ClientDao]

    val paymentModelFactory = TestPaymentModelFactory.withoutSingleWithCalls()

    val activateTask = new ActivateOffersByQuota(
      quotaService,
      quotaOffersService,
      offerDao,
      clientDao,
      epochService,
      testMarker,
      testCategory
    )

    before {
      reset(quotaService, quotaOffersService, epochService, offerDao, clientDao)
      when(epochService.set(?, ?))
        .thenReturn(Success(()))
      when(quotaOffersService.actualize(?, ?))
        .thenReturn(Success(()))
    }

    def whenEpochServiceGet(marker: Marker, epoch: Try[Epoch]): Unit =
      stub(epochService.get _) {
        case m if m == marker.toString => epoch
      }

    def whenEpochServiceSet(
        marker: Marker,
        epoch: Epoch,
        result: Try[Unit]
    ): Unit =
      stub(epochService.set _) {
        case (m, `epoch`) if m == marker.toString => result
      }

    def whenClientDaoGet(result: zio.Task[List[Client]]): Unit =
      stub(clientDao.get _) { case ForStatusWithPoi7(Active, (_, Poi7Value("1"))) =>
        result
      }

    def whenCategorizedClientDaoGet(
        clientId: ClientId,
        categorizedClientId: ClientId,
        result: Client
    ): Unit =
      stub(clientDao.get _) {
        case ClientDao.ForCategorizedClientId(`categorizedClientId`) =>
          ZIO.succeed(List(result))
        case ForIdWithPoi7(`clientId`, (_, Poi7Value("1"))) => ZIO.succeed(Nil)
      }

    def whenQuotasGet(epoch: Epoch, quotas: Try[Iterable[StoredQuota]]): Unit =
      stub(quotaService.get(_: QuotaDao.Filter)(_: RequestContext)) {
        case (ActualActivations(_, _), _) => quotas
        case _ => Success(Iterable.empty)
      }

    def whenClientQuotasGet(
        epoch: Epoch,
        quota: StoredQuota,
        clientId: ClientId
    ): Unit =
      stub(quotaService.get(_: QuotaDao.Filter)(_: RequestContext)) {
        case (ActualActivations(_, _), _) |
            (Current(`clientId`, _, QuotaEntities.Dealer), _) =>
          Success(List(quota))
        case _ => Success(Iterable.empty)
      }

    def whenQuotaOffersActualizeFailed(clientId: ClientId): Unit =
      stub(quotaOffersService.actualize _) {
        case (_, `clientId`) => Failure(new Exception("artificial"))
        case _ => Success(())
      }

    def whenOfferDaoGet(
        epoch: Epoch,
        clients: NonEmptyList[Client],
        offers: Try[List[Offer]]
    ): Unit =
      stub(offerDao.get _) {
        case OfferDao.OfStatusSinceSetDate(
              `epoch`,
              OfferStatuses.WaitingActivation,
              _
            ) =>
          offers
      }
  }
}
