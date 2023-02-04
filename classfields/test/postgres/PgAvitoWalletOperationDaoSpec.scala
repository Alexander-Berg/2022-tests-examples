package auto.dealers.multiposting.storage.test.postgres

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import cats.data.NonEmptySet
import common.zio.doobie.schema
import common.zio.doobie.testkit.TestPostgresql
import common.zio.logging.Logging
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import auto.dealers.multiposting.model.{
  AvitoDayExpenses,
  AvitoWalletOperation,
  AvitoWalletOperationMeta,
  ClientId,
  OperationId,
  PaymentType
}
import auto.dealers.multiposting.storage.{AvitoWalletOperationDao, AvitoWalletOperationMetaDao}
import auto.dealers.multiposting.storage.postgresql.{PgAvitoWalletOperationDao, PgAvitoWalletOperationMetaDao}
import auto.dealers.multiposting.storage.testkit.gen.GenAvitoWalletOperation
import zio.{Has, RIO, Task, ZIO}
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.interop.catz._

object PgAvitoWalletOperationDaoSpec extends DefaultRunnableSpec {
  // nb postgres will save time into UTC even for timestampz
  val now = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS).plusHours(1)
  val clientId = 1L

  val record = AvitoWalletOperation(
    operationId = "1",
    clientId = clientId,
    createdAt = now,
    operationType = "operationType",
    operationName = "operationName",
    serviceId = Some(1),
    serviceName = Some("serviceName"),
    serviceType = Some("serviceType"),
    amountTotal = BigDecimal(10),
    amountRub = BigDecimal(10),
    amountBonus = BigDecimal(10),
    avitoOfferId = Some(1),
    isPlacement = false,
    isVas = true,
    isOther = false
  )

  val upsertNothingCheck = testM("insert should do nothing on operation_id conflict") {
    for {
      emptyResult <- selectAll
      _ <- AvitoWalletOperationDao.insertNonExisting(record)
      firstResult <- selectAll
      _ <- AvitoWalletOperationDao.insertNonExisting(record.copy(amountTotal = 2L))
      secondResult <- selectAll
    } yield {
      assert(emptyResult.isEmpty)(equalTo(true)) &&
      assert(firstResult)(equalTo(secondResult))
    }
  }

  val upsertBatchNothingCheck = testM("batch insert should do nothing on operation_id conflict") {
    for {
      emptyResult <- selectAll
      records = (1 to 20)
        .map(n => record.copy(operationId = s"$n", createdAt = now.plusHours(n)))
      _ <- AvitoWalletOperationDao.insertNonExistingBatch(records)
      firstResult <- selectAll
      recordsChanged = (1 to 20)
        .map(n => record.copy(operationId = s"$n", amountTotal = 2L, createdAt = now.plusHours(n)))
      _ <- AvitoWalletOperationDao.insertNonExistingBatch(recordsChanged)
      secondResult <- selectAll
    } yield {
      assert(emptyResult.isEmpty)(equalTo(true)) &&
      assert(firstResult)(equalTo(secondResult)) &&
      assert(secondResult)(equalTo(records))
    }
  }

  val batchLastOperationTsCheck = testM("batch findLastOperationTimestamp") {
    for {
      noRes <- AvitoWalletOperationDao.findLastOperationTimestamps(NonEmptySet.one(clientId))
      records = (1 to 20)
        .map(n => record.copy(clientId = n, operationId = s"$n", createdAt = now.plusHours(n)))

      _ <- ZIO.foreach_(records)(AvitoWalletOperationDao.insertNonExisting)

      ids = {
        val clientIds = records.map(_.clientId).toList
        NonEmptySet.of(clientIds.head, clientIds.tail: _*)
      }
      expected = records.map(op => (op.clientId, op.createdAt)).toMap
      lastRes <- AvitoWalletOperationDao.findLastOperationTimestamps(ids)
    } yield {
      assert(noRes)(equalTo(Map.empty[Long, OffsetDateTime])) &&
      assert(lastRes)(equalTo(expected))
    }
  }

  val perDayExpensesCheck = testM("getPerDayExpenses") {
    val records = (1 to 20)
      .map(n =>
        record.copy(operationId = s"$n", createdAt = now.plusMinutes(n), isPlacement = n % 2 == 0, isVas = n % 2 == 1)
      )
      .toList

    val from = now.plusHours(0)
    val to = now.plusHours(2)

    def op = AvitoWalletOperationDao.getPerDayExpenses(
      clientId = clientId,
      from = from,
      to = to,
      exceptOperationIds = List.empty[String]
    )

    def isInRange(t: OffsetDateTime) =
      t.toEpochSecond >= from.toEpochSecond &&
        t.toEpochSecond <= to.toEpochSecond

    for {
      _ <- ZIO.foreach_(records)(AvitoWalletOperationDao.insertNonExisting)
      res <- op
    } yield {

      val perDayRecords = records
        .filter(r => isInRange(r.createdAt))
        .groupBy(_.createdAt.truncatedTo(ChronoUnit.DAYS))
        .map { case (day, group) =>
          val (vasSum, placementSum) =
            group.foldLeft((BigDecimal(0), BigDecimal(0))) { case ((vSum, pSum), op) =>
              (op.isVas, op.isPlacement) match {
                case (true, _) => (vSum + op.amountRub, pSum)
                case (_, true) => (vSum, pSum + op.amountRub)
                case _ => (vSum, pSum)
              }
            }
          AvitoDayExpenses(day, placementSum, vasSum, BigDecimal(0))
        }
        .toList
      assert(res)(equalTo(perDayRecords))

    }
  }

  val listNotProcessedOperations = testM("return list of unprocessed operations") {
    val operationsAmount = 20
    for {
      operations <- Gen
        .listOfN(operationsAmount)(GenAvitoWalletOperation.genAvitoWalletOperation)
        .runHead
        .map(_.get)

      exceptOperations <- Gen
        .listOfN(operationsAmount)(GenAvitoWalletOperation.genAvitoWalletOperation)
        .runHead
        .map(_.get)

      refundTariff <- GenAvitoWalletOperation.genAvitoWalletOperationPlacementTariff.runHead.map(_.get)

      exceptOpsUpdate = exceptOperations.zipWithIndex.map { case (op, n) => op.copy(createdAt = now.plusDays(n)) }

      (ops, metaOps) = getOperationsWithOperationMetas(operations)

      updatedMetaOps = metaOps.map(_.copy(refundAvitoWalletOperationId = Some(OperationId(refundTariff.operationId))))

      allOperations = ops ++ exceptOpsUpdate :+ refundTariff

      _ <- AvitoWalletOperationDao.insertNonExistingBatch(allOperations)
      _ <- AvitoWalletOperationMetaDao.insertBatch(updatedMetaOps)
      extOps <- AvitoWalletOperationDao.listUnprocessedOperations(
        AvitoWalletOperationMetaDao.DepthOfCheckingOperationDays
      )
    } yield assert(extOps.map(_.operationId))(hasSameElements(exceptOpsUpdate.map(_.operationId)))
  }

  private def getOperationsWithOperationMetas(
      avitoWalletOperations: List[
        AvitoWalletOperation
      ]): (List[AvitoWalletOperation], List[AvitoWalletOperationMeta]) = {
    val activeDays = 30

    avitoWalletOperations.zipWithIndex
      .foldLeft((List.empty[AvitoWalletOperation], List.empty[AvitoWalletOperationMeta])) {
        case ((operationsAcc, operationsMetaAcc), (operation, n)) =>
          val updatedOperation = operation.copy(createdAt = now.plusDays(n))
          val operationMeta = AvitoWalletOperationMeta(
            avitoWalletOperationId = OperationId(updatedOperation.operationId),
            activeFrom = updatedOperation.createdAt,
            activeTo = updatedOperation.createdAt.plusDays(activeDays),
            clientId = ClientId(updatedOperation.clientId),
            factAmountSumKopecks = updatedOperation.amountRub.toLongExact,
            paymentType = PaymentType.Tariff,
            refundAvitoWalletOperationId = None
          )
          (updatedOperation :: operationsAcc, operationMeta :: operationsMetaAcc)
      }
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PgAvitoWalletOperationDao")(
      upsertNothingCheck,
      upsertBatchNothingCheck,
      batchLastOperationTsCheck,
      perDayExpensesCheck,
      listNotProcessedOperations
    ) @@
      beforeAll(initSchema) @@
      after(clean) @@
      sequential
  }.provideCustomLayerShared(
    (TestPostgresql.managedTransactor ++ Logging.live) >+> (PgAvitoWalletOperationMetaDao.live ++ PgAvitoWalletOperationDao.live)
  )

  private val initSchema: RIO[Has[Transactor[Task]], Unit] = for {
    xa <- ZIO.service[Transactor[Task]]
    _ <- schema.InitSchema("/schema.sql", xa)
  } yield {}

  private val selectAll: RIO[Has[Transactor[Task]], Seq[AvitoWalletOperation]] =
    for {
      xa <- ZIO.service[Transactor[Task]]
      res <- sql"""select * from avito_wallet_operation;"""
        .query[AvitoWalletOperation]
        .to[Seq]
        .transact(xa)
    } yield res

  private val clean: RIO[Has[Transactor[Task]], Unit] =
    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- sql"""
           delete from avito_wallet_operation_meta;
           delete from avito_wallet_operation;""".update.run.transact(xa).unit
    } yield {}
}
