package auto.dealers.multiposting.logic.test.avito

import java.time.ZonedDateTime
import common.scalapb.ScalaProtobuf
import common.zio.clock.MoscowClock
import auto.dealers.multiposting.model.{AvitoWalletOperation => AWORecord, AvitoWalletOperationServiceType, ClientId}
import ru.auto.multiposting.operation_model.AvitoWalletOperation
import auto.dealers.multiposting.storage.testkit.{AvitoWalletOperationDaoMock, AvitoWalletOperationMetaDaoMock}
import auto.dealers.multiposting.logic.avito.AvitoWalletOperationProcessor
import auto.dealers.multiposting.logic.testkit.avito.AvitoUserInfoServiceMock
import auto.dealers.multiposting.storage.AvitoWalletOperationMetaDao
import auto.dealers.multiposting.storage.testkit.gen.GenAvitoWalletOperation
import common.zio.features.Features
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import common.zio.logging.Logging
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object AvitoWalletOperationProcessorSpec extends DefaultRunnableSpec {
  import auto.dealers.multiposting.storage.testkit.AvitoWalletOperationMetaDaoHelper._

  val processingOldOperationsEnabled = "processing_old_operations_enabled"

  val ActiveDays = 30

  private val processOldOperations =
    testM("process old operations") {
      val Client = ClientId(1)
      for {
        placement <- GenAvitoWalletOperation.genAvitoWalletOperationPlacementTariff.runHead.map(_.get)
        placements = List(placement)

        (operations, operationsMeta) = getOperationsWithMetas(
          OperationIdToCreatedAtToPayedSumList,
          placements,
          Client,
          ActiveDays
        )

        mocks = AvitoWalletOperationDaoMock
          .ListUnprocessedOperations(
            equalTo(AvitoWalletOperationMetaDao.DepthOfCheckingOperationDays),
            value(operations)
          ) &&
          AvitoWalletOperationMetaDaoMock.ProcessMetaOperations(equalTo(operationsMeta), unit) &&
          AvitoWalletOperationDaoMock.InsertNonExistingBatch(equalTo(List.empty[AWORecord]), unit)

        res <- assertM(
          Features.updateFeature(processingOldOperationsEnabled, value = true) *>
            AvitoWalletOperationProcessor.process(List.empty[AvitoWalletOperation])
        )(isUnit)
          .provideCustomLayer(
            Features.liveInMemory ++ Logging.live ++ AvitoUserInfoServiceMock.empty ++ mocks >+> AvitoWalletOperationProcessor.live
          )
      } yield res
    }

  private val processNewOperations =
    testM("process new operations") {
      val Client = ClientId(1)
      for {
        placement <- GenAvitoWalletOperation.genAvitoWalletOperationPlacementTariff.runHead.map(_.get)
        placements = List(placement)

        externalOperation = AvitoWalletOperation(
          timestamp = Some(ScalaProtobuf.toTimestamp(OperationIdToCreatedAtToPayedSumList.head._2)),
          operationId = "1",
          clientId = Client.value,
          operationType = placement.operationType,
          serviceId = placement.serviceId.getOrElse(1L),
          serviceName = placement.serviceName.getOrElse("Не Размещение"),
          serviceType = placement.serviceType.getOrElse(AvitoWalletOperationServiceType.Tariff.value),
          operationName = placement.operationName,
          amountTotalKop = placement.amountTotal.toLong,
          amountRubKop = 30000,
          amountBonusKop = placement.amountBonus.toLong,
          itemId = placement.avitoOfferId.getOrElse(0L),
          isPlacement = true,
          isVas = false,
          isOther = false
        )
        (operations, operationsMeta) = getOperationsWithMetas(
          OperationIdToCreatedAtToPayedSumList,
          placements,
          Client,
          ActiveDays
        )

        mocks =
          AvitoWalletOperationDaoMock.InsertNonExistingBatch(equalTo(operations), unit) &&
            AvitoWalletOperationMetaDaoMock.ProcessMetaOperations(equalTo(operationsMeta), unit)

        res <- assertM(AvitoWalletOperationProcessor.process(List(externalOperation)))(isUnit)
          .provideCustomLayer(
            Features.liveInMemory ++ Logging.live ++ AvitoWalletOperationDaoMock.empty ++ AvitoUserInfoServiceMock.empty ++ mocks >+> AvitoWalletOperationProcessor.live
          )
      } yield res
    }

  private val avitoWalletOperationProcessorTest =
    testM("AvitoWalletOperationSubscription writes properly to DAO without processing") {
      val currentDate = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, MoscowClock.timeZone).toOffsetDateTime
      val date = ScalaProtobuf.instantToTimestamp(currentDate.toInstant)
      val client = ClientId(1)

      val message = AvitoWalletOperation(
        timestamp = Some(date),
        operationId = "operationId",
        clientId = client.value,
        operationType = "operationType",
        serviceId = 1L,
        serviceName = "serviceName",
        serviceType = "serviceType",
        operationName = "operationName",
        amountTotalKop = 42,
        amountRubKop = 33,
        amountBonusKop = 12,
        itemId = 11L, // avitoOfferId
        isPlacement = true,
        isVas = false,
        isOther = true
      )
      val expected = AWORecord(
        operationId = "operationId",
        clientId = client.value,
        createdAt = currentDate,
        operationType = "operationType",
        operationName = "operationName",
        serviceId = Some(1L),
        serviceName = Some("serviceName"),
        serviceType = Some("serviceType"),
        amountTotal = BigDecimal(42),
        amountRub = BigDecimal(33),
        amountBonus = BigDecimal(12),
        avitoOfferId = Some(11L), // itemId
        isPlacement = true,
        isVas = false,
        isOther = true
      )

      val mocks =
        AvitoWalletOperationDaoMock.InsertNonExistingBatch(equalTo(List(expected)), unit)

      assertM(AvitoWalletOperationProcessor.process(List(message)))(isUnit)
        .provideCustomLayer(
          Features.liveInMemory ++ Logging.live ++ AvitoWalletOperationMetaDaoMock.empty ++ AvitoUserInfoServiceMock.empty ++ mocks >+> AvitoWalletOperationProcessor.live
        )
    }

  override val spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("AvitoWalletOperationProcessor")(
      avitoWalletOperationProcessorTest,
      processOldOperations,
      processNewOperations
    )
}
