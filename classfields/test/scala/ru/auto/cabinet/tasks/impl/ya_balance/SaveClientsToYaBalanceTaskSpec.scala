package ru.auto.cabinet.tasks.impl.ya_balance

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import ru.auto.cabinet.dao.entities.{BalanceOrder, BalanceProducts}
import ru.auto.cabinet.dao.jdbc.{BalanceDao, JdbcYaBalanceRegBufferDao}
import ru.auto.cabinet.model.{BalanceId, ClientId}
import ru.auto.cabinet.model.billing.{
  CreateClientRequest,
  CreateClientResponse,
  OrdersResponse,
  OverdraftResponse
}
import ru.auto.cabinet.remote.impl.BalanceIO
import ru.auto.cabinet.service.BalanceOrderService
import ru.auto.cabinet.service.billing.VsBillingClient
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.tasks.impl.ya_balance.SaveClientsToYaBalanceTask.{
  CreateOrderBalanceError,
  NoRecordsProcessed
}
import ru.auto.cabinet.tasks.impl.ya_balance.SaveClientsToYaBalanceTaskSpec.{
  balanceOrder,
  clientId1,
  clientId2,
  defaultResponse,
  emptyDummy,
  operatorUid,
  partialBillingDummy,
  unprocessedList,
  BillingError
}
import ru.auto.cabinet.test.TestUtil.RichOngoingStub
import ru.auto.cabinet.test.{JdbcSpecTemplate, ScalamockCallHandlers}
import ru.auto.cabinet.trace.Context

import java.time.OffsetDateTime
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class SaveClientsToYaBalanceTaskSpec
    extends AsyncWordSpec
    with ScalamockCallHandlers
    with JdbcSpecTemplate {
  implicit private val rc = Context.unknown

  override protected def beforeAll(): Unit = {
    val initBalance =
      balanceHandle.init(JdbcSpecTemplate.balanceSchemaScriptPaths)
    Await.result(initBalance, 5 seconds)
  }

  override protected def afterAll(): Unit = {
    Await.result(balanceHandle.drop, 5 seconds)
  }

  implicit val instr: Instr = new EmptyInstr("test")

  private val idGen = arbitrary[Int].map(_.toLong)

  private val yaBalanceRegistrationsDao =
    new JdbcYaBalanceRegBufferDao(balanceDatabase, balanceDatabase)
  private val balanceDao = new BalanceDao(balanceDatabase, balanceDatabase)

  private def balanceOrderService(office7Id: ClientId): BalanceOrderService = {
    val b = mock[BalanceOrderService]
    when(b.getOrCreateOrder(office7Id, None)(true)(rc))
      .thenReturnF(balanceOrder)
    b
  }

  "SaveClientsToYaBalanceTask" should {

    "execute normally when there are no records to process" in {
      val task = new SaveClientsToYaBalanceTask(
        yaBalanceRegistrationsDao,
        balanceDao,
        emptyDummy,
        balanceOrderService(0),
        operatorUid
      )
      for {
        result <- task.execute
      } yield result shouldBe ()
    }

    "process records normally" in {
      val createResponse = defaultResponse.copy(id = idGen.sample)
      for {
        _ <- yaBalanceRegistrationsDao.addClients(unprocessedList.take(1))
        client1 <- balanceDao.getClient(clientId1)()
        billingDummy = partialBillingDummy {
          case CreateClientRequest(None, _, _, _, _, _, _, _, agency, _, _)
              if agency == client1.balanceAgencyId =>
            Future.successful(createResponse)
        }
        task = new SaveClientsToYaBalanceTask(
          yaBalanceRegistrationsDao,
          balanceDao,
          billingDummy,
          balanceOrderService(10),
          operatorUid
        )
        _ <- task.execute
        client1after <- balanceDao.getClient(clientId1)()
      } yield client1after.balanceClientId shouldBe createResponse.id
    }

    "not fail if at least one record was processed" in {
      for {
        _ <- yaBalanceRegistrationsDao.addClients(unprocessedList.slice(1, 3))
        client2 <- balanceDao.getClient(clientId2)()
        createResponse = defaultResponse.copy(id = idGen.sample)
        billingDummy = partialBillingDummy {
          case CreateClientRequest(None, _, _, _, _, _, _, _, agency, _, _)
              if agency == client2.balanceAgencyId =>
            Future.successful(createResponse)
          case CreateClientRequest(None, _, _, _, _, _, _, _, _, _, _) =>
            Future.failed(BillingError)
        }
        task = new SaveClientsToYaBalanceTask(
          yaBalanceRegistrationsDao,
          balanceDao,
          billingDummy,
          balanceOrderService(20),
          operatorUid
        )
        _ <- task.execute
        client2after <- balanceDao.getClient(clientId2)()
      } yield client2after.balanceClientId shouldBe createResponse.id
    }

    "fail if no records were processed" in {
      val billingDummy = partialBillingDummy { case _ =>
        Future.failed(BillingError)
      }
      val task = new SaveClientsToYaBalanceTask(
        yaBalanceRegistrationsDao,
        balanceDao,
        billingDummy,
        balanceOrderService(30),
        operatorUid
      )

      for {
        failure <- task.execute.failed
        unprocessed <- yaBalanceRegistrationsDao.getUnprocessedClients
      } yield {
        failure shouldBe NoRecordsProcessed
        unprocessed should have size 1
      }
    }

    "fail if order wasn't made" in {
      val balanceOrderService = mock[BalanceOrderService]
      when(balanceOrderService.getOrCreateOrder(any(), any())(any())(any()))
        .thenThrow(CreateOrderBalanceError(0, new Throwable))
      val createResponse = defaultResponse.copy(id = idGen.sample)
      for {
        _ <- yaBalanceRegistrationsDao.addClients(unprocessedList.slice(3, 4))
        billingDummy = partialBillingDummy { case _ =>
          Future.successful(createResponse)
        }
        task = new SaveClientsToYaBalanceTask(
          yaBalanceRegistrationsDao,
          balanceDao,
          billingDummy,
          balanceOrderService,
          operatorUid
        )
        failure <- task.execute.failed
        unprocessed <- yaBalanceRegistrationsDao.getUnprocessedClients
      } yield {
        failure shouldBe NoRecordsProcessed
        unprocessed should have size 2
      }
    }
  }
}

