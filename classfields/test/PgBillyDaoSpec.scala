package ru.yandex.vertis.billing.billy

import ru.yandex.vertis.billing.billy.model._
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import common.zio.ops.tracing.testkit.TestTracing
import zio.test.Assertion.equalTo
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test.{assert, assertTrue, DefaultRunnableSpec, ZSpec}
import zio._
import zio.interop.catz._
import doobie._
import doobie.implicits._
import ru.yandex.vertis.billing.billy.dao.{
  BalanceMessageVersionDao,
  ClientDao,
  InvoiceDao,
  OrderDao,
  RequisitesDao,
  SpendingDao
}
import ru.yandex.vertis.billing.billy.model.error.AlreadyExist
import zio.test.environment.TestEnvironment
import zio.test.Assertion._

import java.time.Instant
import scala.annotation.tailrec

object PgBillyDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    val suit = suite("PgBillyDaoSpec")(
      simpleCreateClient,
      simpleUpdateClient,
      simpleCreateRequisitesWithPayerId,
      simpleCreateRequisitesWithError,
      simpleGetNonExistentRequisites,
      simpleCreateRequisitesWithContractId,
      simpleUpdateRequisitesWithPayerId,
      simpleUpdateRequisitesWithContractId,
      updateRequisites,
      createAndUpsertInvoice,
      createOrder,
      createSpending,
      createSpendingWithoutClient,
      getSpendingByTimeRange,
      createDuplicateSpending,
      insertBalanceMessageVersion,
      updateBalanceMessageVersion,
      cantUpdateNewerBalanceMessageVersion
    ) @@ beforeAll(dbInit) @@ after(dbClean) @@ sequential

    suit.provideCustomLayerShared(
      TestPostgresql.managedTransactor ++ TestTracing.noOp >+>
        (ClientDao.live ++ RequisitesDao.live ++ InvoiceDao.live ++ OrderDao.live ++ SpendingDao.live ++ BalanceMessageVersionDao.live)
    )
  }

  // мб кинуть это в коммонсы?
  private def collectMigrations(resourcesPath: String = "sql"): List[String] = {
    @tailrec
    def helper(acc: List[String] = Nil, num: Int = 0): List[String] = {
      val filepath = s"/$resourcesPath/v$num.sql"
      val t = getClass.getResource(filepath)
      if (t != null) { // о да, Э Ф П Э
        helper(acc :+ filepath, num + 1)
      } else acc
    }
    helper()
  }

  private def dbInit: URIO[Has[Transactor[Task]], Unit] = {
    implicit val ordering: Ordering[String] = Ordering.by[String, Int] {
      // v0.sql, v1.sql, v2.sql ... v99...99.sql
      case x if x.startsWith("/sql/v") => x.drop(6).takeWhile(_ != '.').toInt
    }
    ZIO
      .service[Transactor[Task]]
      .flatMap(InitSchema.applyMany(collectMigrations(), _))
      .orDie
  }

  private def dbClean: RIO[Has[Transactor[Task]], Unit] = ZIO.serviceWith[Transactor[Task]](xa =>
    (for {
      _ <- sql"DELETE FROM spending".update.run
      _ <- sql"DELETE FROM invoice".update.run
      _ <- sql"DELETE FROM actual_order".update.run
      _ <- sql"DELETE FROM actual_requisites".update.run
      _ <- sql"DELETE FROM client".update.run
    } yield ()).transact(xa)
  )

  private val client1 = Client(1, "scId1", "name1", "phone1", "mail1", "ws1")
  private val client2 = Client(2, "scId2", "name2", "phone2", "mail2", "ws2")
  private val client3 = Client(3, "scId3", "name3", "phone3", "mail3", "ws3")
  private val requisitesWithPayerId1 = Requisites(1, Some("13"), None)
  private val requisitesWithContractId1 = Requisites(1, None, Some("13"))
  private val fullRequisites = Requisites(1, Some("13"), Some("13"))
  private val invoice1 = Invoice("ws1", 1, "service_id", "1234", None)
  private val spending1 = Spending("1", client1.workspace, client1.balanceId, 1000, Instant.now())
  private val spending2 = Spending("2", client1.workspace, client1.balanceId, 1000, Instant.now())
  private val spending3 = Spending("3", client1.workspace, client1.balanceId, 1000, Instant.now())

  private def simpleCreateClient = testM("creating client should be persisted") {
    for {
      repo <- ZIO.service[ClientDao]
      _ <- repo.upsertClient(client1)
      _ <- repo.upsertClient(client2)
      _ <- repo.upsertClient(client3)
      persistedClient <- repo.getClient(client1.serviceClientId, client1.workspace)
    } yield assert(persistedClient)(equalTo(Some(client1)))
  }

  private def simpleUpdateClient = testM("updates client must be applied") {
    for {
      repo <- ZIO.service[ClientDao]
      _ <- repo.upsertClient(client1)
      _ <- repo.upsertClient(client1.copy(email = "newEmail", phone = "newPhone"))
      persistedClient <- repo.getClient(client1.serviceClientId, client1.workspace)
    } yield assert(persistedClient.map(_.email))(equalTo(Some("newEmail"))) &&
      assert(persistedClient.map(_.phone))(equalTo(Some("newPhone")))
  }

  private def simpleCreateRequisitesWithError =
    testM("create requisites with error. Client should be create before requisites") {
      for {
        requisitesDao <- ZIO.service[RequisitesDao]
        throwable <- requisitesDao
          .upsertPayerId(requisitesWithPayerId1)
          .fold(fail => Left(fail), success => Right(success))
      } yield assert(throwable)(isLeft)
    }

  private def simpleGetNonExistentRequisites = testM("not found requisites") {
    for {
      requisitesDao <- ZIO.service[RequisitesDao]
      requisites <- requisitesDao.getRequisitesBy(requisitesWithPayerId1.balanceId)
    } yield assert(requisites)(isNone)
  }

  private def simpleCreateRequisitesWithPayerId = testM("create requisites with payer id") {
    for {
      clientDao <- ZIO.service[ClientDao]
      requisitesDao <- ZIO.service[RequisitesDao]
      _ <- clientDao.upsertClient(client1)
      _ <- requisitesDao.upsertPayerId(requisitesWithPayerId1)
      requisites <- requisitesDao.getRequisitesBy(requisitesWithPayerId1.balanceId)
    } yield assert(requisites)(equalTo(Some(requisitesWithPayerId1)))
  }

  private def simpleUpdateRequisitesWithPayerId = testM("update requisites with payer id") {
    for {
      clientDao <- ZIO.service[ClientDao]
      requisitesDao <- ZIO.service[RequisitesDao]
      _ <- clientDao.upsertClient(client1)
      _ <- requisitesDao.upsertPayerId(requisitesWithPayerId1)
      newRequisites = requisitesWithPayerId1.copy(payerId = Some("155"))
      _ <- requisitesDao.upsertPayerId(newRequisites)
      requisites <- requisitesDao.getRequisitesBy(requisitesWithPayerId1.balanceId)
    } yield assert(requisites)(equalTo(Some(newRequisites)))
  }

  private def simpleCreateRequisitesWithContractId = testM("create requisites with contract id") {
    for {
      clientDao <- ZIO.service[ClientDao]
      requisitesDao <- ZIO.service[RequisitesDao]
      _ <- clientDao.upsertClient(client1)
      _ <- requisitesDao.upsertContractId(requisitesWithContractId1)
      requisites <- requisitesDao.getRequisitesBy(requisitesWithContractId1.balanceId)
    } yield assert(requisites)(equalTo(Some(requisitesWithContractId1)))
  }

  private def simpleUpdateRequisitesWithContractId = testM("update requisites with contract id") {
    for {
      clientDao <- ZIO.service[ClientDao]
      requisitesDao <- ZIO.service[RequisitesDao]
      _ <- clientDao.upsertClient(client1)
      _ <- requisitesDao.upsertContractId(requisitesWithContractId1)
      newRequisites = requisitesWithContractId1.copy(contractId = Some("1000000"))
      _ <- requisitesDao.upsertContractId(newRequisites)
      requisites <- requisitesDao.getRequisitesBy(requisitesWithContractId1.balanceId)
    } yield assert(requisites)(equalTo(Some(newRequisites)))
  }

  private def updateRequisites = testM("update requisites") {
    for {
      clientDao <- ZIO.service[ClientDao]
      requisitesDao <- ZIO.service[RequisitesDao]
      _ <- clientDao.upsertClient(client1)
      _ <- requisitesDao.upsertContractId(requisitesWithContractId1)
      _ <- requisitesDao.upsertPayerId(requisitesWithPayerId1)
      requisites <- requisitesDao.getRequisitesBy(requisitesWithContractId1.balanceId)
    } yield assert(requisites)(equalTo(Some(fullRequisites)))
  }

  private def createAndUpsertInvoice = testM("create and upsert invoice") {
    for {
      clientDao <- ZIO.service[ClientDao]
      invoiceDao <- ZIO.service[InvoiceDao]
      _ <- clientDao.upsertClient(client1)
      _ <- invoiceDao.upsertInvoice(invoice1)
      actualBeforeUpdate <- invoiceDao.getInvoice(invoice1.serviceInvoiceId, invoice1.workspace)
      updatedInvoice = invoice1.copy(balanceInvoiceId = Some(4321))
      _ <- invoiceDao.upsertInvoice(updatedInvoice)
      actualAfterUpdate <- invoiceDao.getInvoice(updatedInvoice.serviceInvoiceId, updatedInvoice.workspace)
    } yield {
      assert(actualBeforeUpdate)(equalTo(Some(invoice1))) &&
      assert(actualAfterUpdate)(equalTo(Some(updatedInvoice)))
    }
  }

  private def createOrder = testM("create order") {
    for {
      clientDao <- ZIO.service[ClientDao]
      orderDao <- ZIO.service[OrderDao]
      _ <- clientDao.upsertClient(client1)
      order = Order(client1.balanceId, 123, client1.workspace)
      _ <- orderDao.upsertOrder(order)
      actualOrder <- orderDao.getOrder(order.balanceClientId)
    } yield assert(actualOrder)(equalTo(Some(order)))
  }

  private def createSpending = testM("create spending") {
    for {
      clientDao <- ZIO.service[ClientDao]
      spendingDao <- ZIO.service[SpendingDao]
      _ <- clientDao.upsertClient(client1)
      _ <- spendingDao.insert(spending1)
      spending <- spendingDao.getSpendingsByCreateTime(spending1.createTime, Instant.now())
    } yield assert(spending.headOption.map(_.copy(createTime = spending1.createTime)))(equalTo(Some(spending1)))
  }

  private def createSpendingWithoutClient = testM("create spending with error. Client should be created before") {
    for {
      spendingDao <- ZIO.service[SpendingDao]
      throwable <- spendingDao
        .insert(spending1)
        .fold(fail => Left(fail), success => Right(success))
    } yield assert(throwable)(isLeft)
  }

  private def createDuplicateSpending = testM("create non unique spending with error") {
    for {
      clientDao <- ZIO.service[ClientDao]
      spendingDao <- ZIO.service[SpendingDao]
      _ <- clientDao.upsertClient(client1)
      _ <- spendingDao.insert(spending1.copy(spendingId = "13"))
      throwable <- spendingDao
        .insert(spending1.copy(spendingId = "13"))
        .fold(fail => Left(fail), success => Right(success))
    } yield {
      assert(throwable)(isLeft) && assertTrue(throwable.left.exists(_.isInstanceOf[AlreadyExist]))
    }
  }

  private def getSpendingByTimeRange = testM("get spending for certain period") {
    for {
      clientDao <- ZIO.service[ClientDao]
      spendingDao <- ZIO.service[SpendingDao]
      _ <- clientDao.upsertClient(client1)
      _ <- spendingDao.insert(spending1)
      _ <- spendingDao.insert(spending2.copy(createTime = Instant.now().plusSeconds(1)))
      _ <- spendingDao.insert(spending3.copy(createTime = Instant.now().plusSeconds(2)))
      _ <- spendingDao.insert(spending3.copy(spendingId = "4", createTime = Instant.now().plusSeconds(10)))
      spending <- spendingDao.getSpendingsByCreateTime(spending2.createTime, Instant.now().plusSeconds(2))
    } yield assert(spending.size)(equalTo(2))
  }

  private def insertBalanceMessageVersion = testM("insert balance message version") {
    for {
      messageVersionDao <- ZIO.service[BalanceMessageVersionDao]
      version <- messageVersionDao.upsertIfNewer(objectId = 1, versionId = 1)
    } yield assert(version)(equalTo(1L))
  }

  private def updateBalanceMessageVersion = testM("update balance message version") {
    for {
      messageVersionDao <- ZIO.service[BalanceMessageVersionDao]
      _ <- messageVersionDao.upsertIfNewer(objectId = 1, versionId = 1)
      version <- messageVersionDao.upsertIfNewer(objectId = 1, versionId = 2)
    } yield assert(version)(equalTo(2L))
  }

  private def cantUpdateNewerBalanceMessageVersion = testM("can't update newer balance message version") {
    for {
      messageVersionDao <- ZIO.service[BalanceMessageVersionDao]
      _ <- messageVersionDao.upsertIfNewer(objectId = 1, versionId = 2)
      version <- messageVersionDao.upsertIfNewer(objectId = 1, versionId = 1)
    } yield assert(version)(equalTo(2L))
  }
}
