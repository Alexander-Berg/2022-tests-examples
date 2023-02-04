package auto.dealers.multiposting.storage.test.postgres

import common.zio.doobie.schema
import common.zio.doobie.testkit.TestPostgresql
import common.zio.logging.Logging
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import auto.dealers.multiposting.model.{AvitoWalletOperation, AvitoWalletOperationMeta, AvitoWalletOperationServiceType, AvitoWalletOperationType, ClientId, OperationId, PaymentType}
import auto.dealers.multiposting.storage.{AvitoWalletOperationDao, AvitoWalletOperationMetaDao}
import auto.dealers.multiposting.storage.postgresql.{PgAvitoWalletOperationDao, PgAvitoWalletOperationMetaDao}
import auto.dealers.multiposting.storage.testkit.gen.GenAvitoWalletOperation
import zio.{Has, RIO, Task, ZIO}
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.interop.catz._

import java.time.{OffsetDateTime, ZoneId}

object PgAvitoWalletOperationMetaDaoSpec extends DefaultRunnableSpec {
  import auto.dealers.multiposting.storage.testkit.AvitoWalletOperationMetaDaoHelper._

  val ActiveDays = 5

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PgAvitoWalletOperationMetaDao")(
      listOfFilteredByFromToIntersectThreePeriods,
      listOfFilteredByFromToIntersectTwoPeriods,
      listOfFilteredByFromToIntersectOnePeriod,
      listOfFilteredByFromToIntersectOnePeriodWithCorners,
      refundOperation,
      processOperationInsertNothing,
      processPlacementOperations,
      processPlacementOperationsCoverLastDay,
      processPlacementOperationsNotCoverLastDay,
      processPlacementOpsWhichBoughtRefundedAndBoughtTariffAtTheSameDay,
      processPlacementOpsWhichBoughtRefundedAndOnTheNextDayBoughtTariff,
      processPlacementOpsWhichBoughtRefundedAndBoughtServiceAtTheSameDay,
      processPlacementOperationsWithSimultaneouslyBoughtOperations,
      processPlacementOperationsWithRefund,
      processPlacementOperationsWithRefundAndBuyNew,
      processPlacementOperationWithSkipping
    ) @@
      beforeAll(dbInitSchema) @@
      after(dbClean) @@
      sequential
  }.provideCustomLayerShared {
    (TestPostgresql.managedTransactor ++ Logging.live) >+> (PgAvitoWalletOperationMetaDao.live ++ PgAvitoWalletOperationDao.live)
  }

  private val TestClientId = ClientId(1)

  private val listOfFilteredByFromToIntersectThreePeriods =
    testM("list of filtered records by 'from' into one period, through second and 'to' into third") {
      val watchFrom = Now.minusDays(12)
      val watchTo = Now.minusDays(3)
      val dealerClientId = ClientId(1L)

      val expectedMetaOperations: List[AvitoWalletOperationMeta] = List(
        AvitoWalletOperationMeta(
          avitoWalletOperationId = OperationId("2"),
          activeFrom = CreateAtFifteenDaysAgo,
          activeTo = CreateAtFifteenDaysAgo.plusDays(ActiveDays),
          clientId = ClientId(1L),
          factAmountSumKopecks = 3000,
          paymentType = PaymentType.Tariff,
          refundAvitoWalletOperationId = None
        ),
        AvitoWalletOperationMeta(
          avitoWalletOperationId = OperationId("3"),
          activeFrom = CreateAtTenDaysAgo,
          activeTo = CreateAtTenDaysAgo.plusDays(ActiveDays),
          clientId = ClientId(1L),
          factAmountSumKopecks = 3000,
          paymentType = PaymentType.Tariff,
          refundAvitoWalletOperationId = None
        ),
        AvitoWalletOperationMeta(
          avitoWalletOperationId = OperationId("4"),
          activeFrom = CreateAtFiveDaysAgo,
          activeTo = CreateAtFiveDaysAgo.plusDays(ActiveDays),
          clientId = ClientId(1L),
          factAmountSumKopecks = 3000,
          paymentType = PaymentType.Tariff,
          refundAvitoWalletOperationId = None
        )
      )
      for {
        placementTariffOperations <- Gen
          .listOfN(OperationIdToCreatedAtToPayedSumList.size)(
            GenAvitoWalletOperation.genAvitoWalletOperationPlacementTariff
          )
          .runHead
          .map(_.get)

        (operations, operationsMeta) = getOperationsWithMetas(
          OperationIdToCreatedAtToPayedSumList,
          placementTariffOperations,
          TestClientId,
          ActiveDays,
          paymentTypeOpt = Some(PaymentType.Tariff)
        )

        res <- checkSelections(operations, operationsMeta)(
          watchFrom,
          watchTo,
          dealerClientId,
          expectedMetaOperations
        )
      } yield res
    }

  private val listOfFilteredByFromToIntersectOnePeriodWithCorners =
    testM(
      "list of filtered records by 'from' on border active_from opId = 3 and 'to' on border active_to opId = 3 "
    ) {
      val watchFrom = Now.minusDays(10)
      val watchTo = Now.minusDays(5)
      val dealerClientId = ClientId(1L)

      val expectedMetaOperations: List[AvitoWalletOperationMeta] = List(
        AvitoWalletOperationMeta(
          avitoWalletOperationId = OperationId("3"),
          activeFrom = CreateAtTenDaysAgo,
          activeTo = CreateAtTenDaysAgo.plusDays(ActiveDays),
          clientId = ClientId(1L),
          factAmountSumKopecks = 3000,
          paymentType = PaymentType.Tariff,
          refundAvitoWalletOperationId = None
        )
      )
      for {
        placementTariffOperations <- Gen
          .listOfN(OperationIdToCreatedAtToPayedSumList.size)(
            GenAvitoWalletOperation.genAvitoWalletOperationPlacementTariff
          )
          .runHead
          .map(_.get)

        (operations, operationsMeta) = getOperationsWithMetas(
          OperationIdToCreatedAtToPayedSumList,
          placementTariffOperations,
          TestClientId,
          ActiveDays,
          paymentTypeOpt = Some(PaymentType.Tariff)
        )

        res <- checkSelections(operations, operationsMeta)(
          watchFrom,
          watchTo,
          dealerClientId,
          expectedMetaOperations
        )
      } yield res
    }

  private val listOfFilteredByFromToIntersectTwoPeriods =
    testM("list of filtered records by 'from' into one period and 'to' into another period") {
      val watchFrom = Now.minusDays(12)
      val watchTo = Now.minusDays(7)
      val dealerClientId = ClientId(1L)

      val expectedMetaOperations = List(
        AvitoWalletOperationMeta(
          avitoWalletOperationId = OperationId("2"),
          activeFrom = CreateAtFifteenDaysAgo,
          activeTo = CreateAtFifteenDaysAgo.plusDays(ActiveDays),
          clientId = ClientId(1L),
          factAmountSumKopecks = 3000,
          paymentType = PaymentType.Tariff,
          refundAvitoWalletOperationId = None
        ),
        AvitoWalletOperationMeta(
          avitoWalletOperationId = OperationId("3"),
          activeFrom = CreateAtTenDaysAgo,
          activeTo = CreateAtTenDaysAgo.plusDays(ActiveDays),
          clientId = ClientId(1L),
          factAmountSumKopecks = 3000,
          paymentType = PaymentType.Tariff,
          refundAvitoWalletOperationId = None
        )
      )

      for {
        placementTariffOperations <- Gen
          .listOfN(OperationIdToCreatedAtToPayedSumList.size)(
            GenAvitoWalletOperation.genAvitoWalletOperationPlacementTariff
          )
          .runHead
          .map(_.get)

        (operations, operationsMeta) = getOperationsWithMetas(
          OperationIdToCreatedAtToPayedSumList,
          placementTariffOperations,
          TestClientId,
          ActiveDays,
          paymentTypeOpt = Some(PaymentType.Tariff)
        )

        res <- checkSelections(operations, operationsMeta)(
          watchFrom,
          watchTo,
          dealerClientId,
          expectedMetaOperations
        )
      } yield res
    }

  private val listOfFilteredByFromToIntersectOnePeriod =
    testM("list of intersection in one period") {
      val watchFrom = Now.minusDays(9)
      val watchTo = Now.minusDays(7)
      val dealerClientId = ClientId(1L)
      val expectedMetaOperations = List(
        AvitoWalletOperationMeta(
          avitoWalletOperationId = OperationId("3"),
          activeFrom = CreateAtTenDaysAgo,
          activeTo = CreateAtTenDaysAgo.plusDays(ActiveDays),
          clientId = ClientId(1L),
          factAmountSumKopecks = 3000,
          paymentType = PaymentType.Tariff,
          refundAvitoWalletOperationId = None
        )
      )
      for {
        placementTariffOperations <- Gen
          .listOfN(OperationIdToCreatedAtToPayedSumList.size)(
            GenAvitoWalletOperation.genAvitoWalletOperationPlacementTariff
          )
          .runHead
          .map(_.get)

        (operations, operationsMeta) = getOperationsWithMetas(
          OperationIdToCreatedAtToPayedSumList,
          placementTariffOperations,
          TestClientId,
          ActiveDays,
          paymentTypeOpt = Some(PaymentType.Tariff)
        )

        res <- checkSelections(operations, operationsMeta)(
          watchFrom,
          watchTo,
          dealerClientId,
          expectedMetaOperations
        )
      } yield res
    }

  private val refundOperation = testM("refund operation") {

    val activeDays = 30

    for {
      placementTariffOperations <- Gen
        .listOfN(OperationIdToCreatedAtToPayedSumList.size)(
          GenAvitoWalletOperation.genAvitoWalletOperationPlacementTariff
        )
        .runHead
        .map(_.get)

      (_, operationsMeta) = getOperationsWithMetas(
        OperationIdToCreatedAtToPayedSumList,
        placementTariffOperations,
        TestClientId,
        activeDays,
        paymentTypeOpt = Some(PaymentType.Tariff),
        closeDate = Some(OffsetDateTime.now().plusDays(10))
      )

      refundSum = -50000
      (restOfRefundedSum, updatedOperationsMeta) = PgAvitoWalletOperationMetaDao.proportionsRefundAndUpdate(
        refundOperations = operationsMeta,
        refundSum = refundSum,
        refundDate = Now
      )

      isUpdatedListContainsExpectedSum = updatedOperationsMeta.zip(operationsMeta).forall {
        case (updatedOperation, oldOperation) =>
          (updatedOperation.factAmountSumKopecks + 10000) == oldOperation.factAmountSumKopecks
      }

    } yield assert(updatedOperationsMeta.map(_.activeTo))(not(hasSameElements(operationsMeta.map(_.activeTo)))) &&
      assertTrue(isUpdatedListContainsExpectedSum) &&
      assert(restOfRefundedSum)(equalTo(0L))
  }

  private val processOperationInsertNothing = testM("processed operation nothing to insert or update") {
    val activeDays = 30

    for {
      operations <- Gen
        .listOfN(OperationIdToCreatedAtToPayedSumList.size) {
          GenAvitoWalletOperation.genAvitoWalletNotPlacementOperation
        }
        .runHead
        .map(_.get)
      (updatedOperations, operationMeta) = getOperationsWithMetas(
        OperationIdToCreatedAtToPayedSumList,
        operations,
        TestClientId,
        activeDays,
        closeDate = Some(OffsetDateTime.now().plusDays(10))
      )

      _ <- AvitoWalletOperationDao.insertNonExistingBatch(updatedOperations)

      _ <- AvitoWalletOperationMetaDao.processMetaOperations(operationMeta)
      res <- selectAll
    } yield assertTrue(res.isEmpty)
  }

  private val processPlacementOperations = testM("process placement operations") {
    val (operations, metaOperations) = operationsWithOperationMeta.unzip
    for {
      _ <- AvitoWalletOperationDao.insertNonExistingBatch(operations)
      _ <- AvitoWalletOperationMetaDao.processMetaOperations(metaOperations)
      res <- selectAll
    } yield assert(res.map(_.avitoWalletOperationId))(
      hasSameElements(metaOperations.map(_.avitoWalletOperationId))
    )
  }

  private val processPlacementOperationsCoverLastDay = testM("process placement operations with covering last day active") {
    val (operations, metaOperations) = {
      val TariffWithTariffMeta@(_, tariffMeta) = getTariffOperationWithTariffInfo(
        operationId = "1",
        createdAt = CreateAtTwentyDaysAgo,
        amount = 30000,
        isActive = true,
        activeDays = 5
      )

      List(
        TariffWithTariffMeta,
        getServiceOperation(
          operationId = "2",
          createdAt = CreateAtFifteenDaysAgo.minusDays(1).minusMinutes(1),
          amount = 30000,
          activeTo = tariffMeta.activeTo
        )
      ).unzip
    }
    for {
      _ <- AvitoWalletOperationDao.insertNonExistingBatch(operations)
      _ <- AvitoWalletOperationMetaDao.processMetaOperations(metaOperations)
      res <- selectAll
    } yield assert(res.map(_.avitoWalletOperationId))(
      hasSameElements(metaOperations.map(_.avitoWalletOperationId))
    )
  }

  private val processPlacementOperationsNotCoverLastDay = testM("process placement operations without covering last day active") {
    val (operations, metaOperations) = {
      val TariffWithTariffMeta@(_, tariffMeta) = getTariffOperationWithTariffInfo(
        operationId = "1",
        createdAt = CreateAtTwentyDaysAgo,
        amount = 30000,
        isActive = true,
        activeDays = 5
      )

      List(
        TariffWithTariffMeta,
        getServiceOperation(
          operationId = "2",
          createdAt = CreateAtFifteenDaysAgo,
          amount = 30000,
          activeTo = tariffMeta.activeTo
        )
      ).unzip
    }

    val expectedOperationIds = List(
      getTariffOperationWithTariffInfo(
        operationId = "1",
        createdAt = CreateAtTwentyDaysAgo,
        amount = 30000,
        isActive = true,
        activeDays = 5
      )
    )

    for {
      _ <- AvitoWalletOperationDao.insertNonExistingBatch(operations)
      _ <- AvitoWalletOperationMetaDao.processMetaOperations(metaOperations)
      res <- selectAll
    } yield assert(res.map(_.avitoWalletOperationId))(
      hasSameElements(expectedOperationIds.map{ case(_, meta) => meta.avitoWalletOperationId })
    )
  }

  private val processPlacementOpsWhichBoughtRefundedAndBoughtTariffAtTheSameDay =
    testM("process placement operations which bought Tariff refunded and bought another at the same date") {
      val boughtOperations = List(
        getTariffOperationWithTariffInfo(
          operationId = "1",
          createdAt = CreateAtTwentyDaysAgo,
          amount = 30000,
          isActive = true,
          activeDays = 5
        ),
        getRefundTariffOperation(operationId = "2", createdAt = CreateAtTwentyDaysAgo, amount = -30000),
        getTariffOperationWithTariffInfo(
          operationId = "3",
          createdAt = CreateAtTwentyDaysAgo.plusMinutes(1),
          amount = 30000,
          isActive = true,
          activeDays = 5
        )
      )

      val expectedOps = {
        val (tariff, tariffMeta) = getTariffOperationWithTariffInfo(
          operationId = "1",
          createdAt = CreateAtTwentyDaysAgo,
          amount = 30000,
          isActive = true,
          activeDays = 5
        )
        List(
          (tariff, tariffMeta.copy(factAmountSumKopecks = 0, refundAvitoWalletOperationId = Some(OperationId("2")), activeTo = CreateAtTwentyDaysAgo)),
          getTariffOperationWithTariffInfo(
            operationId = "3",
            createdAt = CreateAtTwentyDaysAgo.plusMinutes(1),
            amount = 30000,
            isActive = true,
            activeDays = 5
          )
        )
      }

      val (operations, metaOperations) = boughtOperations.unzip

      for {
        _ <- AvitoWalletOperationDao.insertNonExistingBatch(operations)
        _ <- AvitoWalletOperationMetaDao.processMetaOperations(metaOperations)
        res <- selectAll
      } yield assert(res.map(op => (op.avitoWalletOperationId, op.refundAvitoWalletOperationId, op.factAmountSumKopecks)))(
        hasSameElements(expectedOps.map { case (_, meta) =>
            (meta.avitoWalletOperationId, meta.refundAvitoWalletOperationId, meta.factAmountSumKopecks)
          }
        )
      )
    }

  private val processPlacementOpsWhichBoughtRefundedAndOnTheNextDayBoughtTariff =
    testM("process placement operations which bought Tariff refunded and on the next day bought another") {
      val boughtOperations = List(
        getTariffOperationWithTariffInfo(
          operationId = "1",
          createdAt = CreateAtTwentyDaysAgo,
          amount = 30000,
          isActive = true,
          activeDays = 5
        ),
        getRefundTariffOperation(operationId = "2", createdAt = CreateAtTwentyDaysAgo, amount = -30000),
        getTariffOperationWithTariffInfo(
          operationId = "3",
          createdAt = CreateAtTwentyDaysAgo.plusDays(1),
          amount = 30000,
          isActive = true,
          activeDays = 5
        )
      )

      val (_, expectMetaOperations) = {
        val (tariff, tariffMeta) = getTariffOperationWithTariffInfo(
          operationId = "1",
          createdAt = CreateAtTwentyDaysAgo.atZoneSimilarLocal(ZoneId.of("UTC")).toOffsetDateTime,
          amount = 30000,
          isActive = true,
          activeDays = 5
        )
        List(
          (tariff, tariffMeta.copy(factAmountSumKopecks = 0, refundAvitoWalletOperationId = Some(OperationId("2")), activeTo = CreateAtTwentyDaysAgo)),
          getTariffOperationWithTariffInfo(
            operationId = "3",
            createdAt = CreateAtTwentyDaysAgo.plusDays(1),
            amount = 30000,
            isActive = true,
            activeDays = 5
          )
        )
      }.unzip
      val (operations, metaOperations) = boughtOperations.unzip

      for {
        _ <- AvitoWalletOperationDao.insertNonExistingBatch(operations)
        _ <- AvitoWalletOperationMetaDao.processMetaOperations(metaOperations)
        res <- selectAll
      } yield assert(res.map(op => (op.avitoWalletOperationId, op.refundAvitoWalletOperationId, op.factAmountSumKopecks)))(
          hasSameElements(expectMetaOperations.map {  meta =>
            (meta.avitoWalletOperationId, meta.refundAvitoWalletOperationId, meta.factAmountSumKopecks)
          }
        )
      )
    }

  private val processPlacementOpsWhichBoughtRefundedAndBoughtServiceAtTheSameDay =
    testM("process placement operations which bought Service refunded and bought another at the same date") {
      val boughtOperations = {
        val (tariff, tariffMeta) =
          getTariffOperationWithTariffInfo(
            operationId = "1",
            createdAt = CreateAtTwentyDaysAgo,
            amount = 30000,
            isActive = true,
            activeDays = 5
          )
        List(
          (tariff, tariffMeta),
          getServiceOperation(
            operationId = "2",
            createdAt = CreateAtTwentyDaysAgo,
            activeTo = tariffMeta.activeTo,
            amount = 30000
          ),
          getRefundServiceOperation(operationId = "3", createdAt = CreateAtTwentyDaysAgo.plusMinutes(1), amount = -30000),
          getServiceOperation(
            operationId = "4",
            createdAt = CreateAtTwentyDaysAgo.plusMinutes(2),
            activeTo = tariffMeta.activeTo,
            amount = 30000
          )
        )
      }

      val (_, expectMetaOperations) = {
        val (tariff, tariffMeta) = getTariffOperationWithTariffInfo(
          operationId = "1",
          createdAt = CreateAtTwentyDaysAgo,
          amount = 30000,
          isActive = true,
          activeDays = 5
        )

        val (service, serviceMeta) = getServiceOperation(
          operationId = "2",
          createdAt = CreateAtTwentyDaysAgo,
          activeTo = tariffMeta.activeTo,
          amount = 30000
        )
        List(
          (tariff, tariffMeta),
          (service, serviceMeta.copy(factAmountSumKopecks = 0, refundAvitoWalletOperationId = Some(OperationId("3")), activeTo = CreateAtTwentyDaysAgo.plusMinutes(1))),
          getServiceOperation(
            operationId = "4",
            createdAt = CreateAtTwentyDaysAgo.plusMinutes(2),
            activeTo = tariffMeta.activeTo,
            amount = 30000
          )
        )
      }.unzip

      val (operations, metaOperations) = boughtOperations.unzip

      for {
        _ <- AvitoWalletOperationDao.insertNonExistingBatch(operations)
        _ <- AvitoWalletOperationMetaDao.processMetaOperations(metaOperations)
        res <- selectAll
      } yield assert(res.map(op => (op.avitoWalletOperationId, op.refundAvitoWalletOperationId, op.factAmountSumKopecks)))(
        hasSameElements(expectMetaOperations.map {  meta =>
          (meta.avitoWalletOperationId, meta.refundAvitoWalletOperationId, meta.factAmountSumKopecks)
        }
        )
      )
    }

  private val processPlacementOperationsWithSimultaneouslyBoughtOperations =
    testM("process placement operations with simultaneously bought operations") {
      val simultaneouslyBoughtOperations = {
        val (tariff1, tariffMeta1) =
          getTariffOperationWithTariffInfo(
            operationId = "1",
            createdAt = CreateAtTwentyDaysAgo,
            amount = 30000,
            isActive = true,
            activeDays = 5
          )
        val (tariff2, tariffMeta2) =
          getTariffOperationWithTariffInfo(
            operationId = "3",
            createdAt = CreateAtFifteenDaysAgo,
            amount = 30000,
            isActive = true,
            activeDays = 5
          )
        List(
          (tariff1, tariffMeta1),
          getServiceOperation(
            operationId = "2",
            createdAt = CreateAtFifteenDaysAgo,
            activeTo = tariffMeta2.activeTo,
            amount = 25000
          ),
          (tariff2, tariffMeta2),
          getTariffUpgradeOperation(
            operationId = "4",
            createdAt = CreateAtFifteenDaysAgo,
            activeTo = tariffMeta2.activeTo,
            amount = 30000
          )
        )
      }

      val (operations, metaOperations) = simultaneouslyBoughtOperations.unzip

      for {
        _ <- AvitoWalletOperationDao.insertNonExistingBatch(operations)
        _ <- AvitoWalletOperationMetaDao.processMetaOperations(metaOperations)
        res <- selectAll
      } yield assert(res.map(_.avitoWalletOperationId.value))(
        hasSameElements(operations.map(_.operationId))
      )
    }

  private val processPlacementOperationsWithRefundAndBuyNew =
    testM("process placement operations with refund and buy new") {
      val refunded = List(
        getTariffOperationWithTariffInfo(
          operationId = "3",
          createdAt = Now.plusDays(1),
          amount = 10000,
          isActive = true,
          activeDays = 10
        )
      )

      val refunded2 = List(
        getTariffOperationWithTariffInfo(
          operationId = "1",
          createdAt = Now.plusMinutes(1),
          amount = 10000,
          isActive = true,
          activeDays = 10
        ),
        getRefundTariffOperation(operationId = "2", createdAt = Now.plusMinutes(2), amount = -10000),
      )

      val expectedOpsWithRefundId = List(
        (OperationId("1"), Some(OperationId("2"))),
        (OperationId("3"), None)
      )
      val (operations, metaOperations) = refunded.unzip
      val (operations2, metaOperations2) = refunded2.unzip

      for {
        _ <- AvitoWalletOperationDao.insertNonExistingBatch(operations)
        _ <- AvitoWalletOperationMetaDao.processMetaOperations(metaOperations)
        _ <- AvitoWalletOperationDao.insertNonExistingBatch(operations2)
        _ <- AvitoWalletOperationMetaDao.processMetaOperations(metaOperations2)
        res <- selectAll
      } yield assert(res.map(r => (r.avitoWalletOperationId, r.refundAvitoWalletOperationId)))(
        hasSameElements(expectedOpsWithRefundId)
      )
    }

  private val processPlacementOperationsWithRefund = testM("process placement operations with refund") {
    val moreRefund = List(
      getTariffOperationWithTariffInfo(
        operationId = "8",
        createdAt = Now.plusMinutes(1),
        amount = 10000,
        isActive = true,
        activeDays = 10
      ),
      getRefundTariffOperation(operationId = "9", createdAt = Now.plusMinutes(2), amount = -10000)
    )

    val moreMoreRefund = List(
      getTariffOperationWithTariffInfo("10", Now.plusMinutes(3), 10000, isActive = true, 10),
      getRefundTariffOperation("11", Now.plusMinutes(3).plusDays(5), -5000)
    )

    val expectedOpsWithRefundId = List(
      (OperationId("1"), OperationId("6")),
      (OperationId("2"), OperationId("7")),
      (OperationId("3"), OperationId("6")),
      (OperationId("4"), OperationId("7")),
      (OperationId("5"), OperationId("6"))
    )

    val (operations, metaOperations) = operationsWithOperationMetaRefund.unzip
    val (moreOperations, moreMetaOperations) = moreRefund.unzip
    val (evenMoreOperations, evenMoreMetaOperations) = moreMoreRefund.unzip

    val moreExpectedOpsWithRefundId = expectedOpsWithRefundId ++ List((OperationId("8"), OperationId("9")))

    val moreMoreExpectedOpsWithRefund = moreExpectedOpsWithRefundId ++ List((OperationId("10"), OperationId("11")))
    for {
      _ <- AvitoWalletOperationDao.insertNonExistingBatch(operations)
      _ <- AvitoWalletOperationMetaDao.processMetaOperations(metaOperations)
      res <- selectAll
      _ <- AvitoWalletOperationDao.insertNonExistingBatch(moreOperations)
      _ <- AvitoWalletOperationMetaDao.processMetaOperations(moreMetaOperations)
      res2 <- selectAll
      _ <- AvitoWalletOperationDao.insertNonExistingBatch(evenMoreOperations)
      _ <- AvitoWalletOperationMetaDao.processMetaOperations(evenMoreMetaOperations)
      res3 <- selectAll
    } yield assert(res.map(r => (r.avitoWalletOperationId, r.refundAvitoWalletOperationId.get)))(
      hasSameElements(expectedOpsWithRefundId)
    ) && assert(res2.map(r => (r.avitoWalletOperationId, r.refundAvitoWalletOperationId.get)))(
      hasSameElements(moreExpectedOpsWithRefundId)
    ) && assert(res3.map(r => (r.avitoWalletOperationId, r.refundAvitoWalletOperationId.get)))(
      hasSameElements(moreMoreExpectedOpsWithRefund)
    )
  }

  private val processPlacementOperationWithSkipping =
    testM("while processing should skip sub operation if it comes before main operations") {
      val (service, metaService) =
        getServiceOperation(operationId = "1", createdAt = Now.minusDays(1), activeTo = Now.plusDays(3), amount = 5)
      val (tariff, metaTariff) = getTariffOperationWithTariffInfo(
        operationId = "2",
        createdAt = Now,
        amount = 10000,
        isActive = true,
        activeDays = 5
      )
      for {
        _ <- AvitoWalletOperationDao.insertNonExistingBatch(List(service, tariff))
        _ <- AvitoWalletOperationMetaDao.processMetaOperations(List(metaService, metaTariff))
        res <- selectAll
      } yield assert(res.map(_.avitoWalletOperationId.value))(
        hasSameElements(List(metaTariff.avitoWalletOperationId.value))
      )
    }

  private def checkSelections(
      operations: List[AvitoWalletOperation],
      operationsMeta: List[AvitoWalletOperationMeta]
    )(watchFrom: OffsetDateTime,
      watchTo: OffsetDateTime,
      clientId: ClientId,
      expectedMetaOperations: List[AvitoWalletOperationMeta]) = {

    for {
      _ <- AvitoWalletOperationDao.insertNonExistingBatch(operations)
      _ <- AvitoWalletOperationMetaDao.insertBatch(operationsMeta)
      metaOperationsIn <- selectAll

      list <- AvitoWalletOperationMetaDao.listInPeriodByClient(watchFrom, watchTo, clientId)

    } yield assert(metaOperationsIn.map(_.avitoWalletOperationId))(
      hasSameElements(operationsMeta.map(_.avitoWalletOperationId))
    ) &&
      assert(list.map(_.avitoWalletOperationId))(
        hasSameElements(expectedMetaOperations.map(_.avitoWalletOperationId))
      )
  }

  private def getOperation(
      operationId: String,
      createdAt: OffsetDateTime,
      operationType: AvitoWalletOperationType,
      operationName: String,
      serviceName: String,
      amount: Long,
      serviceType: AvitoWalletOperationServiceType = AvitoWalletOperationServiceType.Tariff) =
    AvitoWalletOperation(
      operationId = operationId,
      clientId = TestClientId.value,
      createdAt = createdAt,
      operationType = operationType.value,
      operationName = operationName,
      serviceId = Some(1),
      serviceName = Some(serviceName),
      serviceType = Some(serviceType.value),
      amountTotal = 0,
      amountRub = amount,
      amountBonus = 0,
      avitoOfferId = None,
      isPlacement = true,
      isVas = false,
      isOther = false
    )

  private def getOperationMeta(
      operation: AvitoWalletOperation,
      activeDaysOpt: Option[Int] = None,
      activeToOpt: Option[OffsetDateTime] = None): AvitoWalletOperationMeta = {
    val paymentTypeIn = AvitoWalletOperationMetaDao.getOperationPaymentType(operation)
    val activeDays: Int = activeDaysOpt.getOrElse(30)
    val defaultActiveTo: OffsetDateTime = operation.createdAt.plusDays(activeDays)
    AvitoWalletOperationMeta(
      avitoWalletOperationId = OperationId(operation.operationId),
      activeFrom = operation.createdAt,
      activeTo = activeToOpt.getOrElse(defaultActiveTo),
      clientId = ClientId(operation.clientId),
      factAmountSumKopecks = operation.amountRub.toLong,
      paymentType = paymentTypeIn,
      refundAvitoWalletOperationId = None
    )
  }

  private def getTariffOperationWithTariffInfo(
      operationId: String,
      createdAt: OffsetDateTime,
      amount: Long,
      isActive: Boolean,
      activeDays: Int): (AvitoWalletOperation, AvitoWalletOperationMeta) = {
    val operation = getOperation(
      operationId,
      createdAt,
      AvitoWalletOperationType.Reservation,
      "Покупка",
      "Размещение",
      amount
    )

    (operation, getOperationMeta(operation, Some(activeDays)))
  }

  private def getServiceOperation(
      operationId: String,
      createdAt: OffsetDateTime,
      activeTo: OffsetDateTime,
      amount: Long) = {
    val operation = getOperation(
      operationId,
      createdAt,
      AvitoWalletOperationType.Reservation,
      "Покупка",
      "Не размещение",
      amount
    )
    (operation, getOperationMeta(operation, activeToOpt = Some(activeTo)))
  }

  private def getTariffUpgradeOperation(
      operationId: String,
      createdAt: OffsetDateTime,
      activeTo: OffsetDateTime,
      amount: Long) = {
    val operation = getOperation(
      operationId,
      createdAt,
      AvitoWalletOperationType.Reservation,
      "Изменение",
      "Размещение",
      amount
    )
    (operation, getOperationMeta(operation, activeToOpt = Some(activeTo)))
  }

  private def getServiceUpgradeOperation(
      operationId: String,
      createdAt: OffsetDateTime,
      activeTo: OffsetDateTime,
      amount: Long) = {
    val operation = getOperation(
      operationId,
      createdAt,
      AvitoWalletOperationType.Reservation,
      "Изменение",
      "Не размещение",
      amount
    )
    (operation, getOperationMeta(operation, activeToOpt = Some(activeTo)))
  }

  private def getRefundTariffOperation(
      operationId: String,
      createdAt: OffsetDateTime,
      amount: Long) = {
    val operation = getOperation(
      operationId,
      createdAt,
      AvitoWalletOperationType.ReservationReturn,
      "Возврат",
      "Размещение",
      amount
    )
    (operation, getOperationMeta(operation))
  }

  private def getRefundServiceOperation(
      operationId: String,
      createdAt: OffsetDateTime,
      amount: Long) = {
    val operation = getOperation(
      operationId,
      createdAt,
      AvitoWalletOperationType.ReservationReturn,
      "Возврат",
      "Не размещение",
      amount
    )
    (operation, getOperationMeta(operation))
  }

  private val operationsWithOperationMeta: List[(AvitoWalletOperation, AvitoWalletOperationMeta)] = {
    val (tariff, tariffMeta) = getTariffOperationWithTariffInfo("1", CreateAtTwentyDaysAgo, 30000, isActive = true, 30)
    List(
      (tariff, tariffMeta),
      getServiceOperation("2", CreateAtFifteenDaysAgo, tariffMeta.activeTo, 25000),
      getTariffUpgradeOperation("3", CreateAtFifteenDaysAgo.plusDays(2), tariffMeta.activeTo, 23000),
      getServiceUpgradeOperation("4", CreateAtFifteenDaysAgo.plusDays(3), tariffMeta.activeTo, 22000),
      getTariffUpgradeOperation("5", CreateAtTenDaysAgo, tariffMeta.activeTo, 20000)
    )
  }

  private val operationsWithOperationMetaRefund = operationsWithOperationMeta ++
    List(
      getRefundTariffOperation("6", Now, -30000),
      getRefundServiceOperation("7", Now, -20000)
    )

  private[storage] val selectAll: RIO[Has[Transactor[Task]], List[AvitoWalletOperationMeta]] = {
    for {
      xa <- ZIO.service[Transactor[Task]]
      res <- sql"select * from avito_wallet_operation_meta"
        .query[AvitoWalletOperationMeta]
        .to[List]
        .transact(xa)
    } yield res
  }

  private[storage] val dbInitSchema = ZIO
    .service[Transactor[Task]]
    .flatMap(schema.InitSchema("/schema.sql", _))
    .orDie

  private[storage] val dbClean =
    for {
      xa <- ZIO.service[Transactor[Task]]
      res <- sql"""
          | truncate table avito_wallet_operation cascade;
          | truncate table avito_wallet_operation_meta;
           """.stripMargin.update.run
        .transact(xa)
        .unit
    } yield res
}