object SaveClientsToYaBalanceTaskSpec {
  val clientId1 = 8L
  val clientId2 = 9L
  val clientId3 = 10L
  val clientId4 = 99L
  val testUid1 = 11L
  val testUid2 = 12L
  val balanceClientId1 = 101L
  val balanceClientId2 = 102L

  val operatorUid = "111"

  val unprocessedList =
    List(clientId1, clientId2, clientId3, clientId4)

  val balanceOrder: BalanceOrder = BalanceOrder(
    id = 1,
    serviceId = BalanceIO.serviceId,
    clientId = 1,
    agencyId = 1,
    productId = BalanceProducts.internalProductId,
    quantity = 0L,
    provided = 0L,
    amount = 1L,
    dateStart = OffsetDateTime.now(),
    providedDate = OffsetDateTime.now(),
    text = "some String",
    status = 0L,
    createDate = OffsetDateTime.now(),
    updateDate = OffsetDateTime.now()
  )

  val defaultResponse: CreateClientResponse =
    CreateClientResponse(
      id = None,
      agency = None,
      name = "",
      `type` = None,
      email = None,
      phone = None,
      fax = None,
      url = None,
      agencyId = None,
      city = None,
      regionId = None
    )

  case object BillingError extends RuntimeException()

  case class BillingDummy() extends VsBillingClient {

    override def getOrders(
        dealerBalanceId: BalanceId,
        agencyBalanceId: Option[BalanceId])(implicit
        rc: Context): Future[OrdersResponse] = ???

    override def getOverdraft(dealerBalanceId: BalanceId)(implicit
        rc: Context): Future[Option[OverdraftResponse]] = ???

    override def createYaBalanceClient(req: CreateClientRequest, uid: String)(
        implicit rc: Context): Future[CreateClientResponse] = ???
  }

  val emptyDummy = BillingDummy()

  def partialBillingDummy(
      pf: PartialFunction[CreateClientRequest, Future[CreateClientResponse]]) =
    new BillingDummy() {

      override def createYaBalanceClient(req: CreateClientRequest, uid: String)(
          implicit rc: Context): Future[CreateClientResponse] =
        pf(req)
    }
}
